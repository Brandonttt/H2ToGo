package com.h2togo.backend.tracking;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persiste la ubicación del repartidor (Q5, §7) con throttle (máx. 1 escritura cada 5 s por
 * repartidor salvo desplazamientos ≥ 35 m) y dispara el aviso de proximidad (RF-005) para sus
 * pedidos en camino.
 */
@Service
public class UbicacionService {

    private record UltimaEscritura(Instant instante, double lat, double lon) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final ProximidadService proximidadService;
    private final int proximidadM;
    private final ConcurrentHashMap<Integer, UltimaEscritura> ultimaEscritura = new ConcurrentHashMap<>();

    public UbicacionService(NamedParameterJdbcTemplate jdbc, ProximidadService proximidadService,
            @Value("${h2togo.pedidos.proximidad-radio-m}") int proximidadM) {
        this.jdbc = jdbc;
        this.proximidadService = proximidadService;
        this.proximidadM = proximidadM;
    }

    @Transactional
    public void reportar(int idRepartidor, double lat, double lon) {
        if (debePersistir(idRepartidor, lat, lon)) {
            jdbc.update("""
                    UPDATE repartidores
                    SET ubicacion_actual = ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                        ubicacion_reportada_en = now()
                    WHERE id_usuario = :id""",
                    new MapSqlParameterSource().addValue("lon", lon).addValue("lat", lat)
                            .addValue("id", idRepartidor));
        }
        verificarProximidad(idRepartidor, lat, lon);
    }

    /** Pedidos en camino de este repartidor a ≤ 500 m del domicilio → aviso (una vez). */
    private void verificarProximidad(int idRepartidor, double lat, double lon) {
        List<Map<String, Object>> cercanos = jdbc.queryForList("""
                SELECT p.id_pedido, p.id_cliente
                FROM pedidos p JOIN direcciones_clientes d ON d.id_direccion = p.id_direccion_entrega
                WHERE p.id_repartidor = :rep AND p.estado_actual = 'en_camino'
                  AND ST_DWithin(d.ubicacion, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :radio)""",
                new MapSqlParameterSource().addValue("rep", idRepartidor)
                        .addValue("lon", lon).addValue("lat", lat).addValue("radio", proximidadM));
        for (Map<String, Object> c : cercanos) {
            proximidadService.avisarProximidad((Integer) c.get("id_pedido"), (Integer) c.get("id_cliente"));
        }
    }

    /**
     * Fija la ubicación sin el límite de 5 s: al registrar "Llegué" o el resultado de una entrega,
     * el repartidor está en ese domicilio y de ahí debe partir la ruta a su siguiente parada. Sin
     * esto podía quedar guardado un punto anterior (el GPS deja de reportar al detenerse y el último
     * reporte en movimiento pudo descartarse por el límite).
     */
    @Transactional
    public void fijar(int idRepartidor, double lat, double lon) {
        ultimaEscritura.put(idRepartidor, new UltimaEscritura(Instant.now(), lat, lon));
        jdbc.update("""
                UPDATE repartidores
                SET ubicacion_actual = ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                    ubicacion_reportada_en = now()
                WHERE id_usuario = :id""",
                new MapSqlParameterSource().addValue("lon", lon).addValue("lat", lat)
                        .addValue("id", idRepartidor));
    }

    /**
     * Persiste si pasaron ≥ 5 s o si el repartidor se desplazó ≥ 35 m respecto al último punto
     * guardado (evita que cambios de posición con FakeGPS o saltos a la purificadora se descarten
     * por el throttle y dejen una ruta calculada desde una ubicación anterior).
     */
    private boolean debePersistir(int idRepartidor, double lat, double lon) {
        Instant ahora = Instant.now();
        UltimaEscritura previa = ultimaEscritura.get(idRepartidor);
        if (previa == null
                || Duration.between(previa.instante(), ahora).toMillis() >= 5_000
                || distanciaMetros(previa.lat(), previa.lon(), lat, lon) >= 35.0) {
            ultimaEscritura.put(idRepartidor, new UltimaEscritura(ahora, lat, lon));
            return true;
        }
        return false;
    }

    private static double distanciaMetros(double lat1, double lon1, double lat2, double lon2) {
        double rad = Math.PI / 180.0;
        double dLat = (lat2 - lat1) * rad;
        double dLon = (lon2 - lon1) * rad;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1 * rad) * Math.cos(lat2 * rad) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * 6_371_000.0 * Math.asin(Math.sqrt(Math.min(1.0, a)));
    }
}
