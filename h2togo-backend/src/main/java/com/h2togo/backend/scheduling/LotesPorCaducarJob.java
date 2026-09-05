package com.h2togo.backend.scheduling;

import com.h2togo.backend.notificaciones.PushService;
import java.time.Clock;
import java.time.LocalDate;
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
 * Mantenimiento diario de lotes: da de baja los lotes vencidos con un movimiento
 * {@code salida_manual} por la merma (RN-013/RN-030) y avisa al dueño de los lotes a ≤7 días.
 */
@Component
public class LotesPorCaducarJob {

    private static final Logger log = LoggerFactory.getLogger(LotesPorCaducarJob.class);

    private final NamedParameterJdbcTemplate jdbc;
    private final PushService pushService;
    private final Clock clock;

    public LotesPorCaducarJob(NamedParameterJdbcTemplate jdbc, PushService pushService, Clock clock) {
        this.jdbc = jdbc;
        this.pushService = pushService;
        this.clock = clock;
    }

    @Scheduled(cron = "${h2togo.scheduling.lotes-cron:0 30 3 * * *}")
    @Transactional
    public void procesar() {
        LocalDate hoy = LocalDate.now(clock);

        // Vencidos: merma + baja lógica.
        List<Map<String, Object>> vencidos = jdbc.queryForList("""
                SELECT id_lote, cantidad_actual FROM lotes_inventario
                WHERE activo AND fecha_caducidad < :hoy
                FOR UPDATE""", new MapSqlParameterSource("hoy", hoy));
        for (Map<String, Object> l : vencidos) {
            Integer idLote = (Integer) l.get("id_lote");
            int qty = (Integer) l.get("cantidad_actual");
            if (qty > 0) {
                jdbc.update("""
                        INSERT INTO movimientos_inventario (id_lote_base, tipo, cantidad, notas)
                        VALUES (:lote, 'salida_manual'::tipo_movimiento, :qty, 'Merma por caducidad')""",
                        new MapSqlParameterSource().addValue("lote", idLote).addValue("qty", qty));
            }
            jdbc.update("UPDATE lotes_inventario SET activo = FALSE, cantidad_actual = 0 WHERE id_lote = :lote",
                    new MapSqlParameterSource("lote", idLote));
        }
        if (!vencidos.isEmpty()) {
            log.info("Lotes vencidos dados de baja: {}", vencidos.size());
        }

        // Por caducar (≤7 días): avisar al dueño.
        List<Map<String, Object>> porCaducar = jdbc.queryForList("""
                SELECT DISTINCT n.id_dueno FROM lotes_inventario l JOIN negocios n ON n.id_negocio = l.id_negocio
                WHERE l.activo AND l.fecha_caducidad BETWEEN :hoy AND :hoy7""",
                new MapSqlParameterSource().addValue("hoy", hoy).addValue("hoy7", hoy.plusDays(7)));
        for (Map<String, Object> row : porCaducar) {
            pushService.notificar((Integer) row.get("id_dueno"), "Lotes por caducar",
                    "Tienes uno o más lotes que caducan en 7 días o menos.");
        }
    }
}
