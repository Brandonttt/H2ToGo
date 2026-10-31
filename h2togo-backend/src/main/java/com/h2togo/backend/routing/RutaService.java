package com.h2togo.backend.routing;

import com.h2togo.backend.common.BusinessRuleException;
import com.h2togo.backend.common.NotFoundException;
import com.h2togo.backend.routing.dto.RouteRequest;
import com.h2togo.backend.routing.dto.RouteResponse;
import com.h2togo.backend.routing.service.CalculoRutaService;
import com.h2togo.backend.routing.service.OsmGraphLoader;
import com.h2togo.backend.tracking.UbicacionService;
import java.util.Map;
import java.util.Set;
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

    private static final Set<String> ESTADOS_RUTA_REPARTIDOR =
            Set.of("asignado", "en_camino", "pendiente", "pendiente_programado");

    private final NamedParameterJdbcTemplate jdbc;
    private final CalculoRutaService calculoRutaService;
    private final OsmGraphLoader osmGraphLoader;
    private final UbicacionService ubicacionService;

    public RutaService(NamedParameterJdbcTemplate jdbc, CalculoRutaService calculoRutaService,
            OsmGraphLoader osmGraphLoader, UbicacionService ubicacionService) {
        this.jdbc = jdbc;
        this.calculoRutaService = calculoRutaService;
        this.osmGraphLoader = osmGraphLoader;
        this.ubicacionService = ubicacionService;
    }

    @Transactional
    public RouteResponse ruta(int idRepartidor, int idPedido) {
        return ruta(idRepartidor, idPedido, null, null);
    }

    @Transactional
    public RouteResponse ruta(int idRepartidor, int idPedido, Double origenLat, Double origenLon) {
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
        if (!ESTADOS_RUTA_REPARTIDOR.contains(estado)) {
            throw new BusinessRuleException("ESTADO_INVALIDO",
                    "La ruta solo está disponible para pedidos asignados o en camino.");
        }

        Integer idDireccion = (Integer) pedido.get("id_direccion_entrega");
        if (coordenadaValida(origenLat, origenLon)) {
            ubicacionService.fijar(idRepartidor, origenLat, origenLon);
            return calcularDesdeCoordenada(origenLat, origenLon, idDireccion);
        }
        return calcular(idRepartidor, idDireccion);
    }

    /**
     * Ruta por calles que ve el cliente en el seguimiento (CU-006): desde la última ubicación del
     * repartidor hasta su domicilio. Solo el cliente dueño del pedido y mientras va en camino.
     */
    @Transactional(readOnly = true)
    public RouteResponse rutaSeguimiento(int idCliente, int idPedido) {
        return rutaSeguimiento(idCliente, idPedido, null, null);
    }

    @Transactional(readOnly = true)
    public RouteResponse rutaSeguimiento(int idCliente, int idPedido, Double origenLat, Double origenLon) {
        Map<String, Object> pedido;
        try {
            pedido = jdbc.queryForMap(
                    "SELECT estado_actual::text, id_cliente, id_repartidor, id_direccion_entrega FROM pedidos WHERE id_pedido = :id",
                    new MapSqlParameterSource("id", idPedido));
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("PEDIDO_NO_ENCONTRADO", "Pedido no encontrado.");
        }
        if (!Integer.valueOf(idCliente).equals(pedido.get("id_cliente"))) {
            throw new AccessDeniedException("Solo el cliente del pedido puede ver su seguimiento (RN-016).");
        }
        Integer idDireccion = (Integer) pedido.get("id_direccion_entrega");
        if (coordenadaValida(origenLat, origenLon)) {
            return calcularDesdeCoordenada(origenLat, origenLon, idDireccion);
        }
        String estado = (String) pedido.get("estado_actual");
        if (!"asignado".equals(estado) && !"en_camino".equals(estado)) {
            throw new BusinessRuleException("ESTADO_INVALIDO", "La ruta se muestra cuando el pedido está asignado o en camino.");
        }
        return calcular((Integer) pedido.get("id_repartidor"), idDireccion);
    }

    /** A* desde la ubicación guardada del repartidor (o base del negocio) hasta la dirección de entrega. */
    private RouteResponse calcular(int idRepartidor, Integer idDireccion) {
        Map<String, Object> origen = jdbc.queryForMap("""
                SELECT ST_Y(COALESCE(r.ubicacion_actual, n.ubicacion_base)::geometry) AS lat,
                       ST_X(COALESCE(r.ubicacion_actual, n.ubicacion_base)::geometry) AS lon
                FROM repartidores r
                LEFT JOIN negocios n ON n.id_negocio = r.id_negocio
                WHERE r.id_usuario = :rep""",
                new MapSqlParameterSource("rep", idRepartidor));
        if (origen.get("lat") == null) {
            throw new BusinessRuleException("SIN_UBICACION",
                    "El repartidor no tiene ubicación reportada; activa el GPS.");
        }
        return calcularDesdeCoordenada(
                ((Number) origen.get("lat")).doubleValue(),
                ((Number) origen.get("lon")).doubleValue(),
                idDireccion);
    }

    private RouteResponse calcularDesdeCoordenada(double origenLat, double origenLon, Integer idDireccion) {
        Map<String, Object> destino = jdbc.queryForMap("""
                SELECT ST_Y(ubicacion::geometry) AS lat, ST_X(ubicacion::geometry) AS lon
                FROM direcciones_clientes WHERE id_direccion = :dir""",
                new MapSqlParameterSource("dir", idDireccion));

        RouteRequest req = new RouteRequest();
        req.setOrigenLat(origenLat);
        req.setOrigenLon(origenLon);
        req.setDestinoLat(((Number) destino.get("lat")).doubleValue());
        req.setDestinoLon(((Number) destino.get("lon")).doubleValue());

        return calculoRutaService.calcularRuta(osmGraphLoader.getGrafo(), req);
    }

    private static boolean coordenadaValida(Double lat, Double lon) {
        return lat != null && lon != null
                && Double.isFinite(lat) && Double.isFinite(lon)
                && lat >= -90.0 && lat <= 90.0
                && lon >= -180.0 && lon <= 180.0
                && (lat != 0.0 || lon != 0.0);
    }
}
