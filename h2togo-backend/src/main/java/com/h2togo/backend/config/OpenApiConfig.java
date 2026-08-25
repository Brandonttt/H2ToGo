package com.h2togo.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos de la documentación OpenAPI (springdoc). Swagger UI en /swagger-ui.html.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI h2togoOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("H2ToGo API")
                .description("API REST del sistema H2ToGo — Trabajo Terminal 2026-B176")
                .version("v1"));
    }
}
