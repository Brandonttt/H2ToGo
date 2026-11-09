package com.h2togo.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.h2togo.backend.notificaciones.PushService;
import com.h2togo.backend.usuarios.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/** Cambio de teléfono con sesión iniciada (cuenta ya verificada): código al número nuevo y confirmación. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class CambioTelefonoFlowIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"))
            .withCopyFileToContainer(MountableFile.forHostPath("../H2ToGo_v7_postgresql.sql"),
                    "/docker-entrypoint-initdb.d/01_schema.sql")
            .withCopyFileToContainer(MountableFile.forHostPath("sql/zona_benito_juarez.sql"),
                    "/docker-entrypoint-initdb.d/02_zona.sql");

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @MockitoBean private PushService pushService;

    @Test
    void cambiaElTelefonoConElCodigoDemo() throws Exception {
        String token = registrar("ct-demo@test.mx", "5558880010");

        solicitud(token, "5558880011").andExpect(status().isNoContent());
        // Hasta confirmar, el número no cambia.
        assertThat(usuarioRepository.findByCorreo("ct-demo@test.mx").orElseThrow().getTelefono())
                .isEqualTo("5558880010");

        confirmacion(token, "5558880011", "123456")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("5558880011"))
                .andExpect(jsonPath("$.telefonoVerificado").value(true));
        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("5558880011"));
        // El número anterior queda libre para otra cuenta.
        registrar("ct-otro@test.mx", "5558880010");
    }

    @Test
    void cambiaElTelefonoConElCodigoEnviado() throws Exception {
        String token = registrar("ct-sms@test.mx", "5558880020");
        solicitud(token, "5558880021").andExpect(status().isNoContent());
        String guardado = usuarioRepository.findByCorreo("ct-sms@test.mx").orElseThrow().getCodigoVerificacion();
        String codigo = guardado.substring(0, 6); // el resto liga el código al número nuevo

        // El código enviado a un número no sirve para confirmar otro.
        confirmacion(token, "5558880029", codigo.equals("123456") ? "654321" : codigo)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("OTP_INVALIDO"));

        confirmacion(token, "5558880021", codigo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("5558880021"));
        assertThat(usuarioRepository.findByCorreo("ct-sms@test.mx").orElseThrow().getCodigoVerificacion()).isNull();
    }

    @Test
    void rechazaUnNumeroDeOtraCuenta() throws Exception {
        registrar("ct-a@test.mx", "5558880030");
        String token = registrar("ct-b@test.mx", "5558880031");

        solicitud(token, "5558880030")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El teléfono ya está registrado."));
        confirmacion(token, "5558880030", "123456").andExpect(status().isConflict());
        assertThat(usuarioRepository.findByCorreo("ct-b@test.mx").orElseThrow().getTelefono())
                .isEqualTo("5558880031");

        solicitud(token, "55588").andExpect(status().isBadRequest());
    }

    @Test
    void rechazaUnCodigoEquivocado() throws Exception {
        String token = registrar("ct-mal@test.mx", "5558880040");
        solicitud(token, "5558880041").andExpect(status().isNoContent());
        String guardado = usuarioRepository.findByCorreo("ct-mal@test.mx").orElseThrow().getCodigoVerificacion();
        String equivocado = guardado.startsWith("700001") ? "700002" : "700001";

        confirmacion(token, "5558880041", equivocado)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("OTP_INVALIDO"))
                .andExpect(jsonPath("$.mensaje").value("El código es inválido o expiró."));
        var u = usuarioRepository.findByCorreo("ct-mal@test.mx").orElseThrow();
        assertThat(u.getTelefono()).isEqualTo("5558880040");
        assertThat(u.getCodigoIntentos()).isEqualTo((short) 1);
    }

    // ---------------------------------------------------------------- Apoyo

    private ResultActions solicitud(String token, String telefono) throws Exception {
        return mvc.perform(post("/api/v1/usuarios/me/telefono/solicitud").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"telefono\":\"%s\"}".formatted(telefono)));
    }

    private ResultActions confirmacion(String token, String telefono, String codigo) throws Exception {
        return mvc.perform(post("/api/v1/usuarios/me/telefono/confirmacion").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"telefono\":\"%s\",\"codigo\":\"%s\"}".formatted(telefono, codigo)));
    }

    /** Registra un cliente, verifica el teléfono e inicia sesión; devuelve el token. */
    private String registrar(String correo, String telefono) throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Ana","apellidos":"Prueba","correo":"%s","password":"password123",
                         "telefono":"%s","rol":"cliente"}""".formatted(correo, telefono)))
                .andExpect(status().isCreated());
        String otp = usuarioRepository.findByCorreo(correo).orElseThrow().getCodigoVerificacion();
        mvc.perform(post("/api/v1/auth/verificacion-telefono").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"%s\",\"codigo\":\"%s\"}".formatted(correo, otp)))
                .andExpect(status().isNoContent());
        String cuerpo = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"%s\",\"password\":\"password123\",\"forzar\":true}".formatted(correo)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(cuerpo, "$.token");
    }
}
