package com.h2togo.backend.negocios;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.h2togo.backend.catalogo.ProductoNegocio;
import com.h2togo.backend.catalogo.ProductoNegocioRepository;
import com.h2togo.backend.catalogo.Marca;
import com.h2togo.backend.catalogo.MarcaRepository;
import com.h2togo.backend.inventario.LoteInventario;
import com.h2togo.backend.inventario.LoteInventarioRepository;
import com.h2togo.backend.usuarios.RepartidorRepository;
import com.h2togo.backend.usuarios.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
 * F4: negocios, catálogo y horarios (CU-022/023). Perfil con stock y abierto/cerrado,
 * cercanía KNN, edición de horarios/precio por el dueño (RN-021) y control por rol.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class NegociosFlowIT {

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

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RepartidorRepository repartidorRepository;
    @Autowired private MarcaRepository marcaRepository;
    @Autowired private ProductoNegocioRepository productoRepository;
    @Autowired private LoteInventarioRepository loteRepository;
    @Autowired private NamedParameterJdbcTemplate jdbc;

    // Dentro de Benito Juárez.
    private static final double LAT = 19.372, LON = -99.178;

    private record Setup(String token, int idUsuario, int idNegocio, int idProducto) {
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

    /** Da de alta un repartidor dueño con negocio nuevo, le siembra base+marca+producto+lote. */
    private Setup setupDueno(String correo, String tel, String negocio, String marcaNombre) throws Exception {
        String reg = """
                {"nombre":"Due","apellidos":"Ño","correo":"%s","password":"password123",
                 "telefono":"%s","rol":"repartidor","negocio":{"nombreComercial":"%s"}}"""
                .formatted(correo, tel, negocio);
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(reg))
                .andExpect(status().isCreated());
        String token = verificarYLoguear(correo);

        int idUsuario = usuarioRepository.findByCorreo(correo).orElseThrow().getId();
        int idNegocio = repartidorRepository.findById(idUsuario).orElseThrow().getIdNegocio();

        // Dirección de la base (all-or-nothing por CHECK) con ubicación dentro de BJ.
        jdbc.update("""
                UPDATE negocios SET calle = 'Av. Base', numero_exterior = '10', colonia = 'Del Valle',
                    codigo_postal = '03100',
                    ubicacion_base = ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography
                WHERE id_negocio = :id""",
                new MapSqlParameterSource().addValue("lon", LON).addValue("lat", LAT).addValue("id", idNegocio));

        Marca marca = new Marca();
        marca.setNombre(marcaNombre);
        marca = marcaRepository.save(marca);

        ProductoNegocio pn = new ProductoNegocio();
        pn.setIdNegocio(idNegocio);
        pn.setIdMarca(marca.getId());
        pn.setPrecio(new BigDecimal("25.00"));
        pn.setPrecioEnvase(new BigDecimal("30.00"));
        pn.setCapacidadMaxima(500);
        pn = productoRepository.save(pn);

        LoteInventario lote = new LoteInventario();
        lote.setIdNegocio(idNegocio);
        lote.setIdMarca(marca.getId());
        lote.setFechaCaducidad(LocalDate.now().plusMonths(3));
        lote.setCantidadInicial(100);
        lote.setCantidadActual(100);
        lote.setCantidadApartada(0);
        loteRepository.save(lote);

        return new Setup(token, idUsuario, idNegocio, pn.getId());
    }

    private static String horariosAbiertos() {
        StringBuilder sb = new StringBuilder("[");
        for (int d = 1; d <= 7; d++) {
            if (d > 1) sb.append(",");
            sb.append("{\"diaSemana\":").append(d)
                    .append(",\"cerrado\":false,\"horaApertura\":\"00:00:00\",\"horaCierre\":\"23:59:59\"}");
        }
        return sb.append("]").toString();
    }

    @Test
    void duenoConfiguraHorariosYVePerfilConStock() throws Exception {
        Setup s = setupDueno("due1@test.mx", "5553330001", "Purificadora Uno", "Marca-Uno");

        mvc.perform(put("/api/v1/negocios/me/horarios").header("Authorization", "Bearer " + s.token())
                        .contentType(MediaType.APPLICATION_JSON).content(horariosAbiertos()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7));

        mvc.perform(get("/api/v1/negocios/" + s.idNegocio() + "/perfil")
                        .header("Authorization", "Bearer " + s.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreComercial").value("Purificadora Uno"))
                .andExpect(jsonPath("$.direccion.lat").value(LAT))
                .andExpect(jsonPath("$.abiertoAhora").value(true))
                .andExpect(jsonPath("$.productos[0].stockDisponible").value(100))
                .andExpect(jsonPath("$.productos[0].precio").value(25.00));
    }

    @Test
    void cambioDePrecioEsInmediato() throws Exception {
        Setup s = setupDueno("due2@test.mx", "5553330002", "Purificadora Dos", "Marca-Dos");

        mvc.perform(put("/api/v1/negocios/me/productos/" + s.idProducto() + "/precio")
                        .header("Authorization", "Bearer " + s.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"precio\":99.50,\"precioEnvase\":40.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precio").value(99.50));

        mvc.perform(get("/api/v1/negocios/" + s.idNegocio() + "/perfil")
                        .header("Authorization", "Bearer " + s.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productos[0].precio").value(99.50));
    }

    @Test
    void horarioInconsistenteDevuelve422() throws Exception {
        Setup s = setupDueno("due3@test.mx", "5553330003", "Purificadora Tres", "Marca-Tres");
        // Día 1 abierto pero sin horas → inconsistente; el resto cerrado.
        StringBuilder sb = new StringBuilder("[{\"diaSemana\":1,\"cerrado\":false}");
        for (int d = 2; d <= 7; d++) sb.append(",{\"diaSemana\":").append(d).append(",\"cerrado\":true}");
        sb.append("]");

        mvc.perform(put("/api/v1/negocios/me/horarios").header("Authorization", "Bearer " + s.token())
                        .contentType(MediaType.APPLICATION_JSON).content(sb.toString()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("HORARIO_INVALIDO"));
    }

    @Test
    void repartidorNoDuenoNoPuedeEditar() throws Exception {
        Setup s = setupDueno("due4@test.mx", "5553330004", "Purificadora Cuatro", "Marca-Cuatro");
        // Segundo repartidor unido al MISMO negocio (miembro, no dueño).
        String regMiembro = """
                {"nombre":"Mie","apellidos":"Mbro","correo":"miembro@test.mx","password":"password123",
                 "telefono":"5553330040","rol":"repartidor","negocio":{"idExistente":%d}}"""
                .formatted(s.idNegocio());
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(regMiembro))
                .andExpect(status().isCreated());
        String tokenMiembro = verificarYLoguear("miembro@test.mx");

        mvc.perform(put("/api/v1/negocios/me/horarios").header("Authorization", "Bearer " + tokenMiembro)
                        .contentType(MediaType.APPLICATION_JSON).content(horariosAbiertos()))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/negocios/me/productos/" + s.idProducto() + "/precio")
                        .header("Authorization", "Bearer " + tokenMiembro)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"precio\":1.00,\"precioEnvase\":1.00}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cercanosDevuelveNegocioEnCobertura() throws Exception {
        Setup s = setupDueno("due5@test.mx", "5553330005", "Purificadora Cinco", "Marca-Cinco");

        // Varios negocios pueden compartir coordenadas en el contenedor; basta con que el
        // negocio aparezca en la lista de cercanos (en cobertura) con distancia pequeña.
        mvc.perform(get("/api/v1/negocios").param("cerca", LAT + "," + LON)
                        .header("Authorization", "Bearer " + s.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + s.idNegocio() + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + s.idNegocio() + " && @.distanciaM < 50)]").exists());
    }

    @Test
    void clienteNoVeMiNegocioPeroSiElPerfilPublico() throws Exception {
        Setup s = setupDueno("due6@test.mx", "5553330006", "Purificadora Seis", "Marca-Seis");

        String regCli = """
                {"nombre":"Cli","apellidos":"Ente","correo":"cli6@test.mx","password":"password123",
                 "telefono":"5553330060","rol":"cliente"}""";
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(regCli))
                .andExpect(status().isCreated());
        String tokenCliente = verificarYLoguear("cli6@test.mx");

        mvc.perform(get("/api/v1/negocios/me").header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/negocios/" + s.idNegocio() + "/perfil")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk());
    }

    @Test
    void listaMarcasGlobales() throws Exception {
        Setup s = setupDueno("due7@test.mx", "5553330007", "Purificadora Siete", "Marca-Siete");

        mvc.perform(get("/api/v1/marcas").header("Authorization", "Bearer " + s.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nombre == 'Marca-Siete')]").exists());
    }
}
