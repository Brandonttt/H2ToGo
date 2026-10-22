package com.h2togo.backend.planeacion;

import com.h2togo.backend.common.BusinessRuleException;
import com.h2togo.backend.common.ConflictException;
import com.h2togo.backend.common.NotFoundException;
import com.h2togo.backend.notificaciones.PushService;
import com.h2togo.backend.pedidos.AsignacionService;
import com.h2togo.backend.pedidos.AvisoNuevoPedido;
import com.h2togo.backend.planeacion.OptimizadorRuta.Problema;
import com.h2togo.backend.planeacion.OptimizadorRuta.Solucion;
import com.h2togo.backend.planeacion.OptimizadorRuta.Visita;
import com.h2togo.backend.planeacion.dto.AgendaPedidoResponse;
import com.h2togo.backend.planeacion.dto.RutaOptimaResponse;
import com.h2togo.backend.planeacion.dto.RutaOptimaResponse.Omitido;
import com.h2togo.backend.planeacion.dto.RutaOptimaResponse.Parada;
import com.h2togo.backend.routing.domain.Grafo;
import com.h2togo.backend.routing.dto.RouteRequest;
import com.h2togo.backend.routing.dto.RouteResponse;
import com.h2togo.backend.routing.dto.RouteResponse.Coordenada;
import com.h2togo.backend.routing.service.CalculoRutaService;
import com.h2togo.backend.routing.service.MatrizDistancias;
import com.h2togo.backend.routing.service.OsmGraphLoader;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Planeación de la ruta del día del repartidor.
 * <ul>
 *   <li><b>Agenda</b>: los pedidos programados de hoy que su negocio puede surtir. El repartidor
 *       los <em>reserva</em> con anticipación (RN-027 ajustada: ya no quedan ocultos hasta su
 *       hora). La reserva solo fija {@code id_repartidor}; el pedido sigue programado, el cliente
 *       aún puede cancelarlo y los garrafones se apartan hasta aceptarlo.</li>
 *   <li><b>Ruta óptima</b>: ordena todas sus paradas (asignadas, en camino y reservadas) con
 *       OR-Tools sobre la matriz de distancias por calles. Se calcula al pedirla, así que una
 *       cancelación ya no aparece en la siguiente consulta (RN-009).</li>
 * </ul>
 */
@Service
public class PlaneacionService {

    static final ZoneId ZONA = ZoneId.of("America/Mexico_City");
    static final String ALGORITMO = "OR-Tools · TSP con ventanas de horario y capacidad · Guided Local Search";

    private final NamedParameterJdbcTemplate jdbc;
    private final OptimizadorRuta optimizador;
    private final MatrizDistancias matriz;
    private final CalculoRutaService calculoRuta;
    private final OsmGraphLoader grafos;
    private final PushService pushService;
    private final AvisoNuevoPedido avisoNuevoPedido;
    private final Clock clock;
    private final double velocidadKmh;
    private final int servicioMin;
    private final int ventanaMin;

    public PlaneacionService(NamedParameterJdbcTemplate jdbc, OptimizadorRuta optimizador,
            MatrizDistancias matriz, CalculoRutaService calculoRuta, OsmGraphLoader grafos,
            PushService pushService, AvisoNuevoPedido avisoNuevoPedido, Clock clock,
            @Value("${h2togo.planeacion.velocidad-kmh:20}") double velocidadKmh,
            @Value("${h2togo.planeacion.servicio-min:5}") int servicioMin,
            @Value("${h2togo.planeacion.ventana-min:30}") int ventanaMin) {
        this.jdbc = jdbc;
        this.optimizador = optimizador;
        this.matriz = matriz;
        this.calculoRuta = calculoRuta;
        this.grafos = grafos;
        this.pushService = pushService;
        this.avisoNuevoPedido = avisoNuevoPedido;
        this.clock = clock;
        this.velocidadKmh = velocidadKmh;
        this.servicioMin = servicioMin;
        this.ventanaMin = ventanaMin;
    }

