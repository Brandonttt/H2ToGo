package com.h2togo.backend.routing;

import com.h2togo.backend.common.BusinessRuleException;
import com.h2togo.backend.common.NotFoundException;
import com.h2togo.backend.routing.dto.RouteRequest;
import com.h2togo.backend.routing.dto.RouteResponse;
import com.h2togo.backend.routing.service.CalculoRutaService;
import com.h2togo.backend.routing.service.OsmGraphLoader;
import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ruta de entrega de un pedido (CU-011): origen = ubicación actual del repartidor asignado,
 * destino = dirección de entrega; A* sobre el grafo OSM. Solo el repartidor asignado.
 */
@Service
public class RutaService {

    private final NamedParameterJdbcTemplate jdbc;
    private final CalculoRutaService calculoRutaService;
    private final OsmGraphLoader osmGraphLoader;

    public RutaService(NamedParameterJdbcTemplate jdbc, CalculoRutaService calculoRutaService,
            OsmGraphLoader osmGraphLoader) {
        this.jdbc = jdbc;
        this.calculoRutaService = calculoRutaService;
        this.osmGraphLoader = osmGraphLoader;
    }

    @Transactional(readOnly = true)
    public RouteResponse ruta(int idRepartidor, int idPedido) {
        Map<String, Object> pedido;
        try {
            pedido = jdbc.queryForMap(
                    "SELECT estado_actual::text, id_repartidor, id_direccion_entrega FROM pedidos WHERE id_pedido = :id",
                    new MapSqlParameterSource("id", idPedido));
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("PEDIDO_NO_ENCONTRADO", "Pedido no encontrado.");
        }
        if (!Integer.valueOf(idRepartidor).equals(pedido.get("id_repartidor"))) {
            throw new AccessDeniedException("Solo el repartidor asignado puede ver la ruta (RN-016).");
        }
        String estado = (String) pedido.get("estado_actual");
        if (!"asignado".equals(estado) && !"en_camino".equals(estado)) {
            throw new BusinessRuleException("ESTADO_INVALIDO",
                    "La ruta solo está disponible para pedidos asignados o en camino.");
        }

        Map<String, Object> origen = jdbc.queryForMap("""
                SELECT ST_Y(ubicacion_actual::geometry) AS lat, ST_X(ubicacion_actual::geometry) AS lon
                FROM repartidores WHERE id_usuario = :rep""",
                new MapSqlParameterSource("rep", idRepartidor));
        if (origen.get("lat") == null) {
            throw new BusinessRuleException("SIN_UBICACION",
                    "El repartidor no tiene ubicación reportada; activa el GPS.");
        }
        Map<String, Object> destino = jdbc.queryForMap("""
                SELECT ST_Y(ubicacion::geometry) AS lat, ST_X(ubicacion::geometry) AS lon
                FROM direcciones_clientes WHERE id_direccion = :dir""",
                new MapSqlParameterSource("dir", pedido.get("id_direccion_entrega")));

        RouteRequest req = new RouteRequest();
        req.setOrigenLat(((Number) origen.get("lat")).doubleValue());
        req.setOrigenLon(((Number) origen.get("lon")).doubleValue());
        req.setDestinoLat(((Number) destino.get("lat")).doubleValue());
        req.setDestinoLon(((Number) destino.get("lon")).doubleValue());

        return calculoRutaService.calcularRuta(osmGraphLoader.getGrafo(), req);
    }
}
