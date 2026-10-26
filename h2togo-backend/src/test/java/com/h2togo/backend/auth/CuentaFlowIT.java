package com.h2togo.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.h2togo.backend.notificaciones.PushService;
import com.h2togo.backend.usuarios.RepartidorRepository;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Cuenta: un repartidor que se une a una purificadora existente espera la aprobación del dueño;
 * cambio de contraseña con sesión; recuperación de contraseña con código por SMS.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class CuentaFlowIT {

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
    @Autowired private RepartidorRepository repartidorRepository;
    @MockitoBean private PushService pushService;

    @Test
    void unirseAUnaPurificadoraRequiereLaAprobacionDelDueno() throws Exception {
        String dueno = registrar("cu-due@test.mx", "5559990010", """
                "rol":"repartidor","negocio":{"nombreComercial":"Purif Cuenta"}""");
        int idDueno = usuarioRepository.findByCorreo("cu-due@test.mx").orElseThrow().getId();
        int idNegocio = repartidorRepository.findById(idDueno).orElseThrow().getIdNegocio();

        String miembro = registrar("cu-mie@test.mx", "5559990011", """
                "rol":"repartidor","negocio":{"idExistente":%d}""".formatted(idNegocio));
        int idMiembro = usuarioRepository.findByCorreo("cu-mie@test.mx").orElseThrow().getId();
        // Al verificar su teléfono se avisa al dueño.
        verify(pushService).notificar(eq(idDueno), eq("Nueva solicitud de repartidor"), anyString(), anyMap());

        // Pendiente: no ve pedidos ni puede iniciar jornada.
        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + miembro))
                .andExpect(status().isOk()).andExpect(jsonPath("$.repartidor.aprobado").value(false));
        mvc.perform(get("/api/v1/repartidores/me/pedidos-disponibles").header("Authorization", "Bearer " + miembro))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("PENDIENTE_APROBACION"));

        // Solo el dueño gestiona: el miembro no puede ver la lista ni aprobarse.
        mvc.perform(get("/api/v1/negocios/me/repartidores").header("Authorization", "Bearer " + miembro))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/negocios/me/repartidores").header("Authorization", "Bearer " + dueno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idUsuario").value(idMiembro))
                .andExpect(jsonPath("$[0].aprobado").value(false));

        mvc.perform(post("/api/v1/negocios/me/repartidores/" + idMiembro + "/aprobacion")
                        .header("Authorization", "Bearer " + dueno))
                .andExpect(status().isNoContent());
        verify(pushService).notificar(eq(idMiembro), eq("Solicitud aprobada"), anyString(), anyMap());
        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + miembro))
                .andExpect(status().isOk()).andExpect(jsonPath("$.repartidor.aprobado").value(true));
        mvc.perform(get("/api/v1/repartidores/me/pedidos-disponibles").header("Authorization", "Bearer " + miembro))
                .andExpect(status().isOk());

        // Una segunda solicitud rechazada: la cuenta queda dada de baja.
        registrar("cu-rech@test.mx", "5559990012", """
                "rol":"repartidor","negocio":{"idExistente":%d}""".formatted(idNegocio));
        int idRechazado = usuarioRepository.findByCorreo("cu-rech@test.mx").orElseThrow().getId();
        mvc.perform(delete("/api/v1/negocios/me/repartidores/" + idRechazado).header("Authorization", "Bearer " + dueno))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"cu-rech@test.mx\",\"password\":\"password123\",\"forzar\":true}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("CUENTA_INACTIVA"));
    }

    @Test
    void cambiarContrasenaConSesion() throws Exception {
        String token = registrar("cu-cli@test.mx", "5559990020", "\"rol\":\"cliente\"");

        mvc.perform(put("/api/v1/usuarios/me/contrasena").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"actual\":\"equivocada\",\"nueva\":\"nuevaClave1\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("CONTRASENA_ACTUAL_INCORRECTA"));
        mvc.perform(put("/api/v1/usuarios/me/contrasena").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"actual\":\"password123\",\"nueva\":\"nuevaClave1\"}"))
                .andExpect(status().isNoContent());

        login("cu-cli@test.mx", "password123").andExpect(status().isUnauthorized());
        login("cu-cli@test.mx", "nuevaClave1").andExpect(status().isOk());
    }

    @Test
    void recuperarContrasenaConCodigoPorSms() throws Exception {
        registrar("cu-olv@test.mx", "5559990030", "\"rol\":\"cliente\"");

        // Un correo que no existe responde igual (no se revela si la cuenta existe).
        mvc.perform(post("/api/v1/auth/recuperacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"nadie@test.mx\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/recuperacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"cu-olv@test.mx\"}"))
                .andExpect(status().isNoContent());
        String codigo = usuarioRepository.findByCorreo("cu-olv@test.mx").orElseThrow().getCodigoVerificacion();
        assertThat(codigo).isNotBlank();

        mvc.perform(post("/api/v1/auth/recuperacion/confirmacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"cu-olv@test.mx\",\"codigo\":\"000001\",\"nuevaContrasena\":\"otraClave22\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("OTP_INVALIDO"));
        mvc.perform(post("/api/v1/auth/recuperacion/confirmacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"cu-olv@test.mx\",\"codigo\":\"%s\",\"nuevaContrasena\":\"otraClave22\"}".formatted(codigo)))
                .andExpect(status().isNoContent());

        login("cu-olv@test.mx", "password123").andExpect(status().isUnauthorized());
        login("cu-olv@test.mx", "otraClave22").andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- Apoyo

    /** Registra, verifica el teléfono e inicia sesión; devuelve el token. */
    private String registrar(String correo, String telefono, String rolYNegocio) throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Ana","apellidos":"Prueba","correo":"%s","password":"password123",
                         "telefono":"%s",%s}""".formatted(correo, telefono, rolYNegocio)))
                .andExpect(status().isCreated());
        String otp = usuarioRepository.findByCorreo(correo).orElseThrow().getCodigoVerificacion();
        mvc.perform(post("/api/v1/auth/verificacion-telefono").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"%s\",\"codigo\":\"%s\"}".formatted(correo, otp)))
                .andExpect(status().isNoContent());
        login(correo, "password123").andExpect(status().isOk());
        return usuarioRepository.findByCorreo(correo).orElseThrow().getTokenSesion();
    }

    private org.springframework.test.web.servlet.ResultActions login(String correo, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"%s\",\"password\":\"%s\",\"forzar\":true}".formatted(correo, password)));
    }
}
