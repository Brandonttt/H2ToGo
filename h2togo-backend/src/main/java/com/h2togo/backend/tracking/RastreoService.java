package com.h2togo.backend.tracking;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rastreo en vivo de pedidos (CU-006): difusión a {@code /topic/pedidos/{id}/ubicacion}, de modo
 * que el canal STOMP y el reporte REST se comportan igual. Quién puede publicar o ver lo decide
 * {@link AccesoRastreo}.
 */
@Service
public class RastreoService {

    /** Estados en los que el pedido tiene repartidor en ruta y por lo tanto se rastrea. */
    static final String ACTIVOS = "('asignado', 'en_camino')";

    private final NamedParameterJdbcTemplate jdbc;
    private final UbicacionService ubicacionService;
    private final SimpMessagingTemplate messaging;

    public RastreoService(NamedParameterJdbcTemplate jdbc, UbicacionService ubicacionService,
            SimpMessagingTemplate messaging) {
        this.jdbc = jdbc;
        this.ubicacionService = ubicacionService;
        this.messaging = messaging;
    }

    public static String topico(int idPedido) {
        return "/topic/pedidos/" + idPedido + "/ubicacion";
    }

    /**
     * Persiste la posición (con el throttle y el aviso de proximidad de {@link UbicacionService})
     * y la reenvía a los clientes de todos los pedidos activos del repartidor.
     */
    @Transactional
    public void publicar(int idRepartidor, double lat, double lon) {
        ubicacionService.reportar(idRepartidor, lat, lon);
        List<Integer> activos = jdbc.queryForList(
                "SELECT id_pedido FROM pedidos WHERE id_repartidor = :u AND estado_actual IN " + ACTIVOS,
                new MapSqlParameterSource("u", idRepartidor), Integer.class);
        Object payload = Map.of("lat", lat, "lon", lon, "timestamp", OffsetDateTime.now().toString());
        for (Integer idPedido : activos) {
            messaging.convertAndSend(topico(idPedido), payload);
        }
    }

    /**
     * Última posición conocida del repartidor de un pedido activo, para pintar el mapa antes del
     * primer mensaje del WebSocket (o como respaldo si el WebSocket no conecta).
     */
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> ultimaUbicacion(int idPedido) {
        return jdbc.query("""
                SELECT ST_Y(r.ubicacion_actual::geometry) AS lat, ST_X(r.ubicacion_actual::geometry) AS lon,
                       r.ubicacion_reportada_en AS timestamp
                FROM pedidos p JOIN repartidores r ON r.id_usuario = p.id_repartidor
                WHERE p.id_pedido = :p AND r.ubicacion_actual IS NOT NULL
                  AND p.estado_actual IN """ + ACTIVOS,
                new MapSqlParameterSource("p", idPedido),
                rs -> rs.next()
                        ? Optional.of(Map.<String, Object>of("lat", rs.getDouble("lat"), "lon", rs.getDouble("lon"),
                                "timestamp", rs.getObject("timestamp", OffsetDateTime.class).toString()))
                        : Optional.empty());
    }

}
