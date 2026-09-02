package com.h2togo.backend.direcciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.h2togo.backend.usuarios.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
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

/**
 * F3: perfil y direcciones del cliente. Verifica clasificación de cobertura (RN-001) con
 * el polígono de Benito Juárez, propiedad de las direcciones (RN-016), CRUD y perfil propio.
 * Carga el esquema v6 y el polígono de zona en el contenedor. Se salta sin Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class DireccionesFlowIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"))
            .withCopyFileToContainer(
                    MountableFile.forHostPath("../H2ToGo_v6_postgresql.sql"),
                    "/docker-entrypoint-initdb.d/01_schema.sql")
            .withCopyFileToContainer(
                    MountableFile.forHostPath("sql/zona_benito_juarez.sql"),
                    "/docker-entrypoint-initdb.d/02_zona.sql");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UsuarioRepository usuarioRepository;

    // Dentro de Benito Juárez (dentro del bbox) y claramente fuera.
    private static final String DENTRO = """
            {"alias":"Casa","calle":"Av. Insurgentes Sur","numeroExterior":"1234","colonia":"Del Valle",
             "codigoPostal":"03100","referencias":"Portón azul","lat":19.372,"lon":-99.178}""";
    private static final String FUERA = """
            {"alias":"Trabajo","calle":"Otra","numeroExterior":"9","colonia":"Lejos",
             "codigoPostal":"00000","referencias":"Fuera de la alcaldía","lat":19.500,"lon":-99.000}""";

    private String registrarClienteYLoguear(String correo, String telefono) throws Exception {
        String reg = """
                {"nombre":"Cli","apellidos":"Ente","correo":"%s","password":"password123",
                 "telefono":"%s","rol":"cliente"}""".formatted(correo, telefono);
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(reg))
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
    void clasificaCoberturaDentroYFuera() throws Exception {
        String token = registrarClienteYLoguear("dir1@test.mx", "5552220001");

        mvc.perform(post("/api/v1/clientes/me/direcciones").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(DENTRO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enZonaCobertura").value(true))
                .andExpect(jsonPath("$.lat").value(19.372));

        mvc.perform(post("/api/v1/clientes/me/direcciones").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(FUERA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enZonaCobertura").value(false));

        mvc.perform(get("/api/v1/clientes/me/direcciones").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void actualizaYEliminaDireccionPropia() throws Exception {
        String token = registrarClienteYLoguear("dir2@test.mx", "5552220002");
        String cuerpo = mvc.perform(post("/api/v1/clientes/me/direcciones")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(DENTRO))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(cuerpo, "$.id");

        String editado = DENTRO.replace("\"Casa\"", "\"Casa nueva\"");
        mvc.perform(put("/api/v1/clientes/me/direcciones/" + id).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(editado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alias").value("Casa nueva"));

        // No usada en pedidos → borrado real.
        mvc.perform(delete("/api/v1/clientes/me/direcciones/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/clientes/me/direcciones").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void noVeNiEditaDireccionesDeOtroCliente() throws Exception {
        String tokenA = registrarClienteYLoguear("dueno@test.mx", "5552220003");
        String cuerpo = mvc.perform(post("/api/v1/clientes/me/direcciones")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(DENTRO))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int idAjena = JsonPath.read(cuerpo, "$.id");

        String tokenB = registrarClienteYLoguear("intruso@test.mx", "5552220004");
        // No ve las de A.
        mvc.perform(get("/api/v1/clientes/me/direcciones").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        // No puede editar ni borrar la ajena → 404 (no filtra existencia).
        mvc.perform(put("/api/v1/clientes/me/direcciones/" + idAjena).header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON).content(DENTRO))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/clientes/me/direcciones/" + idAjena).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void perfilMeDevuelveBloqueDeCliente() throws Exception {
        String token = registrarClienteYLoguear("perfil@test.mx", "5552220005");

        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("cliente"))
                .andExpect(jsonPath("$.correo").value("perfil@test.mx"))
                .andExpect(jsonPath("$.cliente.ausenciasConsecutivas").value(0));

        mvc.perform(patch("/api/v1/usuarios/me").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Nuevo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nuevo"));
    }

    @Test
    void repartidorNoAccedeARutasDeCliente() throws Exception {
        // Alta de repartidor con negocio nuevo, verificado y logueado.
        String reg = """
                {"nombre":"Rep","apellidos":"Artidor","correo":"rep@test.mx","password":"password123",
                 "telefono":"5552220006","rol":"repartidor","negocio":{"nombreComercial":"Purificadora X"}}""";
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(reg))
                .andExpect(status().isCreated());
        String otp = usuarioRepository.findByCorreo("rep@test.mx").orElseThrow().getCodigoVerificacion();
        mvc.perform(post("/api/v1/auth/verificacion-telefono").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"rep@test.mx\",\"codigo\":\"%s\"}".formatted(otp)))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"rep@test.mx\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());
        String token = usuarioRepository.findByCorreo("rep@test.mx").orElseThrow().getTokenSesion();

        // Un repartidor no puede usar las rutas /clientes/** → 403 (RNF-008).
        mvc.perform(get("/api/v1/clientes/me/direcciones").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void perfilRepartidorMarcaEsDueno() throws Exception {
        String reg = """
                {"nombre":"Due","apellidos":"Ño","correo":"dueno2@test.mx","password":"password123",
                 "telefono":"5552220007","rol":"repartidor","negocio":{"nombreComercial":"Purificadora Y"}}""";
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(reg))
                .andExpect(status().isCreated());
        String otp = usuarioRepository.findByCorreo("dueno2@test.mx").orElseThrow().getCodigoVerificacion();
        mvc.perform(post("/api/v1/auth/verificacion-telefono").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"dueno2@test.mx\",\"codigo\":\"%s\"}".formatted(otp)))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"dueno2@test.mx\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());
        String token = usuarioRepository.findByCorreo("dueno2@test.mx").orElseThrow().getTokenSesion();

        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("repartidor"))
                .andExpect(jsonPath("$.repartidor.esDueno").value(true));
    }
}