    // ---------------------------------------------------------------- Agenda

    @Transactional(readOnly = true)
    public List<AgendaPedidoResponse> agenda(int idRepartidor) {
        OffsetDateTime[] hoy = hoy();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT p.id_pedido, p.fecha_programada, p.estado_actual::text AS estado, p.id_repartidor,
                       p.garrafones_totales,
                       CASE
                         WHEN p.tipo_solicitud = 'abierta' THEN COALESCE((
                             SELECT SUM(dp.cantidad_solicitada * (pn.precio + CASE WHEN dp.tiene_envase THEN 0 ELSE COALESCE(pn.precio_envase, 0) END))
                             FROM detalles_pedido dp
                             JOIN productos_negocio pn ON pn.id_negocio = r.id_negocio AND pn.id_marca = dp.id_marca
                             WHERE dp.id_pedido = p.id_pedido
                         ), p.total_pagar)
                         ELSE p.total_pagar
                       END AS total_pagar,
                       p.tipo_solicitud::text AS tipo,
                       CONCAT(u.nombre, ' ', u.apellidos) AS nombre_cliente, u.telefono,
                       CONCAT_WS(', ', NULLIF(CONCAT_WS(' ', d.calle, d.numero_exterior), ''), NULLIF(d.colonia, '')) AS direccion,
                       d.colonia,
                       ST_Y(d.ubicacion::geometry) AS lat, ST_X(d.ubicacion::geometry) AS lon,
                       ST_Distance(d.ubicacion, COALESCE(r.ubicacion_actual, n.ubicacion_base)) AS distancia_m
                FROM pedidos p
                JOIN direcciones_clientes d ON d.id_direccion = p.id_direccion_entrega
                JOIN usuarios u ON u.id_usuario = p.id_cliente
                JOIN repartidores r ON r.id_usuario = :rep
                LEFT JOIN negocios n ON n.id_negocio = r.id_negocio
                WHERE p.es_programado AND d.en_zona_cobertura
                  AND p.fecha_programada >= :inicio AND p.fecha_programada < :fin
                  AND ( (p.estado_actual = 'pendiente_programado' AND p.id_repartidor IS NULL)
                     OR (p.estado_actual IN ('pendiente_programado', 'pendiente') AND p.id_repartidor = :rep) )
                  AND """ + AsignacionService.COMPATIBLE_CON_REPARTIDOR + """
                ORDER BY p.fecha_programada, p.id_pedido""",
                new MapSqlParameterSource().addValue("rep", idRepartidor)
                        .addValue("inicio", hoy[0]).addValue("fin", hoy[1]));
        return rows.stream().map(r -> new AgendaPedidoResponse(
                (Integer) r.get("id_pedido"),
                fecha(r.get("fecha_programada")),
                (String) r.get("estado"),
                Integer.valueOf(idRepartidor).equals(r.get("id_repartidor")),
                (Integer) r.get("garrafones_totales"),
                (BigDecimal) r.get("total_pagar"),
                (String) r.get("tipo"),
                (String) r.get("nombre_cliente"),
                (String) r.get("telefono"),
                (String) r.get("direccion"),
                (String) r.get("colonia"),
                numero(r.get("lat")),
                numero(r.get("lon")),
                numero(r.get("distancia_m")))).toList();
    }

    /** Reserva un programado de hoy para la ruta del repartidor. Solo uno gana (UPDATE condicional). */
    @Transactional
    public void reservar(int idRepartidor, int idPedido) {
        OffsetDateTime[] hoy = hoy();
        Map<String, Object> p = cargar(idPedido);
        if (Integer.valueOf(idRepartidor).equals(p.get("id_repartidor"))) {
            return; // ya era suyo
        }
        if (!"pendiente_programado".equals(p.get("estado"))) {
            throw new ConflictException("PEDIDO_NO_RESERVABLE", "El pedido ya no está en la agenda.");
        }
        OffsetDateTime fecha = fecha(p.get("fecha_programada"));
        if (fecha == null || fecha.isBefore(hoy[0]) || !fecha.isBefore(hoy[1])) {
            throw new BusinessRuleException("FUERA_DE_AGENDA", "Solo puedes reservar pedidos programados para hoy.");
        }
        Boolean compatible = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM pedidos p JOIN repartidores r ON r.id_usuario = :rep
                               WHERE p.id_pedido = :id AND """ + AsignacionService.COMPATIBLE_CON_REPARTIDOR + ")",
                new MapSqlParameterSource().addValue("rep", idRepartidor).addValue("id", idPedido), Boolean.class);
        if (!Boolean.TRUE.equals(compatible)) {
            throw new BusinessRuleException("PEDIDO_NO_COMPATIBLE", "Tu purificadora no puede surtir este pedido.");
        }
        int filas = jdbc.update("""
                UPDATE pedidos SET id_repartidor = :rep
                WHERE id_pedido = :id AND estado_actual = 'pendiente_programado' AND id_repartidor IS NULL""",
                new MapSqlParameterSource().addValue("rep", idRepartidor).addValue("id", idPedido));
        if (filas == 0) {
            throw new ConflictException("PEDIDO_YA_RESERVADO", "Otro repartidor reservó este pedido primero.");
        }
        pushService.notificar((Integer) p.get("id_cliente"), "Tu pedido programado tiene repartidor",
                "Un repartidor ya lo incluyó en su ruta de hoy.",
                Map.of("tipo", "pedido_reservado", "idPedido", String.valueOf(idPedido)));
    }

    /** Suelta una reserva; si el pedido ya se había activado, vuelve a ofrecerse a todos. */
    @Transactional
    public void liberar(int idRepartidor, int idPedido) {
        Map<String, Object> p = cargar(idPedido);
        int filas = jdbc.update("""
                UPDATE pedidos SET id_repartidor = NULL
                WHERE id_pedido = :id AND id_repartidor = :rep
                  AND estado_actual IN ('pendiente_programado', 'pendiente')""",
                new MapSqlParameterSource().addValue("rep", idRepartidor).addValue("id", idPedido));
        if (filas == 0) {
            throw new BusinessRuleException("PEDIDO_NO_LIBERABLE",
                    "Solo puedes liberar pedidos que reservaste y que aún no aceptas.");
        }
        if ("pendiente".equals(p.get("estado"))) {
            avisoNuevoPedido.avisar(idPedido);
        }
    }

    // ---------------------------------------------------------------- Ruta óptima

    @Transactional(readOnly = true)
    public RutaOptimaResponse rutaOptima(int idRepartidor) {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        Map<String, Object> rep = jdbc.queryForMap("""
                SELECT ST_Y(COALESCE(r.ubicacion_actual, n.ubicacion_base)::geometry) AS lat,
                       ST_X(COALESCE(r.ubicacion_actual, n.ubicacion_base)::geometry) AS lon,
                       COALESCE(v.capacidad_garrafones,
                                (SELECT MAX(v2.capacidad_garrafones) FROM vehiculos_negocio v2
                                 WHERE v2.id_negocio = r.id_negocio AND v2.activo)) AS capacidad
                FROM repartidores r
                LEFT JOIN negocios n ON n.id_negocio = r.id_negocio
                LEFT JOIN vehiculos_negocio v ON v.id_vehiculo = r.id_vehiculo_actual
                WHERE r.id_usuario = :rep""", new MapSqlParameterSource("rep", idRepartidor));
        if (rep.get("lat") == null) {
            throw new BusinessRuleException("SIN_UBICACION", "Activa el GPS para calcular tu ruta.");
        }
        double origenLat = numero(rep.get("lat"));
        double origenLon = numero(rep.get("lon"));
        int capacidad = rep.get("capacidad") == null ? Integer.MAX_VALUE : ((Number) rep.get("capacidad")).intValue();

        List<Map<String, Object>> paradas = jdbc.queryForList("""
                SELECT p.id_pedido, p.estado_actual::text AS estado, p.garrafones_totales, p.fecha_programada,
                       CONCAT(u.nombre, ' ', u.apellidos) AS nombre_cliente, u.telefono,
                       CONCAT_WS(', ', NULLIF(CONCAT_WS(' ', d.calle, d.numero_exterior), ''), NULLIF(d.colonia, '')) AS direccion,
                       ST_Y(d.ubicacion::geometry) AS lat, ST_X(d.ubicacion::geometry) AS lon
                FROM pedidos p
                JOIN direcciones_clientes d ON d.id_direccion = p.id_direccion_entrega
                JOIN usuarios u ON u.id_usuario = p.id_cliente
                WHERE p.id_repartidor = :rep
                  AND ( p.estado_actual IN ('asignado', 'en_camino')
                     OR (p.estado_actual IN ('pendiente_programado', 'pendiente') AND p.fecha_programada < :fin) )
                ORDER BY p.id_pedido""",
                new MapSqlParameterSource().addValue("rep", idRepartidor).addValue("fin", hoy()[1]));

        int n = paradas.size() + 1;
        List<double[]> puntos = new ArrayList<>();
        puntos.add(new double[] {origenLat, origenLon});
        int[] demandas = new int[n];
        long[][] ventanas = new long[n][];
        for (int i = 1; i < n; i++) {
            Map<String, Object> p = paradas.get(i - 1);
            puntos.add(new double[] {numero(p.get("lat")), numero(p.get("lon"))});
            demandas[i] = (Integer) p.get("garrafones_totales");
            ventanas[i] = ventana(p, ahora);
        }

        Grafo grafo = grafos.getGrafo();
        long[][] metros = aMetros(matriz.kilometros(grafo, puntos));
        Solucion solucion = optimizador.resolver(new Problema(metros, demandas, ventanas,
                capacidad, servicioMin, velocidadKmh, limiteMs(n)));

        List<Parada> ordenadas = new ArrayList<>();
        List<List<Coordenada>> tramos = new ArrayList<>();
        int anterior = 0;
        int garrafones = 0;
        long duracion = 0;
        for (Visita v : solucion.visitas()) {
            Map<String, Object> p = paradas.get(v.nodo() - 1);
            ordenadas.add(new Parada(ordenadas.size() + 1, (Integer) p.get("id_pedido"), (String) p.get("estado"),
                    (String) p.get("nombre_cliente"), (String) p.get("telefono"), (String) p.get("direccion"),
                    puntos.get(v.nodo())[0], puntos.get(v.nodo())[1], demandas[v.nodo()],
                    fecha(p.get("fecha_programada")), ahora.plusMinutes(v.llegadaMin()),
                    redondear(Math.max(0, v.metrosDesdeAnterior()) / 1000.0),
                    // La ventana [0, 0] del pedido en camino solo sirve para ponerlo primero.
                    v.tarde() && p.get("fecha_programada") != null));
            tramos.add(tramo(grafo, puntos.get(anterior), puntos.get(v.nodo())));
            garrafones += demandas[v.nodo()];
            duracion = v.llegadaMin() + servicioMin;
            anterior = v.nodo();
        }
        List<Omitido> omitidos = solucion.omitidos().stream().map(nodo -> {
            Map<String, Object> p = paradas.get(nodo - 1);
            return new Omitido((Integer) p.get("id_pedido"), (String) p.get("nombre_cliente"),
                    (String) p.get("direccion"), sinCamino(metros, nodo)
                            ? "No hay camino por calles hasta este domicilio."
                            : "No cabe en el vehículo: excede su capacidad de " + capacidad + " garrafones.");
        }).toList();

        return new RutaOptimaResponse(ahora, ALGORITMO, origenLat, origenLon,
                redondear(Math.max(0, solucion.metrosTotales()) / 1000.0), duracion,
                capacidad == Integer.MAX_VALUE ? 0 : capacidad, garrafones,
                ordenadas, omitidos, MatrizDistancias.unir(tramos));
    }

    // ---------------------------------------------------------------- Apoyo

    /** [inicio, fin) del día de hoy en la Ciudad de México. */
    private OffsetDateTime[] hoy() {
        LocalDate dia = LocalDate.now(clock.withZone(ZONA));
        return new OffsetDateTime[] {
                dia.atStartOfDay(ZONA).toOffsetDateTime(),
                dia.plusDays(1).atStartOfDay(ZONA).toOffsetDateTime()};
    }

    /**
     * Ventana en minutos desde ahora: programados, su hora ± margen; el que ya va en camino debe
     * ser la primera parada (ventana [0, 0], llegar tarde solo penaliza); los demás, sin horario.
     */
    private long[] ventana(Map<String, Object> p, OffsetDateTime ahora) {
        if ("en_camino".equals(p.get("estado"))) {
            return new long[] {0, 0};
        }
        OffsetDateTime programada = fecha(p.get("fecha_programada"));
        if (programada == null) {
            return null;
        }
        long minutos = Duration.between(ahora, programada).toMinutes();
        return new long[] {minutos - ventanaMin, minutos + ventanaMin};
    }

    /** km → metros; sin camino por calles → −1 (el optimizador lo trata como inaccesible). */
    private static long[][] aMetros(double[][] km) {
        long[][] m = new long[km.length][km.length];
        for (int i = 0; i < km.length; i++) {
            for (int j = 0; j < km.length; j++) {
                m[i][j] = Double.isInfinite(km[i][j]) ? -1 : Math.round(km[i][j] * 1000);
            }
        }
        return m;
    }

    /**
     * Dijkstra desde la salida ya considera todos los caminos posibles (también los que pasan por
     * otras paradas): si desde ahí no se alcanza el domicilio, no hay forma de llegar.
     */
    private static boolean sinCamino(long[][] metros, int nodo) {
        return metros[0][nodo] < 0;
    }

    /** Pocas paradas convergen rápido; con más se le da más tiempo a la búsqueda local (máx. 3 s). */
    private static int limiteMs(int nodos) {
        return Math.min(3000, 300 + 150 * nodos);
    }

    /** Polilínea por calles de un tramo (A*); si no hay camino, línea recta. */
    private List<Coordenada> tramo(Grafo grafo, double[] desde, double[] hasta) {
        RouteRequest req = new RouteRequest();
        req.setOrigenLat(desde[0]);
        req.setOrigenLon(desde[1]);
        req.setDestinoLat(hasta[0]);
        req.setDestinoLon(hasta[1]);
        RouteResponse r = calculoRuta.calcularRuta(grafo, req);
        if (r.isEncontrada() && r.getCoordenadas() != null && !r.getCoordenadas().isEmpty()) {
            return r.getCoordenadas();
        }
        return List.of(new Coordenada(desde[0], desde[1]), new Coordenada(hasta[0], hasta[1]));
    }

    private Map<String, Object> cargar(int idPedido) {
        try {
            return jdbc.queryForMap("""
                    SELECT estado_actual::text AS estado, id_repartidor, id_cliente, fecha_programada
                    FROM pedidos WHERE id_pedido = :id""", new MapSqlParameterSource("id", idPedido));
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("PEDIDO_NO_ENCONTRADO", "Pedido no encontrado.");
        }
    }

    private static OffsetDateTime fecha(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof OffsetDateTime o) {
            return o;
        }
        if (valor instanceof java.sql.Timestamp t) {
            return t.toInstant().atZone(ZONA).toOffsetDateTime();
        }
        return OffsetDateTime.parse(valor.toString());
    }

    private static Double numero(Object valor) {
        return valor == null ? null : ((Number) valor).doubleValue();
    }

    private static double redondear(double km) {
        return Math.round(km * 100.0) / 100.0;
    }
}
