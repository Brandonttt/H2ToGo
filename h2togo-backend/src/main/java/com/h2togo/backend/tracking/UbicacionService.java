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
 * repartidor) y dispara el aviso de proximidad (RF-005) para sus pedidos en camino.
 */
@Service
public class UbicacionService {

    private final NamedParameterJdbcTemplate jdbc;
    private final ProximidadService proximidadService;
    private final int proximidadM;
    private final ConcurrentHashMap<Integer, Instant> ultimaEscritura = new ConcurrentHashMap<>();

    public UbicacionService(NamedParameterJdbcTemplate jdbc, ProximidadService proximidadService,
            @Value("${h2togo.pedidos.proximidad-radio-m}") int proximidadM) {
        this.jdbc = jdbc;
        this.proximidadService = proximidadService;
        this.proximidadM = proximidadM;
    }

    @Transactional
    public void reportar(int idRepartidor, double lat, double lon) {
        if (debePersistir(idRepartidor)) {
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

    private boolean debePersistir(int idRepartidor) {
        Instant ahora = Instant.now();
        Instant previa = ultimaEscritura.get(idRepartidor);
        if (previa == null || Duration.between(previa, ahora).getSeconds() >= 5) {
            ultimaEscritura.put(idRepartidor, ahora);
            return true;
        }
        return false;
    }
}
