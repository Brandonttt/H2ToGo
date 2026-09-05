package com.h2togo.backend.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.h2togo.backend.catalogo.Marca;
import com.h2togo.backend.catalogo.MarcaRepository;
import com.h2togo.backend.catalogo.ProductoNegocio;
import com.h2togo.backend.catalogo.ProductoNegocioRepository;
import com.h2togo.backend.negocios.NegocioRepository;
import com.h2togo.backend.usuarios.RepartidorRepository;
import com.h2togo.backend.usuarios.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/** F10: administración (CU-014/015/016/020/021). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AdministracionFlowIT {

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
    @Autowired private NegocioRepository negocioRepository;
    @Autowired private MarcaRepository marcaRepository;
    @Autowired private ProductoNegocioRepository productoRepository;
    @Autowired private NamedParameterJdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken() {
        String token = "admin-" + UUID.randomUUID();
        Integer id = jdbc.queryForObject("""
                INSERT INTO usuarios (nombre, apellidos, correo, password_hash, telefono, rol,
                    telefono_verificado, cuenta_activa, token_sesion, sesion_fecha_creacion, sesion_fecha_expiracion)
                VALUES ('Admin','Root',:correo,:hash,:tel,'admin'::rol_usuario, TRUE, TRUE, :token, now(), now() + interval '30 days')
                RETURNING id_usuario""",
                new MapSqlParameterSource().addValue("correo", "adm-" + token + "@test.mx")
                        .addValue("hash", passwordEncoder.encode("x"))
                        .addValue("tel", "999" + System.nanoTime() % 100000000L).addValue("token", token),
                Integer.class);
        jdbc.update("INSERT INTO administradores (id_usuario) VALUES (:id)", new MapSqlParameterSource("id", id));
        return token;
    }

    private String verificarYLoguear(String correo) throws Exception {
        String otp = usuarioRepository.findByCorreo(correo).orElseThrow().getCodigoVerificacion();
        mvc.perform(post("/api/v1/auth/verificacion-telefono").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"%s\",\"codigo\":\"%s\"}".formatted(correo, otp)))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"%s\",\"password\":\"password123\"}".formatted(correo)))
                .andExpect(status().isOk());
        return usuarioRepository.findByCorreo(correo).orElseThrow().getTokenSesion();
    }

    private String registrarDueno(String correo, String tel, String negocio) throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"D","apellidos":"U","correo":"%s","password":"password123",
                         "telefono":"%s","rol":"repartidor","negocio":{"nombreComercial":"%s"}}"""
                        .formatted(correo, tel, negocio))).andExpect(status().isCreated());
        return verificarYLoguear(correo);
    }

    @Test
    void altaDeUsuarioPorAdminPermiteLoginInmediato() throws Exception {
        String admin = adminToken();
        mvc.perform(post("/api/v1/admin/usuarios").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","apellidos":"G","correo":"alta1@test.mx","password":"password123",
                                 "telefono":"5551230001","rol":"cliente"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value("alta1@test.mx"));

        // Pre-verificado: puede iniciar sesión sin OTP.
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"alta1@test.mx\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());

        // Correo duplicado → 409.
        mvc.perform(post("/api/v1/admin/usuarios").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","apellidos":"G","correo":"alta1@test.mx","password":"password123",
                                 "telefono":"5551230099","rol":"cliente"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void bajaConTransferenciaDeDueno() throws Exception {
        String admin = adminToken();
        String tokenA = registrarDueno("dueA@test.mx", "5551230010", "Negocio Transf");
        int idA = usuarioRepository.findByCorreo("dueA@test.mx").orElseThrow().getId();
        int idNegocio = repartidorRepository.findById(idA).orElseThrow().getIdNegocio();
        // Miembro B del mismo negocio.
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"B","apellidos":"M","correo":"memB@test.mx","password":"password123",
                         "telefono":"5551230011","rol":"repartidor","negocio":{"idExistente":%d}}""".formatted(idNegocio)))
                .andExpect(status().isCreated());
        verificarYLoguear("memB@test.mx");
        int idB = usuarioRepository.findByCorreo("memB@test.mx").orElseThrow().getId();

        mvc.perform(post("/api/v1/admin/usuarios/" + idA + "/baja").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"reestructura\",\"idNuevoDueno\":%d}".formatted(idB)))
                .andExpect(status().isNoContent());

        assertThat(negocioRepository.findById(idNegocio).orElseThrow().getIdDueno()).isEqualTo(idB);
        assertThat(negocioRepository.findById(idNegocio).orElseThrow().isActivo()).isTrue();
        // A ya no puede iniciar sesión (cuenta inactiva).
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"dueA@test.mx\",\"password\":\"password123\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void bajaSinTransferenciaDesactivaNegocioYReactivacion() throws Exception {
        String admin = adminToken();
        registrarDueno("dueC@test.mx", "5551230020", "Negocio Solo");
        int idC = usuarioRepository.findByCorreo("dueC@test.mx").orElseThrow().getId();
        int idNegocio = repartidorRepository.findById(idC).orElseThrow().getIdNegocio();

        mvc.perform(post("/api/v1/admin/usuarios/" + idC + "/baja").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"cierre\"}"))
                .andExpect(status().isNoContent());
        assertThat(negocioRepository.findById(idNegocio).orElseThrow().isActivo()).isFalse();

        // Reactivación → puede volver a iniciar sesión.
        mvc.perform(post("/api/v1/admin/usuarios/" + idC + "/reactivacion").header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"dueC@test.mx\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void historialGlobalConFiltroDeEstado() throws Exception {
        String admin = adminToken();
        // Un pedido pendiente para que el historial no esté vacío.
        String tokenCli = registrarCliente("hist@test.mx", "5551230030");
        int idDir = crearDireccion(tokenCli);
        String tokenDue = registrarDueno("hdue@test.mx", "5551230031", "Negocio Hist");
        int idRep = usuarioRepository.findByCorreo("hdue@test.mx").orElseThrow().getId();
        int idNegocio = repartidorRepository.findById(idRep).orElseThrow().getIdNegocio();
        int idMarca = seedProductoYHorario(tokenDue, idNegocio);
        mvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + tokenCli)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoSolicitud":"directa","idNegocio":%d,"idDireccionEntrega":%d,
                                 "detalles":[{"idMarca":%d,"cantidad":1,"tieneEnvase":true}]}"""
                                .formatted(idNegocio, idDir, idMarca)))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/admin/pedidos").header("Authorization", "Bearer " + admin)
                        .param("estado", "pendiente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.contenido[0].estado").value("pendiente"));
    }

    @Test
    void solicitudCambioNombreAprobadaAplicaElCambio() throws Exception {
        String admin = adminToken();
        String tokenDue = registrarDueno("sdue@test.mx", "5551230040", "Nombre Viejo");
        int idRep = usuarioRepository.findByCorreo("sdue@test.mx").orElseThrow().getId();
        int idNegocio = repartidorRepository.findById(idRep).orElseThrow().getIdNegocio();

        String cuerpo = mvc.perform(post("/api/v1/solicitudes").header("Authorization", "Bearer " + tokenDue)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigoCambio":"NOMBRE_NEGOCIO","valorNuevo":{"nombreComercial":"Nombre Nuevo"}}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("pendiente")).andReturn().getResponse().getContentAsString();
        int idSol = JsonPath.read(cuerpo, "$.id");

        // Admin aprueba → aplica el cambio.
        mvc.perform(post("/api/v1/admin/solicitudes/" + idSol + "/resolucion").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APROBADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("aprobado"));
        assertThat(negocioRepository.findById(idNegocio).orElseThrow().getNombreComercial()).isEqualTo("Nombre Nuevo");
    }

    @Test
    void rechazarSolicitudExigeComentario() throws Exception {
        String admin = adminToken();
        String tokenDue = registrarDueno("sdue2@test.mx", "5551230050", "Negocio Rech");
        String cuerpo = mvc.perform(post("/api/v1/solicitudes").header("Authorization", "Bearer " + tokenDue)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigoCambio\":\"NOMBRE_NEGOCIO\",\"valorNuevo\":{\"nombreComercial\":\"X\"}}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int idSol = JsonPath.read(cuerpo, "$.id");

        mvc.perform(post("/api/v1/admin/solicitudes/" + idSol + "/resolucion").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"RECHAZADO\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("COMENTARIO_REQUERIDO"));

        mvc.perform(post("/api/v1/admin/solicitudes/" + idSol + "/resolucion").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"RECHAZADO\",\"comentario\":\"nombre inapropiado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("rechazado"));
    }

    @Test
    void noDuenoNoPuedeSolicitar() throws Exception {
        String tokenDue = registrarDueno("sdue3@test.mx", "5551230060", "Negocio NoDueno");
        int idRep = usuarioRepository.findByCorreo("sdue3@test.mx").orElseThrow().getId();
        int idNegocio = repartidorRepository.findById(idRep).orElseThrow().getIdNegocio();
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"M","apellidos":"M","correo":"nomem@test.mx","password":"password123",
                         "telefono":"5551230061","rol":"repartidor","negocio":{"idExistente":%d}}""".formatted(idNegocio)))
                .andExpect(status().isCreated());
        String tokenMiembro = verificarYLoguear("nomem@test.mx");

        mvc.perform(post("/api/v1/solicitudes").header("Authorization", "Bearer " + tokenMiembro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigoCambio\":\"NOMBRE_NEGOCIO\",\"valorNuevo\":{\"nombreComercial\":\"X\"}}"))
                .andExpect(status().isForbidden());
    }

    // ---- helpers de escenario de pedido ----

    private String registrarCliente(String correo, String tel) throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"C","apellidos":"E","correo":"%s","password":"password123",
                         "telefono":"%s","rol":"cliente"}""".formatted(correo, tel))).andExpect(status().isCreated());
        return verificarYLoguear(correo);
    }

    private int crearDireccion(String token) throws Exception {
        String cuerpo = mvc.perform(post("/api/v1/clientes/me/direcciones").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"alias":"Casa","calle":"C","numeroExterior":"1","colonia":"Del Valle",
                                 "codigoPostal":"03100","referencias":"r","lat":19.372,"lon":-99.178}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.id");
    }

    private int seedProductoYHorario(String tokenDueno, int idNegocio) throws Exception {
        Marca marca = new Marca();
        marca.setNombre("MarcaAdm-" + idNegocio);
        int idMarca = marcaRepository.save(marca).getId();
        ProductoNegocio pn = new ProductoNegocio();
        pn.setIdNegocio(idNegocio);
        pn.setIdMarca(idMarca);
        pn.setPrecio(new BigDecimal("25.00"));
        pn.setPrecioEnvase(new BigDecimal("30.00"));
        pn.setCapacidadMaxima(1000);
        productoRepository.save(pn);
        StringBuilder h = new StringBuilder("[");
        for (int d = 1; d <= 7; d++) {
            if (d > 1) h.append(",");
            h.append("{\"diaSemana\":%d,\"cerrado\":false,\"horaApertura\":\"00:00:00\",\"horaCierre\":\"23:59:59\"}".formatted(d));
        }
        h.append("]");
        mvc.perform(put("/api/v1/negocios/me/horarios").header("Authorization", "Bearer " + tokenDueno)
                .contentType(MediaType.APPLICATION_JSON).content(h.toString())).andExpect(status().isOk());
        return idMarca;
    }
}
