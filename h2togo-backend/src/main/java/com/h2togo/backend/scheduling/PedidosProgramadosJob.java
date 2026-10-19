package com.h2togo.backend.scheduling;

import com.h2togo.backend.notificaciones.PushService;
import com.h2togo.backend.pedidos.AvisoNuevoPedido;
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
 * Si un repartidor lo reservó en su agenda, sigue siendo suyo: solo se le avisa a él.
 */
@Component
public class PedidosProgramadosJob {

    private static final Logger log = LoggerFactory.getLogger(PedidosProgramadosJob.class);

    private final NamedParameterJdbcTemplate jdbc;
    private final AvisoNuevoPedido avisoNuevoPedido;
    private final PushService pushService;
    private final Clock clock;

    public PedidosProgramadosJob(NamedParameterJdbcTemplate jdbc, AvisoNuevoPedido avisoNuevoPedido,
            PushService pushService, Clock clock) {
        this.jdbc = jdbc;
        this.avisoNuevoPedido = avisoNuevoPedido;
        this.pushService = pushService;
        this.clock = clock;
    }

    @Scheduled(fixedRateString = "${h2togo.scheduling.pedidos-ms:60000}")
    @Transactional
    public void activar() {
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        List<Map<String, Object>> listos = jdbc.queryForList("""
                SELECT id_pedido, id_negocio_solicitado, id_repartidor FROM pedidos
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
            Integer reservadoPor = (Integer) p.get("id_repartidor");
            if (reservadoPor == null) {
                avisoNuevoPedido.avisar(idPedido);
            } else {
                pushService.notificar(reservadoPor, "Pedido programado activo",
                        "Ya es hora del pedido #" + idPedido + " de tu ruta.",
                        Map.of("tipo", "ruta_recalculada", "idPedido", String.valueOf(idPedido)));
            }
        }
        if (!listos.isEmpty()) {
            log.info("Pedidos programados activados: {}", listos.size());
        }
    }
}
