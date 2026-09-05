package com.h2togo.backend.pedidos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.h2togo.backend.catalogo.Marca;
import com.h2togo.backend.catalogo.MarcaRepository;
import com.h2togo.backend.catalogo.ProductoNegocio;
import com.h2togo.backend.catalogo.ProductoNegocioRepository;
import com.h2togo.backend.usuarios.RepartidorRepository;
import com.h2togo.backend.usuarios.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
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
 * F6: pedidos (CU-004/005/007/013). Creación directa/abierta con snapshot de precios,
 * validación de cobertura y horario, cancelación (solo pendiente), ownership e historial.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PedidosFlowIT {

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

    private static final double LAT = 19.372, LON = -99.178;     // dentro de BJ
    private static final double LAT_FUERA = 19.5, LON_FUERA = -99.0; // fuera

    private record Cli(String token, int idCliente, int idDireccion) {
    }

    private record Neg(String tokenDueno, int idNegocio, int idMarca, int idProducto) {
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

    private Cli registrarCliente(String correo, String tel, double lat, double lon) throws Exception {
        String reg = """
                {"nombre":"Cli","apellidos":"Ente","correo":"%s","password":"password123",
                 "telefono":"%s","rol":"cliente"}""".formatted(correo, tel);
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(reg))
                .andExpect(status().isCreated());
        String token = verificarYLoguear(correo);
        int idCliente = usuarioRepository.findByCorreo(correo).orElseThrow().getId();

        String dir = """
                {"alias":"Casa","calle":"Insurgentes","numeroExterior":"1","colonia":"Del Valle",
                 "codigoPostal":"03100","referencias":"ref","lat":%s,"lon":%s}""".formatted(lat, lon);
        String cuerpo = mvc.perform(post("/api/v1/clientes/me/direcciones")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(dir))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new Cli(token, idCliente, JsonPath.read(cuerpo, "$.id"));
    }

    private Neg setupNegocio(String correo, String tel, String negocio, String marcaNombre,
            String precio, boolean abierto) throws Exception {
        String reg = """
                {"nombre":"Due","apellidos":"Ño","correo":"%s","password":"password123",
                 "telefono":"%s","rol":"repartidor","negocio":{"nombreComercial":"%s"}}"""
                .formatted(correo, tel, negocio);
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(reg))
                .andExpect(status().isCreated());
        String token = verificarYLoguear(correo);
        int idRep = usuarioRepository.findByCorreo(correo).orElseThrow().getId();
        int idNegocio = repartidorRepository.findById(idRep).orElseThrow().getIdNegocio();

        Marca marca = new Marca();
        marca.setNombre(marcaNombre);
        int idMarca = marcaRepository.save(marca).getId();

        ProductoNegocio pn = new ProductoNegocio();
        pn.setIdNegocio(idNegocio);
        pn.setIdMarca(idMarca);
        pn.setPrecio(new BigDecimal(precio));
        pn.setPrecioEnvase(new BigDecimal("30.00"));
        pn.setCapacidadMaxima(1000);
        int idProducto = productoRepository.save(pn).getId();

        // Horario: 7 días abiertos 00:00–23:59 (o todos cerrados).
        StringBuilder sb = new StringBuilder("[");
        for (int d = 1; d <= 7; d++) {
            if (d > 1) sb.append(",");
            sb.append(abierto
                    ? "{\"diaSemana\":%d,\"cerrado\":false,\"horaApertura\":\"00:00:00\",\"horaCierre\":\"23:59:59\"}".formatted(d)
                    : "{\"diaSemana\":%d,\"cerrado\":true}".formatted(d));
        }
        sb.append("]");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/negocios/me/horarios").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(sb.toString()))
                .andExpect(status().isOk());
        return new Neg(token, idNegocio, idMarca, idProducto);
    }

    private static String pedidoDirecto(int idNegocio, int idDireccion, int idMarca, int cantidad, boolean envase) {
        return """
                {"tipoSolicitud":"directa","idNegocio":%d,"idDireccionEntrega":%d,
                 "detalles":[{"idMarca":%d,"cantidad":%d,"tieneEnvase":%s}]}"""
                .formatted(idNegocio, idDireccion, idMarca, cantidad, envase);
    }

    @Test
    void crearPedidoDirectoCalculaTotalConSnapshot() throws Exception {
        Cli c = registrarCliente("ped1@test.mx", "5556660001", LAT, LON);
        Neg n = setupNegocio("dped1@test.mx", "5556660010", "Purif Ped1", "MarcaPed1", "25.00", true);

        mvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + c.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoDirecto(n.idNegocio(), c.idDireccion(), n.idMarca(), 2, false)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("pendiente"))
                .andExpect(jsonPath("$.garrafonesTotales").value(2))
                .andExpect(jsonPath("$.totalPagar").value(110.00)) // 2 * (25 + 30)
                .andExpect(jsonPath("$.detalles[0].precioUnitario").value(25.00))
                .andExpect(jsonPath("$.historial[0].estado").value("pendiente"));
    }

    @Test
    void snapshotDePrecioNoCambiaAlCambiarElCatalogo() throws Exception {
        Cli c = registrarCliente("ped2@test.mx", "5556660002", LAT, LON);
        Neg n = setupNegocio("dped2@test.mx", "5556660020", "Purif Ped2", "MarcaPed2", "25.00", true);

        String cuerpo = mvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + c.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoDirecto(n.idNegocio(), c.idDireccion(), n.idMarca(), 1, true)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int idPedido = JsonPath.read(cuerpo, "$.id");

        // El dueño sube el precio después (RF-028). No debe afectar el pedido ya creado (RN-025).
        ProductoNegocio pn = productoRepository.findById(n.idProducto()).orElseThrow();
        pn.setPrecio(new BigDecimal("99.00"));
        productoRepository.saveAndFlush(pn);

        mvc.perform(get("/api/v1/pedidos/" + idPedido).header("Authorization", "Bearer " + c.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detalles[0].precioUnitario").value(25.00));
    }

    @Test
    void negocioCerradoDevuelve422() throws Exception {
        Cli c = registrarCliente("ped3@test.mx", "5556660003", LAT, LON);
        Neg n = setupNegocio("dped3@test.mx", "5556660030", "Purif Ped3", "MarcaPed3", "25.00", false); // cerrado

        mvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + c.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoDirecto(n.idNegocio(), c.idDireccion(), n.idMarca(), 1, false)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("NEGOCIO_CERRADO"));
    }

    @Test
    void direccionFueraDeCoberturaDevuelve422() throws Exception {
        Cli c = registrarCliente("ped4@test.mx", "5556660004", LAT_FUERA, LON_FUERA);
        Neg n = setupNegocio("dped4@test.mx", "5556660040", "Purif Ped4", "MarcaPed4", "25.00", true);

        mvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + c.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoDirecto(n.idNegocio(), c.idDireccion(), n.idMarca(), 1, false)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("FUERA_DE_COBERTURA"));
    }

    @Test
    void abiertaSinPrecioMaximoYSinPurificadora() throws Exception {
        Cli c = registrarCliente("ped5@test.mx", "5556660005", LAT, LON);
        Neg n = setupNegocio("dped5@test.mx", "5556660050", "Purif Ped5", "MarcaPed5", "25.00", true);

        // Sin precio máximo → 422.
        mvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + c.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoSolicitud":"abierta","idDireccionEntrega":%d,
                                 "detalles":[{"idMarca":%d,"cantidad":1,"tieneEnvase":true}]}"""
                                .formatted(c.idDireccion(), n.idMarca())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("PRECIO_MAXIMO_REQUERIDO"));

        // Precio máximo por debajo de todos los negocios → sin purificadoras.
        mvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + c.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoSolicitud":"abierta","precioMaximoGarrafon":10.00,"idDireccionEntrega":%d,
                                 "detalles":[{"idMarca":%d,"cantidad":1,"tieneEnvase":true}]}"""
                                .formatted(c.idDireccion(), n.idMarca())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("SIN_PURIFICADORAS"));
    }

    @Test
    void abiertaConPurificadoraQueCumpleCrea() throws Exception {
        Cli c = registrarCliente("ped6@test.mx", "5556660006", LAT, LON);
        Neg n = setupNegocio("dped6@test.mx", "5556660060", "Purif Ped6", "MarcaPed6", "25.00", true);

        mvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + c.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoSolicitud":"abierta","precioMaximoGarrafon":30.00,"idDireccionEntrega":%d,
                                 "detalles":[{"idMarca":%d,"cantidad":2,"tieneEnvase":true}]}"""
                                .formatted(c.idDireccion(), n.idMarca())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoSolicitud").value("abierta"))
                .andExpect(jsonPath("$.totalPagar").value(60.00)); // 2 * 30 (estimado)
    }

    @Test
    void cancelarPendienteYLuegoNoCancelable() throws Exception {
        Cli c = registrarCliente("ped7@test.mx", "5556660007", LAT, LON);
        Neg n = setupNegocio("dped7@test.mx", "5556660070", "Purif Ped7", "MarcaPed7", "25.00", true);
        String cuerpo = mvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + c.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoDirecto(n.idNegocio(), c.idDireccion(), n.idMarca(), 1, false)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int idPedido = JsonPath.read(cuerpo, "$.id");

        mvc.perform(post("/api/v1/pedidos/" + idPedido + "/cancelacion")
                        .header("Authorization", "Bearer " + c.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("cancelado"));

        // Ya cancelado → no cancelable.
        mvc.perform(post("/api/v1/pedidos/" + idPedido + "/cancelacion")
                        .header("Authorization", "Bearer " + c.token()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("PEDIDO_NO_CANCELABLE"));
    }

    @Test
    void otroClienteNoVeElPedido() throws Exception {
        Cli a = registrarCliente("ped8a@test.mx", "5556660008", LAT, LON);
        Neg n = setupNegocio("dped8@test.mx", "5556660080", "Purif Ped8", "MarcaPed8", "25.00", true);
        String cuerpo = mvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + a.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedidoDirecto(n.idNegocio(), a.idDireccion(), n.idMarca(), 1, false)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int idPedido = JsonPath.read(cuerpo, "$.id");

        Cli b = registrarCliente("ped8b@test.mx", "5556660081", LAT, LON);
        mvc.perform(get("/api/v1/pedidos/" + idPedido).header("Authorization", "Bearer " + b.token()))
                .andExpect(status().isForbidden());

        // El cliente dueño sí lo ve y aparece en su listado.
        mvc.perform(get("/api/v1/clientes/me/pedidos").header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].id").value(idPedido));
    }
}
