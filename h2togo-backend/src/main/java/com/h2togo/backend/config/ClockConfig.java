package com.h2togo.backend.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reloj del sistema inyectable (permite pruebas deterministas de las tareas programadas). */
@Configuration
public class ClockConfig {

    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }
}
