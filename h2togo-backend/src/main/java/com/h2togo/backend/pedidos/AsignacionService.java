package com.h2togo.backend.pedidos;

import com.h2togo.backend.common.BusinessRuleException;
import com.h2togo.backend.common.ConflictException;
import com.h2togo.backend.common.NotFoundException;
import com.h2togo.backend.notificaciones.PushService;
import com.h2togo.backend.pedidos.dto.DetalleResponse;
import com.h2togo.backend.pedidos.dto.PedidoDisponibleResponse;
import com.h2togo.backend.pedidos.dto.PedidoResponse;
import com.h2togo.backend.usuarios.Repartidor;
import com.h2togo.backend.usuarios.RepartidorRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asignación de pedidos al repartidor (CU-010): lista de disponibles, aceptación con
 * concurrencia RF-019 (UPDATE condicional) y apartado desde el vehículo (RN-022/RN-008,
 * §10 #28), y puesta en camino. Todo transaccional.
 */
@Service
public class AsignacionService {

    private final NamedParameterJdbcTemplate jdbc;
    private final PedidoRepository pedidoRepository;
    private final RepartidorRepository repartidorRepository;
    private final PushService pushService;

    public AsignacionService(NamedParameterJdbcTemplate jdbc, PedidoRepository pedidoRepository,
            RepartidorRepository repartidorRepository, PushService pushService) {
        this.jdbc = jdbc;
        this.pedidoRepository = pedidoRepository;
        this.repartidorRepository = repartidorRepository;
        this.pushService = pushService;
    }

    // ---------------------------------------------------------------- CU-010: disponibles

    @Transactional(readOnly = true)
    public List<PedidoDisponibleResponse> disponibles(int idRepartidor) {
        // Q3 (§7): pendientes compatibles (directa a su negocio, o abierta cuyo catálogo cumpla),
        // con dirección en zona, ordenados por distancia a la ubicación actual del repartidor.
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT p.id_pedido,
                       ST_Distance(d.ubicacion, r.ubicacion_actual) AS distancia_m,
                       p.garrafones_totales, d.colonia, p.tipo_solicitud, p.total_pagar
                FROM pedidos p
                JOIN direcciones_clientes d ON d.id_direccion = p.id_direccion_entrega
                JOIN repartidores r ON r.id_usuario = :rep
                WHERE p.estado_actual = 'pendiente' AND d.en_zona_cobertura
                  AND ( p.id_negocio_solicitado = r.id_negocio
                     OR ( p.tipo_solicitud = 'abierta' AND NOT EXISTS (
                            SELECT 1 FROM detalles_pedido dp
                            WHERE dp.id_pedido = p.id_pedido AND NOT EXISTS (
                                SELECT 1 FROM productos_negocio pn
                                WHERE pn.id_negocio = r.id_negocio AND pn.id_marca = dp.id_marca
                                  AND pn.activo AND pn.precio <= p.precio_maximo_garrafon)) ) )
                ORDER BY distancia_m ASC NULLS LAST""",
                new MapSqlParameterSource("rep", idRepartidor));
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Integer> ids = rows.stream().map(r -> (Integer) r.get("id_pedido")).toList();
        Map<Integer, List<DetalleResponse>> detalles = detallesPorPedido(ids);

        List<PedidoDisponibleResponse> salida = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Integer id = (Integer) row.get("id_pedido");
            Number dist = (Number) row.get("distancia_m");
            salida.add(new PedidoDisponibleResponse(
                    id, dist == null ? null : dist.doubleValue(),
                    (Integer) row.get("garrafones_totales"), (String) row.get("colonia"),
                    com.h2togo.backend.common.enums.TipoSolicitudPedido.valueOf((String) row.get("tipo_solicitud")),
                    (BigDecimal) row.get("total_pagar"),
                    detalles.getOrDefault(id, List.of())));
        }
        return salida;
    }

    // ---------------------------------------------------------------- CU-010: aceptar

    @Transactional
    public PedidoResponse aceptar(int idRepartidor, int idPedido) {
        Repartidor r = repartidorRepository.findById(idRepartidor)
                .orElseThrow(() -> new NotFoundException("REPARTIDOR_NO_ENCONTRADO", "No es repartidor."));
        if (r.getIdVehiculoActual() == null || !r.isEstadoOperativo()) {
            throw new BusinessRuleException("SIN_JORNADA", "Debes iniciar jornada con un vehículo (RN-014).");
        }
        int idVehiculo = r.getIdVehiculoActual();
        int idNegocio = r.getIdNegocio();

        Map<String, Object> pedido = cargarPedidoParaAceptar(idPedido);
        if (!"pendiente".equals(pedido.get("estado_actual"))) {
            throw new ConflictException("PEDIDO_YA_ASIGNADO", "El pedido ya no está disponible.");
        }
        String tipo = (String) pedido.get("tipo_solicitud");
        Integer idNegocioSolicitado = (Integer) pedido.get("id_negocio_solicitado");
        BigDecimal precioMax = (BigDecimal) pedido.get("precio_maximo_garrafon");

        List<Map<String, Object>> detalles = jdbc.queryForList(
                "SELECT id_marca, cantidad_solicitada FROM detalles_pedido WHERE id_pedido = :p",
                new MapSqlParameterSource("p", idPedido));

        validarCompatibilidad(tipo, idNegocioSolicitado, idNegocio, precioMax, detalles);
        validarCargaVehiculo(idVehiculo, detalles); // RN-008 (§10 #28)

        // RF-019: solo el primero en confirmar gana (UPDATE condicional).
        int filas = jdbc.update("""
                UPDATE pedidos SET id_repartidor = :rep, estado_actual = 'asignado'::estado_pedido,
                    id_vehiculo_utilizado = :veh
                WHERE id_pedido = :id AND estado_actual = 'pendiente' AND id_repartidor IS NULL""",
                new MapSqlParameterSource().addValue("rep", idRepartidor)
                        .addValue("veh", idVehiculo).addValue("id", idPedido));
        if (filas == 0) {
            throw new ConflictException("PEDIDO_YA_ASIGNADO", "Otro repartidor tomó el pedido primero.");
        }

        for (Map<String, Object> d : detalles) {
            apartarDelVehiculo(idVehiculo, (Integer) d.get("id_marca"),
                    (Integer) d.get("cantidad_solicitada"), idPedido);
        }
        if ("abierta".equals(tipo)) {
            fijarPreciosAbierta(idPedido, idNegocio); // RN-025: precio real del negocio que acepta
        }
        insertarHistorial(idPedido, "asignado", null, null, null);
        pushService.notificar((Integer) pedido.get("id_cliente"), "Pedido asignado",
                "Un repartidor tomó tu pedido y lo preparará para entrega.");

        return respuesta(idPedido);
    }

    // ---------------------------------------------------------------- CU-012 (parte): en camino

    @Transactional
    public PedidoResponse enCamino(int idRepartidor, int idPedido, double lat, double lon) {
        Map<String, Object> pedido = cargarPedidoAsignado(idPedido, idRepartidor);
        if (!"asignado".equals(pedido.get("estado_actual"))) {
            throw new BusinessRuleException("TRANSICION_INVALIDA",
                    "El pedido debe estar asignado para ponerse en camino.");
        }
        jdbc.update("UPDATE pedidos SET estado_actual = 'en_camino'::estado_pedido WHERE id_pedido = :id",
                new MapSqlParameterSource("id", idPedido));
        insertarHistorial(idPedido, "en_camino", null, lat, lon);
        pushService.notificar((Integer) pedido.get("id_cliente"), "Pedido en camino",
                "Tu repartidor va en camino a tu domicilio.");
        return respuesta(idPedido);
    }

    // ---------------------------------------------------------------- Helpers

    private Map<String, Object> cargarPedidoParaAceptar(int idPedido) {
        try {
            return jdbc.queryForMap("""
                    SELECT estado_actual::text, tipo_solicitud::text, id_negocio_solicitado,
                           precio_maximo_garrafon, id_cliente
                    FROM pedidos WHERE id_pedido = :id""", new MapSqlParameterSource("id", idPedido));
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new NotFoundException("PEDIDO_NO_ENCONTRADO", "Pedido no encontrado.");
        }
    }

    private Map<String, Object> cargarPedidoAsignado(int idPedido, int idRepartidor) {
        Map<String, Object> pedido;
        try {
            pedido = jdbc.queryForMap(
                    "SELECT estado_actual::text, id_repartidor, id_cliente FROM pedidos WHERE id_pedido = :id",
                    new MapSqlParameterSource("id", idPedido));
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new NotFoundException("PEDIDO_NO_ENCONTRADO", "Pedido no encontrado.");
        }
        if (!Integer.valueOf(idRepartidor).equals(pedido.get("id_repartidor"))) {
            throw new org.springframework.security.access.AccessDeniedException("No eres el repartidor asignado.");
        }
        return pedido;
    }

    private void validarCompatibilidad(String tipo, Integer idNegocioSolicitado, int idNegocio,
            BigDecimal precioMax, List<Map<String, Object>> detalles) {
        if ("directa".equals(tipo)) {
            if (!Integer.valueOf(idNegocio).equals(idNegocioSolicitado)) {
                throw new BusinessRuleException("PEDIDO_NO_COMPATIBLE", "El pedido directo es de otra purificadora.");
            }
        } else { // abierta: el negocio vende TODAS las marcas a precio <= máximo (RN-028)
            for (Map<String, Object> d : detalles) {
                Integer count = jdbc.queryForObject("""
                        SELECT COUNT(*) FROM productos_negocio
                        WHERE id_negocio = :neg AND id_marca = :marca AND activo AND precio <= :max""",
                        new MapSqlParameterSource().addValue("neg", idNegocio)
                                .addValue("marca", d.get("id_marca")).addValue("max", precioMax),
                        Integer.class);
                if (count == null || count == 0) {
                    throw new BusinessRuleException("PEDIDO_NO_COMPATIBLE",
                            "Tu negocio no cumple el precio máximo para todas las marcas (RN-028).");
                }
            }
        }
    }

    private void validarCargaVehiculo(int idVehiculo, List<Map<String, Object>> detalles) {
        for (Map<String, Object> d : detalles) {
            Integer disponible = jdbc.queryForObject("""
                    SELECT COALESCE(SUM(iv.cantidad_actual - iv.cantidad_apartada), 0)
                    FROM inventario_vehiculo iv JOIN lotes_inventario l ON l.id_lote = iv.id_lote
                    WHERE iv.id_vehiculo = :veh AND iv.activo AND l.id_marca = :marca
                      AND l.fecha_caducidad >= CURRENT_DATE""",
                    new MapSqlParameterSource().addValue("veh", idVehiculo).addValue("marca", d.get("id_marca")),
                    Integer.class);
            if (disponible == null || disponible < (Integer) d.get("cantidad_solicitada")) {
                throw new BusinessRuleException("CARGA_INSUFICIENTE",
                        "El vehículo no tiene carga suficiente; carga más desde la base (RN-008).");
            }
        }
    }

    private void apartarDelVehiculo(int idVehiculo, int idMarca, int cantidad, int idPedido) {
        List<Map<String, Object>> filas = jdbc.queryForList("""
                SELECT iv.id_inventario_vehiculo, (iv.cantidad_actual - iv.cantidad_apartada) AS disp
                FROM inventario_vehiculo iv JOIN lotes_inventario l ON l.id_lote = iv.id_lote
                WHERE iv.id_vehiculo = :veh AND iv.activo AND l.id_marca = :marca
                  AND l.fecha_caducidad >= CURRENT_DATE AND (iv.cantidad_actual - iv.cantidad_apartada) > 0
                ORDER BY l.fecha_caducidad, l.id_lote
                FOR UPDATE""",
                new MapSqlParameterSource().addValue("veh", idVehiculo).addValue("marca", idMarca));
        int restante = cantidad;
        for (Map<String, Object> f : filas) {
            if (restante == 0) {
                break;
            }
            int disp = ((Number) f.get("disp")).intValue();
            int tomar = Math.min(restante, disp);
            var p = new MapSqlParameterSource().addValue("iv", f.get("id_inventario_vehiculo"))
                    .addValue("tomar", tomar).addValue("ped", idPedido);
            jdbc.update("UPDATE inventario_vehiculo SET cantidad_apartada = cantidad_apartada + :tomar "
                    + "WHERE id_inventario_vehiculo = :iv", p);
            jdbc.update("INSERT INTO apartados_pedido (id_pedido, id_inventario_vehiculo, cantidad) "
                    + "VALUES (:ped, :iv, :tomar)", p);
            restante -= tomar;
        }
    }

    private void fijarPreciosAbierta(int idPedido, int idNegocio) {
        List<Map<String, Object>> marcas = jdbc.queryForList(
                "SELECT DISTINCT id_marca FROM detalles_pedido WHERE id_pedido = :p",
                new MapSqlParameterSource("p", idPedido));
        for (Map<String, Object> m : marcas) {
            Map<String, Object> prod = jdbc.queryForMap(
                    "SELECT precio, precio_envase FROM productos_negocio WHERE id_negocio = :neg AND id_marca = :marca",
                    new MapSqlParameterSource().addValue("neg", idNegocio).addValue("marca", m.get("id_marca")));
            jdbc.update("""
                    UPDATE detalles_pedido
                    SET precio_unitario = :precio,
                        precio_envase_unitario = CASE WHEN tiene_envase THEN NULL ELSE :precioEnvase END
                    WHERE id_pedido = :p AND id_marca = :marca""",
                    new MapSqlParameterSource().addValue("precio", prod.get("precio"))
                            .addValue("precioEnvase", prod.get("precio_envase"))
                            .addValue("p", idPedido).addValue("marca", m.get("id_marca")));
        }
        jdbc.update("""
                UPDATE pedidos SET total_pagar = (
                    SELECT COALESCE(SUM(cantidad_solicitada * (precio_unitario + COALESCE(precio_envase_unitario, 0))), 0)
                    FROM detalles_pedido WHERE id_pedido = :p)
                WHERE id_pedido = :p""", new MapSqlParameterSource("p", idPedido));
    }

    /** Inserta un cambio de estado en el historial (RN-013), con ubicación si aplica. */
    void insertarHistorial(int idPedido, String estado, String notas, Double lat, Double lon) {
        var p = new MapSqlParameterSource().addValue("ped", idPedido).addValue("estado", estado)
                .addValue("notas", notas).addValue("lat", lat).addValue("lon", lon);
        String ubic = (lat == null || lon == null) ? "NULL"
                : "ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography";
        jdbc.update("INSERT INTO historial_estados_pedido (id_pedido, estado, notas_adicionales, ubicacion) "
                + "VALUES (:ped, CAST(:estado AS estado_pedido), :notas, " + ubic + ")", p);
    }

    private Map<Integer, List<DetalleResponse>> detallesPorPedido(List<Integer> ids) {
        return jdbc.query("""
                SELECT id_detalle, id_pedido, id_marca, cantidad_solicitada, cantidad_entregada,
                       tiene_envase, precio_unitario, precio_envase_unitario, agregada_en_sitio
                FROM detalles_pedido WHERE id_pedido IN (:ids)""",
                new MapSqlParameterSource("ids", ids), rs -> {
                    Map<Integer, List<DetalleResponse>> map = new java.util.HashMap<>();
                    while (rs.next()) {
                        Integer idPedido = rs.getInt("id_pedido");
                        Integer entregada = (Integer) rs.getObject("cantidad_entregada");
                        map.computeIfAbsent(idPedido, k -> new ArrayList<>()).add(new DetalleResponse(
                                rs.getInt("id_detalle"), rs.getInt("id_marca"), rs.getInt("cantidad_solicitada"),
                                entregada, rs.getBoolean("tiene_envase"), rs.getBigDecimal("precio_unitario"),
                                rs.getBigDecimal("precio_envase_unitario"), rs.getBoolean("agregada_en_sitio")));
                    }
                    return map;
                });
    }

    private PedidoResponse respuesta(int idPedido) {
        Pedido p = pedidoRepository.findById(idPedido).orElseThrow();
        return PedidoMapper.toResponse(p);
    }
}
