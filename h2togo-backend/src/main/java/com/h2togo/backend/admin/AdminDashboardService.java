package com.h2togo.backend.admin;

import com.h2togo.backend.admin.dto.DashboardResponse;
import com.h2togo.backend.admin.dto.DashboardResponse.Actividad;
import com.h2togo.backend.admin.dto.DashboardResponse.PuntoMapa;
import com.h2togo.backend.admin.dto.DashboardResponse.PuntoSerie;
import com.h2togo.backend.admin.dto.DashboardResponse.TopRepartidor;
import com.h2togo.backend.common.enums.EstadoPedido;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Métricas de solo lectura para el dashboard del panel de administración. Solo ADMIN. */
@Service
public class AdminDashboardService {

    /** Zona de operación: los cortes diarios se hacen en hora de la CDMX, no del servidor (UTC en Azure). */
    static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    private static final int DIAS_SERIE = 30;

    private final NamedParameterJdbcTemplate jdbc;
    private final Clock reloj;

    public AdminDashboardService(NamedParameterJdbcTemplate jdbc, Clock reloj) {
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public DashboardResponse resumen() {
        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        LocalDate hoy = ahora.atZoneSameInstant(ZONA).toLocalDate();
        OffsetDateTime inicioHoy = inicioDe(hoy);
        OffsetDateTime inicioAyer = inicioDe(hoy.minusDays(1));
        OffsetDateTime inicioManana = inicioDe(hoy.plusDays(1));
        OffsetDateTime inicioSerie = inicioDe(hoy.minusDays(DIAS_SERIE - 1));

        var p = new MapSqlParameterSource()
                .addValue("hoy", inicioHoy)
                .addValue("ayer", inicioAyer)
                .addValue("manana", inicioManana)
                .addValue("serie", inicioSerie)
                .addValue("hace7", ahora.minusDays(7))
                .addValue("hace30", ahora.minusDays(30))
                .addValue("zona", ZONA.getId());

        long pedidosHoy = contar("SELECT COUNT(*) FROM pedidos WHERE fecha_creacion >= :hoy AND fecha_creacion < :manana", p);
        long pedidosAyer = contar("SELECT COUNT(*) FROM pedidos WHERE fecha_creacion >= :ayer AND fecha_creacion < :hoy", p);
        BigDecimal ingresosHoy = sumar("""
                SELECT COALESCE(SUM(total_pagar), 0) FROM pedidos
                WHERE estado_actual = 'entregado' AND fecha_entrega >= :hoy AND fecha_entrega < :manana""", p);
        BigDecimal ingresosAyer = sumar("""
                SELECT COALESCE(SUM(total_pagar), 0) FROM pedidos
                WHERE estado_actual = 'entregado' AND fecha_entrega >= :ayer AND fecha_entrega < :hoy""", p);

        Map<String, Object> cierres = jdbc.queryForMap("""
                SELECT COUNT(*) FILTER (WHERE estado_actual = 'entregado')    AS ok,
                       COUNT(*) FILTER (WHERE estado_actual = 'no_entregado') AS ko
                FROM pedidos WHERE fecha_creacion >= :hace30""", p);
        long ok = ((Number) cierres.get("ok")).longValue();
        long ko = ((Number) cierres.get("ko")).longValue();
        Double tasaExito = ok + ko == 0 ? null : (double) ok / (ok + ko);

        long usuariosNuevos = contar("SELECT COUNT(*) FROM usuarios WHERE fecha_registro >= :hace7", p);
        long usuariosTotales = contar("SELECT COUNT(*) FROM usuarios", p);
        long solicitudesPendientes = contar(
                "SELECT COUNT(*) FROM solicitudes_cambio_perfil WHERE estado = 'pendiente'", p);

        Map<String, Long> estadoActual = porEstado("SELECT estado_actual::text AS e, COUNT(*) AS n FROM pedidos GROUP BY 1", p);
        Map<String, Long> distribucionHoy = porEstado("""
                SELECT estado_actual::text AS e, COUNT(*) AS n FROM pedidos
                WHERE fecha_creacion >= :hoy AND fecha_creacion < :manana GROUP BY 1""", p);

        Map<String, Object> reps = jdbc.queryForMap("""
                SELECT COUNT(*) FILTER (WHERE r.estado_operativo) AS en_linea, COUNT(*) AS activos
                FROM repartidores r JOIN usuarios u ON u.id_usuario = r.id_usuario
                WHERE u.cuenta_activa""", p);

        return new DashboardResponse(ahora, pedidosHoy, pedidosAyer, ingresosHoy, ingresosAyer, tasaExito,
                usuariosNuevos, usuariosTotales, solicitudesPendientes, estadoActual, distribucionHoy,
                ((Number) reps.get("en_linea")).longValue(), ((Number) reps.get("activos")).longValue(),
                serie(hoy, p), topRepartidores(p), actividad(), mapa());
    }

    private static OffsetDateTime inicioDe(LocalDate dia) {
        return dia.atStartOfDay(ZONA).toOffsetDateTime();
    }

    private long contar(String sql, MapSqlParameterSource p) {
        Long n = jdbc.queryForObject(sql, p, Long.class);
        return n == null ? 0 : n;
    }

    private BigDecimal sumar(String sql, MapSqlParameterSource p) {
        BigDecimal v = jdbc.queryForObject(sql, p, BigDecimal.class);
        return v == null ? BigDecimal.ZERO : v;
    }

    /** Devuelve todos los estados (en orden del enum) aunque alguno tenga 0. */
    private Map<String, Long> porEstado(String sql, MapSqlParameterSource p) {
        Map<String, Long> res = new LinkedHashMap<>();
        for (EstadoPedido e : EstadoPedido.values()) {
            res.put(e.name(), 0L);
        }
        jdbc.query(sql, p, rs -> {
            res.put(rs.getString("e"), rs.getLong("n"));
        });
        return res;
    }

    /** Pedidos creados por día (en hora local) de los últimos 30 días; los días sin pedidos van en 0. */
    private List<PuntoSerie> serie(LocalDate hoy, MapSqlParameterSource p) {
        Map<LocalDate, long[]> porDia = new HashMap<>();
        jdbc.query("""
                SELECT (fecha_creacion AT TIME ZONE :zona)::date AS dia,
                       COUNT(*) AS total,
                       COUNT(*) FILTER (WHERE estado_actual = 'entregado') AS entregados
                FROM pedidos WHERE fecha_creacion >= :serie AND fecha_creacion < :manana
                GROUP BY 1""", p, rs -> {
            porDia.put(rs.getObject("dia", LocalDate.class),
                    new long[]{rs.getLong("total"), rs.getLong("entregados")});
        });
        List<PuntoSerie> serie = new ArrayList<>(DIAS_SERIE);
        for (int i = DIAS_SERIE - 1; i >= 0; i--) {
            LocalDate dia = hoy.minusDays(i);
            long[] v = porDia.getOrDefault(dia, new long[]{0, 0});
            serie.add(new PuntoSerie(dia, v[0], v[1]));
        }
        return serie;
    }

    private List<TopRepartidor> topRepartidores(MapSqlParameterSource p) {
        return jdbc.query("""
                SELECT u.id_usuario, u.nombre || ' ' || u.apellidos AS nombre, n.nombre_comercial,
                       COUNT(*) AS entregas, COALESCE(SUM(pe.total_pagar), 0) AS ingresos
                FROM pedidos pe
                JOIN usuarios u ON u.id_usuario = pe.id_repartidor
                JOIN repartidores r ON r.id_usuario = pe.id_repartidor
                JOIN negocios n ON n.id_negocio = r.id_negocio
                WHERE pe.estado_actual = 'entregado' AND pe.fecha_entrega >= :hace7
                GROUP BY u.id_usuario, u.nombre, u.apellidos, n.nombre_comercial
                ORDER BY entregas DESC, ingresos DESC
                LIMIT 5""", p, (rs, i) -> new TopRepartidor(rs.getInt("id_usuario"), rs.getString("nombre"),
                rs.getString("nombre_comercial"), rs.getLong("entregas"), rs.getBigDecimal("ingresos")));
    }

    private List<Actividad> actividad() {
        return jdbc.query("""
                SELECT id_pedido, estado::text AS estado, notas_adicionales, fecha_cambio
                FROM historial_estados_pedido ORDER BY fecha_cambio DESC, id_historial DESC LIMIT 8""",
                (rs, i) -> new Actividad(rs.getInt("id_pedido"), rs.getString("estado"),
                        rs.getString("notas_adicionales"), rs.getObject("fecha_cambio", OffsetDateTime.class)));
    }

    /** Repartidores en línea con ubicación conocida y pedidos activos en su domicilio de entrega. */
    private List<PuntoMapa> mapa() {
        List<PuntoMapa> puntos = new ArrayList<>(jdbc.query("""
                SELECT u.id_usuario, u.nombre || ' ' || u.apellidos AS etiqueta,
                       ST_Y(r.ubicacion_actual::geometry) AS lat, ST_X(r.ubicacion_actual::geometry) AS lon
                FROM repartidores r JOIN usuarios u ON u.id_usuario = r.id_usuario
                WHERE r.estado_operativo AND r.ubicacion_actual IS NOT NULL AND u.cuenta_activa""",
                (rs, i) -> new PuntoMapa("repartidor", rs.getInt("id_usuario"), rs.getString("etiqueta"),
                        rs.getDouble("lat"), rs.getDouble("lon"))));
        puntos.addAll(jdbc.query("""
                SELECT pe.id_pedido, pe.estado_actual::text AS estado,
                       ST_Y(d.ubicacion::geometry) AS lat, ST_X(d.ubicacion::geometry) AS lon
                FROM pedidos pe JOIN direcciones_clientes d ON d.id_direccion = pe.id_direccion_entrega
                WHERE pe.estado_actual IN ('pendiente', 'asignado', 'en_camino')
                ORDER BY pe.fecha_creacion DESC LIMIT 200""",
                (rs, i) -> new PuntoMapa("pedido", rs.getInt("id_pedido"),
                        "#" + rs.getInt("id_pedido") + " · " + rs.getString("estado"),
                        rs.getDouble("lat"), rs.getDouble("lon"))));
        return puntos;
    }
}
