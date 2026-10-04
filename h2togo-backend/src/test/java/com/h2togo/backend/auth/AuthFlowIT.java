package com.h2togo.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.h2togo.backend.usuarios.Usuario;
import com.h2togo.backend.usuarios.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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
    private NamedParameterJdbcTemplate jdbc;

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
    @Test
    void registroDeDuenoGuardaDireccionYHorarioSinDatosFicticios() throws Exception {
        String cuerpo = mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Dora","apellidos":"Dueña","correo":"dora@test.mx","password":"password123",
                                 "telefono":"5559870001","rol":"repartidor",
                                 "negocio":{"nombreComercial":"Agua Dora","calle":"Pilares","colonia":"Del Valle",
                                            "codigoPostal":"03100","lat":19.372,"lon":-99.178,
                                            "horarioApertura":"8:00","horarioCierre":"19:30"}}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int idNegocio = com.jayway.jsonpath.JsonPath.read(cuerpo, "$.idNegocio");
        var p = new MapSqlParameterSource("id", idNegocio);

        // Sin número exterior la dirección se guarda como "S/N" (el esquema exige dirección completa).
        assertThat(jdbc.queryForObject("SELECT numero_exterior FROM negocios WHERE id_negocio = :id", p, String.class))
                .isEqualTo("S/N");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM horarios_negocio
                WHERE id_negocio = :id AND NOT cerrado AND hora_apertura = '08:00' AND hora_cierre = '19:30'""",
                p, Integer.class)).isEqualTo(6);
        // Productos y vehículos se solicitan después (RN-020): nada sembrado.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM productos_negocio WHERE id_negocio = :id", p, Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vehiculos_negocio WHERE id_negocio = :id", p, Integer.class)).isZero();
    }

    @Test
    void registroConHorarioInvalidoDevuelve422() throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"E","apellidos":"F","correo":"horario@test.mx","password":"password123",
                                 "telefono":"5559870002","rol":"repartidor",
                                 "negocio":{"nombreComercial":"Agua Mala","horarioApertura":"20:00","horarioCierre":"08:00"}}"""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("HORARIO_INVALIDO"));
    }
}
