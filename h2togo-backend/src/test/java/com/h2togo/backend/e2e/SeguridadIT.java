package com.h2togo.backend.e2e;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import com.h2togo.backend.usuarios.UsuarioRepository;

/**
 * F12: endurecimiento de seguridad. Verifica que los endpoints protegidos exigen token (401) y
 * el rol correcto (403), que un token inválido se rechaza, y que los errores no filtran detalles
 * internos (RN-016): el cuerpo trae {@code codigo}/{@code mensaje} y nunca trazas ni clases Java.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class SeguridadIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"))
            .withCopyFileToContainer(MountableFile.forHostPath("../H2ToGo_v6_postgresql.sql"),
                    "/docker-entrypoint-initdb.d/01_schema.sql")
            .withCopyFileToContainer(MountableFile.forHostPath("sql/zona_benito_juarez.sql"),
                    "/docker-entrypoint-initdb.d/02_zona.sql");

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarioRepository;

    private String registrarYLoguear(String correo, String tel, String rol) throws Exception {
        String negocio = "repartidor".equals(rol) ? ",\"negocio\":{\"nombreComercial\":\"Purif Sec\"}" : "";
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"N","apellidos":"A","correo":"%s","password":"password123",
                         "telefono":"%s","rol":"%s"%s}""".formatted(correo, tel, rol, negocio)))
                .andExpect(status().isCreated());
        String otp = usuarioRepository.findByCorreo(correo).orElseThrow().getCodigoVerificacion();
        mvc.perform(post("/api/v1/auth/verificacion-telefono").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"%s\",\"codigo\":\"%s\"}".formatted(correo, otp)))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"%s\",\"password\":\"password123\"}".formatted(correo)))
                .andExpect(status().isOk());
        return usuarioRepository.findByCorreo(correo).orElseThrow().getTokenSesion();
    }

    @Test
    void endpointsProtegidosExigenToken() throws Exception {
        mvc.perform(get("/api/v1/clientes/me/pedidos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/usuarios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/repartidores/me/pedidos-disponibles")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoSeRechaza() throws Exception {
        mvc.perform(get("/api/v1/clientes/me/pedidos").header("Authorization", "Bearer no-existe-este-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rolEquivocadoRecibe403() throws Exception {
        String tokenCliente = registrarYLoguear("sec.cli@test.mx", "5552000001", "cliente");
        String tokenRepartidor = registrarYLoguear("sec.rep@test.mx", "5552000002", "repartidor");

        // Cliente no puede tocar el panel de administración.
        mvc.perform(get("/api/v1/admin/usuarios").header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());
        // Repartidor no puede usar endpoints exclusivos de cliente.
        mvc.perform(get("/api/v1/clientes/me/pedidos").header("Authorization", "Bearer " + tokenRepartidor))
                .andExpect(status().isForbidden());
    }

    @Test
    void erroresNoFiltranDetallesInternos() throws Exception {
        String tokenCliente = registrarYLoguear("sec.leak@test.mx", "5552000003", "cliente");
        // 404 controlado: cuerpo con codigo/mensaje, sin trazas ni nombres de clase Java.
        mvc.perform(get("/api/v1/pedidos/999999").header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").exists())
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.mensaje").value(Matchers.not(Matchers.containsString("Exception"))))
                .andExpect(jsonPath("$.mensaje").value(Matchers.not(Matchers.containsString("com.h2togo"))));
    }
}
