package com.h2togo.backend.pedidos;

import com.h2togo.backend.common.BusinessRuleException;
import com.h2togo.backend.common.NotFoundException;
import com.h2togo.backend.notificaciones.PushService;
import com.h2togo.backend.pedidos.dto.PedidoResponse;
import com.h2togo.backend.pedidos.dto.ResultadoEntregaRequest;
import com.h2togo.backend.pedidos.dto.ResultadoEntregaRequest.Linea;
import com.h2togo.backend.pedidos.dto.ResultadoEntregaRequest.Resultado;
import com.h2togo.backend.tracking.ProximidadService;
import com.h2togo.backend.usuarios.Repartidor;
import com.h2togo.backend.usuarios.RepartidorRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro del resultado de la entrega (CU-012). {@code ENTREGADO}: valida ≤ radio (RF-014),
 * fija cantidades entregadas (parciales, RN-032), consume del vehículo (salida_pedido),
 * libera lo no entregado (RN-023) y recalcula el total definitivo (RN-033). {@code NO_ENTREGADO}:
 * libera apartados, suma ausencia y suspende al umbral (RN-006, §10 #1,#29).
 */
@Service
public class EntregaService {

    private final NamedParameterJdbcTemplate jdbc;
    private final PedidoRepository pedidoRepository;
    private final RepartidorRepository repartidorRepository;
    private final PushService pushService;
    private final ProximidadService proximidadService;
    private final int radioM;
    private final int ausenciasUmbral;
    private final int suspensionDias;

    public EntregaService(NamedParameterJdbcTemplate jdbc, PedidoRepository pedidoRepository,
            RepartidorRepository repartidorRepository, PushService pushService,
            ProximidadService proximidadService,
            @Value("${h2togo.pedidos.entrega-radio-m}") int radioM,
            @Value("${h2togo.pedidos.ausencias-umbral}") int ausenciasUmbral,
            @Value("${h2togo.pedidos.suspension-dias}") int suspensionDias) {
        this.jdbc = jdbc;
        this.pedidoRepository = pedidoRepository;
        this.repartidorRepository = repartidorRepository;
        this.pushService = pushService;
        this.proximidadService = proximidadService;
        this.radioM = radioM;
        this.ausenciasUmbral = ausenciasUmbral;
        this.suspensionDias = suspensionDias;
    }

    @Transactional
    public PedidoResponse resultado(int idRepartidor, int idPedido, ResultadoEntregaRequest req) {
        Map<String, Object> pedido = cargarPedido(idPedido);
        if (!Integer.valueOf(idRepartidor).equals(pedido.get("id_repartidor"))) {
            throw new AccessDeniedException("No eres el repartidor asignado a este pedido.");
        }
        if (!"en_camino".equals(pedido.get("estado_actual"))) {
            throw new BusinessRuleException("TRANSICION_INVALIDA", "El pedido debe estar en camino.");
        }
        int idCliente = (Integer) pedido.get("id_cliente");
        int idDireccion = (Integer) pedido.get("id_direccion_entrega");
        int idVehiculo = (Integer) pedido.get("id_vehiculo_utilizado");
        int idNegocio = repartidorRepository.findById(idRepartidor)
                .map(Repartidor::getIdNegocio)
                .orElseThrow(() -> new NotFoundException("REPARTIDOR_NO_ENCONTRADO", "No es repartidor."));

        if (req.resultado() == Resultado.ENTREGADO) {
            entregar(idPedido, idRepartidor, idVehiculo, idCliente, idDireccion, idNegocio, req);
            pushService.notificar(idCliente, "Pedido entregado", "Tu pedido fue entregado. ¡Gracias!");
        } else {
            noEntregar(idPedido, idCliente, req);
            pushService.notificar(idCliente, "Pedido no entregado",
                    "Tu pedido no pudo entregarse: " + req.motivoNoEntrega());
        }
        proximidadService.olvidar(idPedido); // limpia el flag de proximidad al cerrar
        Pedido p = pedidoRepository.findById(idPedido).orElseThrow();
        return PedidoMapper.toResponse(p);
    }

    // ---------------------------------------------------------------- ENTREGADO

    private void entregar(int idPedido, int idRepartidor, int idVehiculo, int idCliente, int idDireccion,
            int idNegocio, ResultadoEntregaRequest req) {
        // RF-014: el repartidor debe estar a ≤ radio del domicilio.
        Boolean dentro = jdbc.queryForObject("""
                SELECT ST_DWithin(d.ubicacion, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :radio)
                FROM direcciones_clientes d WHERE d.id_direccion = :dir""",
                new MapSqlParameterSource().addValue("lon", req.lon()).addValue("lat", req.lat())
                        .addValue("radio", radioM).addValue("dir", idDireccion), Boolean.class);
        if (!Boolean.TRUE.equals(dentro)) {
            throw new BusinessRuleException("FUERA_DE_RANGO",
                    "Debes estar a menos de " + radioM + " m del domicilio para entregar (RF-014).");
        }

        procesarLineas(idPedido, idNegocio, req.lineas());
        consumirEntregado(idPedido, idVehiculo, idRepartidor);

        jdbc.update("""
                UPDATE pedidos SET estado_actual = 'entregado'::estado_pedido, fecha_entrega = now(),
                    total_pagar = (SELECT COALESCE(SUM(cantidad_entregada * (precio_unitario + COALESCE(precio_envase_unitario, 0))), 0)
                                   FROM detalles_pedido WHERE id_pedido = :p AND cantidad_entregada IS NOT NULL)
                WHERE id_pedido = :p""", new MapSqlParameterSource("p", idPedido));
        // RN-006: una entrega exitosa reinicia el contador de ausencias.
        jdbc.update("UPDATE clientes SET ausencias_consecutivas = 0 WHERE id_usuario = :c",
                new MapSqlParameterSource("c", idCliente));
        insertarHistorial(idPedido, "entregado", null, req.lat(), req.lon());
    }

    private void procesarLineas(int idPedido, int idNegocio, List<Linea> lineas) {
        if (lineas == null) {
            return;
        }
        for (Linea l : lineas) {
            if (l.agregadaEnSitio()) {
                if (l.cantidadEntregada() <= 0 || l.idMarca() == null) {
                    continue;
                }
                Map<String, Object> prod = jdbc.queryForMap(
                        "SELECT precio, precio_envase FROM productos_negocio WHERE id_negocio = :neg AND id_marca = :m",
                        new MapSqlParameterSource().addValue("neg", idNegocio).addValue("m", l.idMarca()));
                jdbc.update("""
                        INSERT INTO detalles_pedido (id_pedido, id_marca, cantidad_solicitada, cantidad_entregada,
                            tiene_envase, precio_unitario, precio_envase_unitario, agregada_en_sitio)
                        VALUES (:p, :m, 0, :c, FALSE, :precio, :precioEnvase, TRUE)""",
                        new MapSqlParameterSource().addValue("p", idPedido).addValue("m", l.idMarca())
                                .addValue("c", l.cantidadEntregada()).addValue("precio", prod.get("precio"))
                                .addValue("precioEnvase", prod.get("precio_envase")));
            } else {
                if (l.idDetalle() == null) {
                    throw new BusinessRuleException("LINEA_INVALIDA", "Cada línea existente requiere idDetalle.");
                }
                int filas = jdbc.update("""
                        UPDATE detalles_pedido SET cantidad_entregada = :c
                        WHERE id_detalle = :d AND id_pedido = :p AND :c <= cantidad_solicitada""",
                        new MapSqlParameterSource().addValue("c", l.cantidadEntregada())
                                .addValue("d", l.idDetalle()).addValue("p", idPedido));
                if (filas == 0) {
                    throw new BusinessRuleException("CANTIDAD_INVALIDA",
                            "La cantidad entregada excede la solicitada o el detalle no existe (RN-032).");
                }
            }
        }
        // Las líneas existentes no incluidas quedan como entregadas = 0 (no recibidas).
        jdbc.update("UPDATE detalles_pedido SET cantidad_entregada = 0 "
                + "WHERE id_pedido = :p AND agregada_en_sitio = FALSE AND cantidad_entregada IS NULL",
                new MapSqlParameterSource("p", idPedido));
    }

    /** Consume del vehículo lo entregado (salida_pedido) y libera los apartados sobrantes (RN-023). */
    private void consumirEntregado(int idPedido, int idVehiculo, int idRepartidor) {
        Map<Integer, Integer> entregadoOriginal = sumaEntregadaPorMarca(idPedido, false);
        Map<Integer, Integer> entregadoEnSitio = sumaEntregadaPorMarca(idPedido, true);

        List<Map<String, Object>> apartados = jdbc.queryForList("""
                SELECT a.id_apartado, a.id_inventario_vehiculo AS iv, a.cantidad, l.id_marca
                FROM apartados_pedido a
                JOIN inventario_vehiculo ivt ON ivt.id_inventario_vehiculo = a.id_inventario_vehiculo
                JOIN lotes_inventario l ON l.id_lote = ivt.id_lote
                WHERE a.id_pedido = :p AND a.id_inventario_vehiculo IS NOT NULL
                ORDER BY l.fecha_caducidad, l.id_lote
                FOR UPDATE OF ivt""", new MapSqlParameterSource("p", idPedido));

        Map<Integer, Integer> restante = new HashMap<>(entregadoOriginal);
        for (Map<String, Object> a : apartados) {
            int marca = (Integer) a.get("id_marca");
            int reservado = (Integer) a.get("cantidad");
            int iv = (Integer) a.get("iv");
            int consume = Math.min(restante.getOrDefault(marca, 0), reservado);
            jdbc.update("""
                    UPDATE inventario_vehiculo
                    SET cantidad_actual = cantidad_actual - :consume, cantidad_apartada = cantidad_apartada - :reservado
                    WHERE id_inventario_vehiculo = :iv""",
                    new MapSqlParameterSource().addValue("consume", consume)
                            .addValue("reservado", reservado).addValue("iv", iv));
            if (consume > 0) {
                movimientoSalida(iv, consume, idPedido, idRepartidor);
                restante.put(marca, restante.get(marca) - consume);
            }
        }
        jdbc.update("DELETE FROM apartados_pedido WHERE id_pedido = :p", new MapSqlParameterSource("p", idPedido));

        // Líneas agregadas en sitio: consumen del vehículo directamente (no estaban apartadas).
        for (Map.Entry<Integer, Integer> e : entregadoEnSitio.entrySet()) {
            consumirDirecto(idVehiculo, e.getKey(), e.getValue(), idPedido, idRepartidor);
        }
    }

    private void consumirDirecto(int idVehiculo, int idMarca, int cantidad, int idPedido, int idRepartidor) {
        List<Map<String, Object>> filas = jdbc.queryForList("""
                SELECT iv.id_inventario_vehiculo AS iv, iv.cantidad_actual AS disp
                FROM inventario_vehiculo iv JOIN lotes_inventario l ON l.id_lote = iv.id_lote
                WHERE iv.id_vehiculo = :veh AND iv.activo AND l.id_marca = :marca
                  AND l.fecha_caducidad >= CURRENT_DATE AND iv.cantidad_actual > 0
                ORDER BY l.fecha_caducidad, l.id_lote FOR UPDATE OF iv""",
                new MapSqlParameterSource().addValue("veh", idVehiculo).addValue("marca", idMarca));
        int restante = cantidad;
        for (Map<String, Object> f : filas) {
            if (restante == 0) {
                break;
            }
            int disp = (Integer) f.get("disp");
            int iv = (Integer) f.get("iv");
            int tomar = Math.min(restante, disp);
            jdbc.update("UPDATE inventario_vehiculo SET cantidad_actual = cantidad_actual - :t WHERE id_inventario_vehiculo = :iv",
                    new MapSqlParameterSource().addValue("t", tomar).addValue("iv", iv));
            movimientoSalida(iv, tomar, idPedido, idRepartidor);
            restante -= tomar;
        }
        if (restante > 0) {
            throw new BusinessRuleException("SIN_STOCK_ENSITIO",
                    "El vehículo no tiene stock para la línea agregada en sitio.");
        }
    }

    // ---------------------------------------------------------------- NO_ENTREGADO

    private void noEntregar(int idPedido, int idCliente, ResultadoEntregaRequest req) {
        if (req.motivoNoEntrega() == null || req.motivoNoEntrega().isBlank()) {
            throw new BusinessRuleException("MOTIVO_REQUERIDO", "Un pedido no entregado requiere motivo.");
        }
        jdbc.update("UPDATE detalles_pedido SET cantidad_entregada = 0 WHERE id_pedido = :p AND agregada_en_sitio = FALSE",
                new MapSqlParameterSource("p", idPedido));
        liberarApartados(idPedido); // los garrafones vuelven al vehículo
        jdbc.update("UPDATE pedidos SET estado_actual = 'no_entregado'::estado_pedido, total_pagar = 0, fecha_entrega = now() WHERE id_pedido = :p",
                new MapSqlParameterSource("p", idPedido));

        // RN-006: ausencia +1; al llegar al umbral, suspende y reinicia el contador.
        Integer ausencias = jdbc.queryForObject(
                "UPDATE clientes SET ausencias_consecutivas = ausencias_consecutivas + 1 WHERE id_usuario = :c RETURNING ausencias_consecutivas",
                new MapSqlParameterSource("c", idCliente), Integer.class);
        if (ausencias != null && ausencias >= ausenciasUmbral) {
            jdbc.update("""
                    UPDATE clientes SET suspendido_hasta = now() + make_interval(days => :dias),
                        ausencias_consecutivas = 0 WHERE id_usuario = :c""",
                    new MapSqlParameterSource().addValue("dias", suspensionDias).addValue("c", idCliente));
        }
        insertarHistorial(idPedido, "no_entregado", req.motivoNoEntrega(), req.lat(), req.lon());
    }

    private void liberarApartados(int idPedido) {
        var params = new MapSqlParameterSource("p", idPedido);
        jdbc.update("""
                UPDATE inventario_vehiculo iv SET cantidad_apartada = iv.cantidad_apartada - a.cantidad
                FROM apartados_pedido a
                WHERE a.id_pedido = :p AND a.id_inventario_vehiculo = iv.id_inventario_vehiculo""", params);
        jdbc.update("""
                UPDATE lotes_inventario l SET cantidad_apartada = l.cantidad_apartada - a.cantidad
                FROM apartados_pedido a
                WHERE a.id_pedido = :p AND a.id_lote_base = l.id_lote""", params);
        jdbc.update("DELETE FROM apartados_pedido WHERE id_pedido = :p", params);
    }

    // ---------------------------------------------------------------- Helpers

    private Map<Integer, Integer> sumaEntregadaPorMarca(int idPedido, boolean enSitio) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id_marca, COALESCE(SUM(cantidad_entregada), 0) AS total
                FROM detalles_pedido
                WHERE id_pedido = :p AND agregada_en_sitio = :sitio AND cantidad_entregada IS NOT NULL
                GROUP BY id_marca""",
                new MapSqlParameterSource().addValue("p", idPedido).addValue("sitio", enSitio));
        Map<Integer, Integer> map = new HashMap<>();
        for (Map<String, Object> r : rows) {
            map.put((Integer) r.get("id_marca"), ((Number) r.get("total")).intValue());
        }
        return map;
    }

    private void movimientoSalida(int idInventarioVehiculo, int cantidad, int idPedido, int idRepartidor) {
        jdbc.update("""
                INSERT INTO movimientos_inventario (id_inventario_vehiculo, tipo, cantidad, id_pedido, id_repartidor_responsable)
                VALUES (:iv, 'salida_pedido'::tipo_movimiento, :cant, :ped, :rep)""",
                new MapSqlParameterSource().addValue("iv", idInventarioVehiculo).addValue("cant", cantidad)
                        .addValue("ped", idPedido).addValue("rep", idRepartidor));
    }

    private void insertarHistorial(int idPedido, String estado, String notas, Double lat, Double lon) {
        String ubic = (lat == null || lon == null) ? "NULL"
                : "ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography";
        jdbc.update("INSERT INTO historial_estados_pedido (id_pedido, estado, notas_adicionales, ubicacion) "
                + "VALUES (:ped, CAST(:estado AS estado_pedido), :notas, " + ubic + ")",
                new MapSqlParameterSource().addValue("ped", idPedido).addValue("estado", estado)
                        .addValue("notas", notas).addValue("lat", lat).addValue("lon", lon));
    }

    private Map<String, Object> cargarPedido(int idPedido) {
        try {
            return jdbc.queryForMap("""
                    SELECT estado_actual::text, id_repartidor, id_cliente, id_direccion_entrega, id_vehiculo_utilizado
                    FROM pedidos WHERE id_pedido = :id""", new MapSqlParameterSource("id", idPedido));
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("PEDIDO_NO_ENCONTRADO", "Pedido no encontrado.");
        }
    }
}
