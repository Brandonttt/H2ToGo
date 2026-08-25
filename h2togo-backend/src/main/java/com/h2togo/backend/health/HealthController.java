package com.h2togo.backend.health;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Healthcheck público (§F0). Responde {@code {status, db}} tras verificar la
 * conectividad con la BD v6 con un {@code SELECT 1}.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public HealthResponse health() {
        String db;
        try {
            Integer uno = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            db = (uno != null && uno == 1) ? "ok" : "down";
        } catch (Exception ex) {
            db = "down";
        }
        return new HealthResponse("UP", db);
    }

    public record HealthResponse(String status, String db) {
    }
}
