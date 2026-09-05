package com.h2togo.backend.scheduling;

import com.h2togo.backend.notificaciones.PushService;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Activa los pedidos programados cuya fecha llegó (RF-025/RN-027): los pasa a {@code pendiente}
 * y marca {@code notificado_programado} para evitar doble proceso. Usa {@code FOR UPDATE SKIP
 * LOCKED} (Q7) para que varias instancias no se pisen, y notifica a los repartidores del negocio.
 */
@Component
public class PedidosProgramadosJob {

    private static final Logger log = LoggerFactory.getLogger(PedidosProgramadosJob.class);

    private final NamedParameterJdbcTemplate jdbc;
    private final PushService pushService;
    private final Clock clock;

    public PedidosProgramadosJob(NamedParameterJdbcTemplate jdbc, PushService pushService, Clock clock) {
        this.jdbc = jdbc;
        this.pushService = pushService;
        this.clock = clock;
    }

    @Scheduled(fixedRateString = "${h2togo.scheduling.pedidos-ms:60000}")
    @Transactional
    public void activar() {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        List<Map<String, Object>> listos = jdbc.queryForList("""
                SELECT id_pedido, id_negocio_solicitado FROM pedidos
                WHERE estado_actual = 'pendiente_programado' AND NOT notificado_programado
                  AND fecha_programada <= :ahora
                FOR UPDATE SKIP LOCKED""", new MapSqlParameterSource("ahora", ahora));

        for (Map<String, Object> p : listos) {
            Integer idPedido = (Integer) p.get("id_pedido");
            jdbc.update("""
                    UPDATE pedidos SET estado_actual = 'pendiente'::estado_pedido, notificado_programado = TRUE
                    WHERE id_pedido = :id""", new MapSqlParameterSource("id", idPedido));
            jdbc.update("INSERT INTO historial_estados_pedido (id_pedido, estado) VALUES (:id, 'pendiente'::estado_pedido)",
                    new MapSqlParameterSource("id", idPedido));
            notificarRepartidores((Integer) p.get("id_negocio_solicitado"));
        }
        if (!listos.isEmpty()) {
            log.info("Pedidos programados activados: {}", listos.size());
        }
    }

    private void notificarRepartidores(Integer idNegocio) {
        if (idNegocio == null) {
            return; // modalidad abierta: el filtrado por precio lo hace la lista de disponibles
        }
        List<Integer> reps = jdbc.queryForList(
                "SELECT id_usuario FROM repartidores WHERE id_negocio = :neg",
                new MapSqlParameterSource("neg", idNegocio), Integer.class);
        for (Integer rep : reps) {
            pushService.notificar(rep, "Nuevo pedido disponible",
                    "Un pedido programado quedó disponible para asignarse.");
        }
    }
}
