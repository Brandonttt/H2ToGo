package com.h2togo.backend.pedidos;

import com.h2togo.backend.repartidores.MiembrosNegocioService;
import com.h2togo.backend.common.BusinessRuleException;
import com.h2togo.backend.common.ConflictException;
import com.h2togo.backend.common.NotFoundException;
import com.h2togo.backend.notificaciones.PushService;
import com.h2togo.backend.pedidos.dto.DetalleResponse;
import com.h2togo.backend.pedidos.dto.PedidoDisponibleResponse;
import com.h2togo.backend.pedidos.dto.PedidoResponse;
import com.h2togo.backend.usuarios.Repartidor;
import com.h2togo.backend.usuarios.RepartidorRepository;
import com.h2togo.backend.vehiculos.VehiculoNegocio;
import com.h2togo.backend.vehiculos.VehiculoNegocioRepository;
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

    private final PedidoEnriquecedor enriquecedor;
    private final NamedParameterJdbcTemplate jdbc;
    private final PedidoRepository pedidoRepository;
    private final RepartidorRepository repartidorRepository;
    private final PushService pushService;
    private final VehiculoNegocioRepository vehiculoRepository;

    private final MiembrosNegocioService miembrosService;

    public AsignacionService(NamedParameterJdbcTemplate jdbc, PedidoRepository pedidoRepository,
            RepartidorRepository repartidorRepository, PushService pushService,
            VehiculoNegocioRepository vehiculoRepository,
            PedidoEnriquecedor enriquecedor,
            MiembrosNegocioService miembrosService) {
        this.miembrosService = miembrosService;
        this.jdbc = jdbc;
        this.enriquecedor = enriquecedor;
        this.pedidoRepository = pedidoRepository;
        this.repartidorRepository = repartidorRepository;
        this.pushService = pushService;
        this.vehiculoRepository = vehiculoRepository;
    }

    /**
     * Condición SQL "el pedido {@code p} lo puede surtir el negocio del repartidor {@code r}": es
     * directo a su negocio, o es abierto y el negocio ofrece todas las marcas al precio máximo o
     * menos (RN-025). La comparten la lista de disponibles y la agenda de programados.
     */
    public static final String COMPATIBLE_CON_REPARTIDOR = """
            ( p.id_negocio_solicitado = r.id_negocio
              OR ( p.tipo_solicitud = 'abierta' AND NOT EXISTS (
                     SELECT 1 FROM detalles_pedido dp
                     WHERE dp.id_pedido = p.id_pedido AND NOT EXISTS (
                         SELECT 1 FROM productos_negocio pn
                         WHERE pn.id_negocio = r.id_negocio AND pn.id_marca = dp.id_marca
                           AND pn.activo AND pn.precio <= p.precio_maximo_garrafon)) ) )""";

    // ---------------------------------------------------------------- CU-010: disponibles

    @Transactional(readOnly = true)
    public List<PedidoDisponibleResponse> disponibles(int idRepartidor) {
        miembrosService.exigirAprobado(idRepartidor); // los pedidos traen datos de los clientes
        // Q3 (§7): pendientes compatibles (directa a su negocio, o abierta cuyo catálogo cumpla),
        // con dirección en zona, ordenados por distancia a la ubicación actual del repartidor.
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT p.id_pedido,
                       ST_Distance(d.ubicacion, r.ubicacion_actual) AS distancia_m,
                       p.garrafones_totales, d.colonia, p.tipo_solicitud,
                       CASE
                         WHEN p.tipo_solicitud = 'abierta' THEN COALESCE((
                             SELECT SUM(dp.cantidad_solicitada * (pn.precio + CASE WHEN dp.tiene_envase THEN 0 ELSE COALESCE(pn.precio_envase, 0) END))
                             FROM detalles_pedido dp
                             JOIN productos_negocio pn ON pn.id_negocio = r.id_negocio AND pn.id_marca = dp.id_marca
                             WHERE dp.id_pedido = p.id_pedido
                         ), p.total_pagar)
                         ELSE p.total_pagar
                       END AS total_pagar,
                       CONCAT(u.nombre, ' ', u.apellidos) AS nombre_cliente,
                       u.telefono AS telefono_cliente,
                       CONCAT_WS(', ', NULLIF(CONCAT_WS(' ', d.calle, d.numero_exterior), ''), NULLIF(d.colonia, '')) AS direccion,
                       ST_Y(d.ubicacion::geometry) AS lat,
                       ST_X(d.ubicacion::geometry) AS lon
                FROM pedidos p
                JOIN direcciones_clientes d ON d.id_direccion = p.id_direccion_entrega
                JOIN repartidores r ON r.id_usuario = :rep
                JOIN usuarios u ON u.id_usuario = p.id_cliente
                WHERE p.estado_actual = 'pendiente' AND d.en_zona_cobertura
                  AND (p.id_repartidor IS NULL OR p.id_repartidor = :rep) -- reservados: solo su repartidor
                  AND """ + COMPATIBLE_CON_REPARTIDOR + """
                ORDER BY distancia_m ASC NULLS LAST""",
                new MapSqlParameterSource("rep", idRepartidor));
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Integer> ids = rows.stream().map(r -> (Integer) r.get("id_pedido")).toList();
        Map<Integer, List<DetalleResponse>> detalles = detallesPorPedido(ids, idRepartidor);

        List<PedidoDisponibleResponse> salida = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Integer id = (Integer) row.get("id_pedido");
            Number dist = (Number) row.get("distancia_m");
            Number lat = (Number) row.get("lat");
            Number lon = (Number) row.get("lon");
            salida.add(new PedidoDisponibleResponse(
                    id, dist == null ? null : dist.doubleValue(),
                    (Integer) row.get("garrafones_totales"), (String) row.get("colonia"),
                    com.h2togo.backend.common.enums.TipoSolicitudPedido.valueOf((String) row.get("tipo_solicitud")),
                    (BigDecimal) row.get("total_pagar"),
                    detalles.getOrDefault(id, List.of()),
                    (String) row.get("nombre_cliente"),
                    (String) row.get("direccion"),
                    (String) row.get("telefono_cliente"),
                    lat == null ? null : lat.doubleValue(),
                    lon == null ? null : lon.doubleValue()));
        }
        return salida;
    }

    // ---------------------------------------------------------------- CU-010: aceptar

    @Transactional
    public PedidoResponse aceptar(int idRepartidor, int idPedido) {
        miembrosService.exigirAprobado(idRepartidor);
        Repartidor r = repartidorRepository.findById(idRepartidor)
                .orElseThrow(() -> new NotFoundException("REPARTIDOR_NO_ENCONTRADO", "No es repartidor."));
        // RN-007 / RN-014 / RF-030: solo acepta quien inició jornada con vehículo y lo cargó.
        if (r.getIdVehiculoActual() == null || !r.isEstadoOperativo()) {
            throw new BusinessRuleException("SIN_JORNADA",
                    "Inicia tu jornada (elige vehículo y cárgalo desde la base) antes de aceptar pedidos.");
        }
        int idVehiculo = r.getIdVehiculoActual();
        int idNegocio = r.getIdNegocio();

        Map<String, Object> pedido = cargarPedidoParaAceptar(idPedido);
        // Un programado reservado en la agenda lo acepta su repartidor aunque aún no sea su hora.
        Object reservadoPor = pedido.get("id_repartidor");
        boolean libre = "pendiente".equals(pedido.get("estado_actual"))
                && (reservadoPor == null || Integer.valueOf(idRepartidor).equals(reservadoPor));
        boolean reservadoPorMi = "pendiente_programado".equals(pedido.get("estado_actual"))
                && Integer.valueOf(idRepartidor).equals(reservadoPor);
        if (!libre && !reservadoPorMi) {
            throw new ConflictException("PEDIDO_YA_ASIGNADO", "El pedido ya no está disponible.");
        }
        String tipo = (String) pedido.get("tipo_solicitud");
        Integer idNegocioSolicitado = (Integer) pedido.get("id_negocio_solicitado");
        BigDecimal precioMax = (BigDecimal) pedido.get("precio_maximo_garrafon");

        List<Map<String, Object>> detalles = jdbc.queryForList(
                "SELECT id_marca, cantidad_solicitada FROM detalles_pedido WHERE id_pedido = :p",
                new MapSqlParameterSource("p", idPedido));

        validarCompatibilidad(tipo, idNegocioSolicitado, idNegocio, precioMax, detalles);
        validarStockDisponible(idVehiculo, idNegocio, detalles); // RN-008, RN-022

        // RF-019: solo el primero en confirmar gana (UPDATE condicional).
        int filas = jdbc.update("""
                UPDATE pedidos SET id_repartidor = :rep, estado_actual = 'asignado'::estado_pedido
                WHERE id_pedido = :id
                  AND ( (estado_actual = 'pendiente' AND (id_repartidor IS NULL OR id_repartidor = :rep))
                     OR (estado_actual = 'pendiente_programado' AND id_repartidor = :rep) )""",
                new MapSqlParameterSource().addValue("rep", idRepartidor).addValue("id", idPedido));
        if (filas == 0) {
            throw new ConflictException("PEDIDO_YA_ASIGNADO", "Otro repartidor tomó el pedido primero.");
        }

        boolean apartadosEnBase = false;
        for (Map<String, Object> d : detalles) {
            int faltante = apartarDelVehiculo(idVehiculo, (Integer) d.get("id_marca"),
                    (Integer) d.get("cantidad_solicitada"), idPedido);
            if (faltante > 0) {
                apartarDeLaBase(idNegocio, (Integer) d.get("id_marca"), faltante, idPedido);
                apartadosEnBase = true;
            }
        }
        if ("abierta".equals(tipo)) {
            fijarPreciosAbierta(idPedido, idNegocio); // RN-025: precio real del negocio que acepta
        }
        insertarHistorial(idPedido, "asignado", apartadosEnBase ? "Apartados completados con stock en base" : null, null, null);
        pushService.notificar((Integer) pedido.get("id_cliente"), "Pedido asignado",
                "Un repartidor tomó tu pedido y lo preparará para entrega.", datosPedido("pedido_asignado", idPedido));

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

        // RN-008 / CU-011: El vehículo debe tener todos los garrafones cargados; no puede iniciar ruta con apartados pendientes en base.
        Integer apartadosEnBase = jdbc.queryForObject("""
                SELECT COALESCE(SUM(cantidad), 0)
                FROM apartados_pedido
                WHERE id_pedido = :id AND id_vehiculo IS NULL""",
                new MapSqlParameterSource("id", idPedido), Integer.class);
        if (apartadosEnBase != null && apartadosEnBase > 0) {
            throw new BusinessRuleException("CARGA_PENDIENTE_EN_BASE",
                    "Tienes " + apartadosEnBase + " garrafones apartados en la base para este pedido. Debes cargarlos al vehículo antes de iniciar la ruta (CU-019).");
        }

        jdbc.update("UPDATE pedidos SET estado_actual = 'en_camino'::estado_pedido WHERE id_pedido = :id",
                new MapSqlParameterSource("id", idPedido));
        insertarHistorial(idPedido, "en_camino", null, lat, lon);
        pushService.notificar((Integer) pedido.get("id_cliente"), "Pedido en camino",
                "Tu repartidor va en camino a tu domicilio.", datosPedido("pedido_en_camino", idPedido));
        return respuesta(idPedido);
    }

    /**
     * CU-019 / CU-011: Carga al vehículo los garrafones que quedaron apartados en la base para este pedido.
     */
    @Transactional
    public PedidoResponse cargarApartadosBase(int idRepartidor, int idPedido) {
        cargarPedidoAsignado(idPedido, idRepartidor);
        // Se cargan al vehículo con el que el repartidor trabaja ahora.
        Integer idVehiculo = repartidorRepository.findById(idRepartidor)
                .map(Repartidor::getIdVehiculoActual).orElse(null);
        if (idVehiculo == null) {
            throw new BusinessRuleException("SIN_JORNADA",
                    "Inicia tu jornada con un vehículo para cargar los garrafones apartados.");
        }

        List<Map<String, Object>> baseApartados = jdbc.queryForList("""
                SELECT a.id_apartado, a.id_lote, a.cantidad
                FROM apartados_pedido a
                WHERE a.id_pedido = :p AND a.id_vehiculo IS NULL
                FOR UPDATE""", new MapSqlParameterSource("p", idPedido));

        if (baseApartados.isEmpty()) {
            return respuesta(idPedido);
        }

        int totalACargar = baseApartados.stream().mapToInt(a -> ((Number) a.get("cantidad")).intValue()).sum();
        int capacidad = vehiculoRepository.findById(idVehiculo).map(VehiculoNegocio::getCapacidadGarrafones).orElse(30);
        Integer ocupadoActual = jdbc.queryForObject("""
                SELECT COALESCE(SUM(cantidad_actual), 0)
                FROM inventario_vehiculo WHERE id_vehiculo = :veh""",
                new MapSqlParameterSource("veh", idVehiculo), Integer.class);
        if ((ocupadoActual != null ? ocupadoActual : 0) + totalACargar > capacidad) {
            throw new BusinessRuleException("CAPACIDAD_VEHICULO_EXCEDIDA",
                    "El vehículo no tiene capacidad suficiente (" + capacidad + ") para cargar los " + totalACargar + " garrafones apartados.");
        }

        for (Map<String, Object> a : baseApartados) {
            int idApartado = ((Number) a.get("id_apartado")).intValue();
            int idLote = ((Number) a.get("id_lote")).intValue();
            int cant = ((Number) a.get("cantidad")).intValue();

            jdbc.update("""
                    UPDATE lotes_inventario
                    SET cantidad_actual = cantidad_actual - :cant,
                        cantidad_apartada = cantidad_apartada - :cant
                    WHERE id_lote = :lote""",
                    new MapSqlParameterSource().addValue("cant", cant).addValue("lote", idLote));

            var p = new MapSqlParameterSource().addValue("veh", idVehiculo).addValue("lote", idLote)
                    .addValue("cant", cant).addValue("ap", idApartado)
                    .addValue("ped", idPedido).addValue("rep", idRepartidor);
            jdbc.update("""
                    INSERT INTO inventario_vehiculo (id_vehiculo, id_lote, id_negocio, cantidad_actual, cantidad_apartada)
                    SELECT :veh, l.id_lote, l.id_negocio, :cant, :cant FROM lotes_inventario l WHERE l.id_lote = :lote
                    ON CONFLICT (id_vehiculo, id_lote)
                    DO UPDATE SET cantidad_actual = inventario_vehiculo.cantidad_actual + :cant,
                                  cantidad_apartada = inventario_vehiculo.cantidad_apartada + :cant""", p);

            // El garrafón ya va en el vehículo: el apartado solo gana el vehículo (v7 [R2]).
            jdbc.update("UPDATE apartados_pedido SET id_vehiculo = :veh WHERE id_apartado = :ap", p);

            jdbc.update("""
                    INSERT INTO movimientos_inventario
                        (id_lote, id_vehiculo, tipo, cantidad, id_pedido, id_repartidor_responsable)
                    VALUES (:lote, :veh, 'traspaso_a_vehiculo'::tipo_movimiento, :cant, :ped, :rep)""", p);
        }

        return respuesta(idPedido);
    }

    // ---------------------------------------------------------------- Helpers

    private static Map<String, String> datosPedido(String tipo, int idPedido) {
        return Map.of("tipo", tipo, "idPedido", String.valueOf(idPedido));
    }

    private Map<String, Object> cargarPedidoParaAceptar(int idPedido) {
        try {
            return jdbc.queryForMap("""
                    SELECT estado_actual::text, tipo_solicitud::text, id_negocio_solicitado,
                           precio_maximo_garrafon, id_cliente, id_repartidor
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

    private void validarStockDisponible(int idVehiculo, int idNegocio, List<Map<String, Object>> detalles) {
        for (Map<String, Object> d : detalles) {
            int idMarca = (Integer) d.get("id_marca");
            int cantidadSolicitada = (Integer) d.get("cantidad_solicitada");

            Integer dispVehiculo = jdbc.queryForObject("""
                    SELECT COALESCE(SUM(iv.cantidad_actual - iv.cantidad_apartada), 0)
                    FROM inventario_vehiculo iv JOIN lotes_inventario l ON l.id_lote = iv.id_lote
                    WHERE iv.id_vehiculo = :veh AND l.id_marca = :marca
                      AND l.fecha_caducidad >= CURRENT_DATE""",
                    new MapSqlParameterSource().addValue("veh", idVehiculo).addValue("marca", idMarca),
                    Integer.class);

            Integer dispBase = jdbc.queryForObject("""
                    SELECT COALESCE(SUM(l.cantidad_actual - l.cantidad_apartada), 0)
                    FROM lotes_inventario l
                    WHERE l.id_negocio = :neg AND l.activo AND l.id_marca = :marca
                      AND l.fecha_caducidad >= CURRENT_DATE""",
                    new MapSqlParameterSource().addValue("neg", idNegocio).addValue("marca", idMarca),
                    Integer.class);

            int totalDisponible = (dispVehiculo != null ? dispVehiculo : 0) + (dispBase != null ? dispBase : 0);
            if (totalDisponible < cantidadSolicitada) {
                throw new BusinessRuleException("STOCK_INSUFICIENTE",
                        "Stock insuficiente: ni el vehículo ni la base cuentan con producto suficiente para cubrir el pedido (RN-008).");
            }
        }
    }

    private int apartarDelVehiculo(int idVehiculo, int idMarca, int cantidad, int idPedido) {
        List<Map<String, Object>> filas = jdbc.queryForList("""
                SELECT iv.id_lote, (iv.cantidad_actual - iv.cantidad_apartada) AS disp
                FROM inventario_vehiculo iv JOIN lotes_inventario l ON l.id_lote = iv.id_lote
                WHERE iv.id_vehiculo = :veh AND l.id_marca = :marca
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
            var p = new MapSqlParameterSource().addValue("veh", idVehiculo).addValue("lote", f.get("id_lote"))
                    .addValue("tomar", tomar).addValue("ped", idPedido);
            jdbc.update("UPDATE inventario_vehiculo SET cantidad_apartada = cantidad_apartada + :tomar "
                    + "WHERE id_vehiculo = :veh AND id_lote = :lote", p);
            jdbc.update("INSERT INTO apartados_pedido (id_pedido, id_lote, id_vehiculo, cantidad) "
                    + "VALUES (:ped, :lote, :veh, :tomar)", p);
            restante -= tomar;
        }
        return restante;
    }

    private void apartarDeLaBase(int idNegocio, int idMarca, int cantidad, int idPedido) {
        List<Map<String, Object>> filas = jdbc.queryForList("""
                SELECT l.id_lote, (l.cantidad_actual - l.cantidad_apartada) AS disp
                FROM lotes_inventario l
                WHERE l.id_negocio = :neg AND l.activo AND l.id_marca = :marca
                  AND l.fecha_caducidad >= CURRENT_DATE AND (l.cantidad_actual - l.cantidad_apartada) > 0
                ORDER BY l.fecha_caducidad, l.id_lote
                FOR UPDATE""",
                new MapSqlParameterSource().addValue("neg", idNegocio).addValue("marca", idMarca));
        int restante = cantidad;
        for (Map<String, Object> f : filas) {
            if (restante == 0) {
                break;
            }
            int disp = ((Number) f.get("disp")).intValue();
            int tomar = Math.min(restante, disp);
            var p = new MapSqlParameterSource().addValue("lote", f.get("id_lote"))
                    .addValue("tomar", tomar).addValue("ped", idPedido);
            jdbc.update("UPDATE lotes_inventario SET cantidad_apartada = cantidad_apartada + :tomar "
                    + "WHERE id_lote = :lote", p);
            jdbc.update("INSERT INTO apartados_pedido (id_pedido, id_lote, cantidad) "
                    + "VALUES (:ped, :lote, :tomar)", p);
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

    private Map<Integer, List<DetalleResponse>> detallesPorPedido(List<Integer> ids, int idRepartidor) {
        return jdbc.query("""
                SELECT dp.id_detalle, dp.id_pedido, dp.id_marca, m.nombre AS nombre_marca,
                       dp.cantidad_solicitada, dp.cantidad_entregada, dp.tiene_envase,
                       CASE WHEN p.tipo_solicitud = 'abierta' AND pn.precio IS NOT NULL
                            THEN pn.precio ELSE dp.precio_unitario END AS precio_unitario,
                       CASE WHEN p.tipo_solicitud = 'abierta' AND pn.precio IS NOT NULL
                            THEN (CASE WHEN dp.tiene_envase THEN NULL ELSE pn.precio_envase END)
                            ELSE dp.precio_envase_unitario END AS precio_envase_unitario,
                       dp.agregada_en_sitio
                FROM detalles_pedido dp
                JOIN pedidos p ON p.id_pedido = dp.id_pedido
                LEFT JOIN repartidores r ON r.id_usuario = :rep
                LEFT JOIN productos_negocio pn ON pn.id_negocio = r.id_negocio AND pn.id_marca = dp.id_marca
                LEFT JOIN marcas m ON m.id_marca = dp.id_marca
                WHERE dp.id_pedido IN (:ids)""",
                new MapSqlParameterSource().addValue("ids", ids).addValue("rep", idRepartidor), rs -> {
                    Map<Integer, List<DetalleResponse>> map = new java.util.HashMap<>();
                    while (rs.next()) {
                        Integer idPedido = rs.getInt("id_pedido");
                        Integer entregada = (Integer) rs.getObject("cantidad_entregada");
                        map.computeIfAbsent(idPedido, k -> new ArrayList<>()).add(new DetalleResponse(
                                rs.getInt("id_detalle"), rs.getInt("id_marca"), rs.getString("nombre_marca"),
                                rs.getInt("cantidad_solicitada"), entregada, rs.getBoolean("tiene_envase"),
                                rs.getBigDecimal("precio_unitario"), rs.getBigDecimal("precio_envase_unitario"),
                                rs.getBoolean("agregada_en_sitio")));
                    }
                    return map;
                });
    }

    private PedidoResponse respuesta(int idPedido) {
        Pedido p = pedidoRepository.findById(idPedido).orElseThrow();
        return enriquecedor.enriquecer(PedidoMapper.toResponse(p), idPedido);
    }
}
