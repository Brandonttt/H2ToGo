package com.h2togo.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.h2togo.backend.usuarios.Usuario;
import com.h2togo.backend.usuarios.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Flujo de autenticación de F2 contra la BD v6 real (CU-001/002/003): registro → OTP →
 * login → endpoint protegido → logout, más sesión única (RN-018) y control por rol (RNF-008).
 * Se salta sin Docker; corre bajo failsafe en {@code verify}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AuthFlowIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"))
            .withCopyFileToContainer(
                    MountableFile.forHostPath("../H2ToGo_v6_postgresql.sql"),
                    "/docker-entrypoint-initdb.d/01_schema.sql");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UsuarioRepository usuarioRepository;

    private static String registro(String correo, String telefono) {
        return """
                {"nombre":"Ana","apellidos":"García","correo":"%s","password":"password123",
                 "telefono":"%s","rol":"cliente"}
                """.formatted(correo, telefono);
    }

    private static String login(String correo, boolean forzar) {
        return """
                {"correo":"%s","password":"password123","forzar":%s}
                """.formatted(correo, forzar);
    }

    /** Da de alta, lee el OTP de la BD, lo verifica y hace login; devuelve el token de sesión. */
    private String registrarVerificarYLoguear(String correo, String telefono) throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(registro(correo, telefono))).andExpect(status().isCreated());

        Usuario u = usuarioRepository.findByCorreo(correo).orElseThrow();
        String otp = u.getCodigoVerificacion();

        mvc.perform(post("/api/v1/auth/verificacion-telefono").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"%s\",\"codigo\":\"%s\"}".formatted(correo, otp)))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(login(correo, false))).andExpect(status().isOk());

        return usuarioRepository.findByCorreo(correo).orElseThrow().getTokenSesion();
    }

    @Test
    void flujoCompletoRegistroVerificacionLoginLogout() throws Exception {
        String token = registrarVerificarYLoguear("flujo@test.mx", "5551110001");
        assertThat(token).isNotBlank();

        // Token válido pero rol CLIENTE → 403 en /admin (autenticado pero sin permiso).
        mvc.perform(get("/api/v1/admin/pedidos").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        // Logout invalida la sesión.
        mvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        assertThat(usuarioRepository.findByCorreo("flujo@test.mx").orElseThrow().getTokenSesion())
                .isNull();

        // Reusar el token ya invalidado → 401.
        mvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginSinVerificarSeBloquea() throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(registro("sinverif@test.mx", "5551110002"))).andExpect(status().isCreated());

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("sinverif@test.mx", false)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void registroDuplicadoDevuelve409() throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(registro("dup@test.mx", "5551110003"))).andExpect(status().isCreated());

        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(registro("dup@test.mx", "5551110099")))
                .andExpect(status().isConflict());
    }

    @Test
    void sesionUnicaExigeForzar() throws Exception {
        registrarVerificarYLoguear("unica@test.mx", "5551110004");

        // Segundo login sin forzar → 409.
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(login("unica@test.mx", false))).andExpect(status().isConflict());

        // Con forzar → 200 (cierra la anterior).
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(login("unica@test.mx", true))).andExpect(status().isOk());
    }

    @Test
    void accesoSinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/v1/admin/pedidos")).andExpect(status().isUnauthorized());
    }
}
