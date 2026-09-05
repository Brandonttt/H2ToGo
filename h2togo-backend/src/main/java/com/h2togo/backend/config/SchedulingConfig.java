package com.h2togo.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activa las tareas programadas (F11). Se desactiva con {@code h2togo.scheduling.enabled=false}
 * (perfil de pruebas), donde los jobs existen como beans pero se invocan manualmente para
 * pruebas deterministas.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "h2togo.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
