package com.h2togo.backend.catalogo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Inicializa el catálogo global de marcas con las marcas comerciales y genéricas
 * más populares si aún no existen en la base de datos.
 * Es 100% idempotente (ON CONFLICT DO NOTHING) y garantiza que cualquier purificadora
 * cuente con opciones disponibles para agregar productos a su catálogo.
 */
@Component
public class CatalogoWarmup {

    private static final Logger log = LoggerFactory.getLogger(CatalogoWarmup.class);

    private static final List<String> MARCAS_PREDETERMINADAS = List.of(
            "Ciel 20L",
            "Bonafont 20L",
            "e-pura 20L",
            "Santorini 20L",
            "Santa María 20L",
            "Pureza Aga 20L",
            "Agua Purificada Genérica 20L",
            "Agua Alcalina 20L",
            "Garrafón Cristal 20L",
            "Agua Demo 20L"
    );

    private final NamedParameterJdbcTemplate jdbc;

    public CatalogoWarmup(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void precargarMarcas() {
        try {
            int insertadas = 0;
            for (String nombre : MARCAS_PREDETERMINADAS) {
                int filas = jdbc.update("""
                        INSERT INTO marcas (nombre, activo)
                        VALUES (:nombre, TRUE)
                        ON CONFLICT (nombre) DO NOTHING""",
                        new MapSqlParameterSource("nombre", nombre));
                insertadas += filas;
            }
            if (insertadas > 0) {
                log.info("Catálogo global: {} marcas predeterminadas añadidas exitosamente.", insertadas);
            }
        } catch (Exception e) {
            log.warn("No se pudieron precargar las marcas predeterminadas: {}", e.getMessage());
        }
    }
}
