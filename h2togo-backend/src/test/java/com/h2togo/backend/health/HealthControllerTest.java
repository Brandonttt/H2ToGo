package com.h2togo.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.h2togo.backend.health.HealthController.HealthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Prueba unitaria del healthcheck: el controlador reporta {@code db=ok} cuando la
 * consulta responde y {@code db=down} cuando falla, sin tocar una BD real (JdbcTemplate
 * simulado), de modo que {@code mvn verify} no requiere Docker. El cableado HTTP +
 * seguridad (endpoint público) se verifica en runtime con {@code spring-boot:run}.
 */
class HealthControllerTest {

    @Test
    void reportaDbOkCuandoLaConsultaResponde() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(eq("SELECT 1"), eq(Integer.class))).thenReturn(1);

        HealthResponse resp = new HealthController(jdbc).health();

        assertThat(resp.status()).isEqualTo("UP");
        assertThat(resp.db()).isEqualTo("ok");
    }

    @Test
    void reportaDbDownCuandoLaConsultaFalla() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(eq("SELECT 1"), eq(Integer.class)))
                .thenThrow(new RuntimeException("sin conexión"));

        HealthResponse resp = new HealthController(jdbc).health();

        assertThat(resp.db()).isEqualTo("down");
    }
}
