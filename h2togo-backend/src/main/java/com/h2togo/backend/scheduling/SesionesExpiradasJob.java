package com.h2togo.backend.scheduling;

import java.time.Clock;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Limpia diariamente los tokens de sesión vencidos (RNF-004). */
@Component
public class SesionesExpiradasJob {

    private static final Logger log = LoggerFactory.getLogger(SesionesExpiradasJob.class);

    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public SesionesExpiradasJob(NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Scheduled(cron = "${h2togo.scheduling.sesiones-cron:0 0 3 * * *}")
    @Transactional
    public void limpiar() {
        int n = jdbc.update("""
                UPDATE usuarios SET token_sesion = NULL, token_fcm = NULL,
                    sesion_fecha_creacion = NULL, sesion_fecha_expiracion = NULL
                WHERE sesion_fecha_expiracion IS NOT NULL AND sesion_fecha_expiracion < :ahora""",
                new MapSqlParameterSource("ahora", OffsetDateTime.now(clock)));
        if (n > 0) {
            log.info("Sesiones expiradas limpiadas: {}", n);
        }
    }
}
