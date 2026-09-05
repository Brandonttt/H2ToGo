package com.h2togo.backend.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * F12: exporta la especificación OpenAPI generada por SpringDoc a {@code docs/openapi.json}
 * (evidencia para el TT) y verifica que expone las rutas y esquemas de seguridad clave.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class OpenApiExportIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"))
            .withCopyFileToContainer(MountableFile.forHostPath("../H2ToGo_v6_postgresql.sql"),
                    "/docker-entrypoint-initdb.d/01_schema.sql")
            .withCopyFileToContainer(MountableFile.forHostPath("sql/zona_benito_juarez.sql"),
                    "/docker-entrypoint-initdb.d/02_zona.sql");

    @Autowired private MockMvc mvc;

    @Test
    void exportaOpenApiYExponeRutasClave() throws Exception {
        String spec = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Rutas representativas de cada actor deben estar documentadas.
        assertThat(spec)
                .contains("\"openapi\"")
                .contains("/api/v1/auth/login")
                .contains("/api/v1/pedidos")
                .contains("/api/v1/repartidores/me/jornada")
                .contains("/api/v1/admin/routing/route");

        // Se escribe a docs/openapi.json en la raíz del repo como evidencia versionable.
        Path destino = Path.of("..", "docs", "openapi.json").toAbsolutePath().normalize();
        Files.createDirectories(destino.getParent());
        Files.writeString(destino, spec);
        assertThat(Files.size(destino)).isGreaterThan(1000L);
    }
}
