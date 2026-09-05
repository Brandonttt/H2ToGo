package com.h2togo.backend.e2e;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.h2togo.backend.catalogo.Marca;
import com.h2togo.backend.catalogo.MarcaRepository;
import com.h2togo.backend.catalogo.ProductoNegocio;
import com.h2togo.backend.catalogo.ProductoNegocioRepository;
import com.h2togo.backend.common.enums.TipoVehiculo;
import com.h2togo.backend.inventario.InventarioService;
import com.h2togo.backend.inventario.dto.CargaItem;
import com.h2togo.backend.inventario.dto.LoteEntradaRequest;
import com.h2togo.backend.repartidores.dto.IniciarJornadaRequest;
import com.h2togo.backend.usuarios.RepartidorRepository;
import com.h2togo.backend.usuarios.UsuarioRepository;
import com.h2togo.backend.vehiculos.VehiculoNegocio;
import com.h2togo.backend.vehiculos.VehiculoNegocioRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
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

/**
 * F12: prueba end-to-end del flujo feliz completo, atravesando todas las fases sobre la BD real
 * (Testcontainers + PostGIS + polígono OSM): registro → verificación → login → dirección →
 * catálogo/horario/inventario/jornada → pedido directo → aceptación → en camino → ubicación →
 * ruta A* → entrega. Y las variantes de creación abierta y programada.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class E2EFlujoFelizIT {

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
    @Autowired private MarcaRepository marcaRepository;
    @Autowired private ProductoNegocioRepository productoRepository;
    @Autowired private VehiculoNegocioRepository vehiculoRepository;
    @Autowired private InventarioService inventarioService;

    // Nodos reales y conectados del grafo OSM de BJ (en cobertura): A* halla ruta determinista.
    private static final double LAT_DEST = 19.377934, LON_DEST = -99.171138; // dirección de entrega
    private static final double LAT_REP = 19.376692, LON_REP = -99.165057;   // ubicación del repartidor

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

    private record Cliente(String token, int idDireccion) {
    }

    private Cliente registrarCliente(String correo, String tel, double lat, double lon) throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"Cli","apellidos":"Ente","correo":"%s","password":"password123",
                         "telefono":"%s","rol":"cliente"}""".formatted(correo, tel)))
                .andExpect(status().isCreated());
        String token = verificarYLoguear(correo);
        String cuerpo = mvc.perform(post("/api/v1/clientes/me/direcciones").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"alias":"Casa","calle":"Nueva York","numeroExterior":"1","colonia":"Napoles",
                                 "codigoPostal":"03810","referencias":"Porton azul","lat":%s,"lon":%s}"""
                                .formatted(lat, lon)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new Cliente(token, JsonPath.read(cuerpo, "$.id"));
    }

    /** Registra dueño + catálogo + horario abierto; devuelve [tokenRep, idRep, idNegocio, idMarca]. */
    private Object[] registrarNegocioAbierto(String correo, String tel, String nombre, String marcaNombre,
            String precio) throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"Due","apellidos":"Ño","correo":"%s","password":"password123",
                         "telefono":"%s","rol":"repartidor","negocio":{"nombreComercial":"%s"}}"""
                        .formatted(correo, tel, nombre)))
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
        productoRepository.save(pn);

        StringBuilder h = new StringBuilder("[");
        for (int d = 1; d <= 7; d++) {
            if (d > 1) h.append(",");
            h.append("{\"diaSemana\":%d,\"cerrado\":false,\"horaApertura\":\"00:00:00\",\"horaCierre\":\"23:59:59\"}".formatted(d));
        }
        h.append("]");
        mvc.perform(put("/api/v1/negocios/me/horarios").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(h.toString())).andExpect(status().isOk());
        return new Object[]{token, idRep, idNegocio, idMarca};
    }

    @Test
    void flujoFelizDirectoDeExtremoAExtremo() throws Exception {
        // (a)(c) Cliente + dirección en cobertura.
        Cliente cli = registrarCliente("e2e.cli@test.mx", "5551000001", LAT_DEST, LON_DEST);
        // (b)(d) Dueño + catálogo + horario.
        Object[] neg = registrarNegocioAbierto("e2e.due@test.mx", "5551000002", "Purif E2E", "MarcaE2E", "25.00");
        String tokenRep = (String) neg[0];
        int idRep = (int) neg[1];
        int idNegocio = (int) neg[2];
        int idMarca = (int) neg[3];

        // (d) Vehículo + lote + jornada con carga.
        VehiculoNegocio v = new VehiculoNegocio();
        v.setIdNegocio(idNegocio);
        v.setTipoVehiculo(TipoVehiculo.motocicleta);
        v.setMarca("Honda");
        v.setColor("Rojo");
        v.setCapacidadGarrafones(50);
        int idVehiculo = vehiculoRepository.save(v).getId();
        inventarioService.registrarLote(idRep,
                new LoteEntradaRequest(idMarca, 50, LocalDate.now().plusMonths(3), null, null, null));
        inventarioService.iniciarJornada(idRep, new IniciarJornadaRequest(idVehiculo, List.of(new CargaItem(idMarca, 20))));

        // (e) Pedido directo.
        String cuerpoPed = mvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + cli.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoSolicitud":"directa","idNegocio":%d,"idDireccionEntrega":%d,
                                 "detalles":[{"idMarca":%d,"cantidad":2,"tieneEnvase":true}]}"""
                                .formatted(idNegocio, cli.idDireccion(), idMarca)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("pendiente"))
                .andReturn().getResponse().getContentAsString();
        int idPedido = JsonPath.read(cuerpoPed, "$.id");
        int idDetalle = JsonPath.read(cuerpoPed, "$.detalles[0].id");

        // (f) Aceptar → asignado.
        mvc.perform(post("/api/v1/pedidos/" + idPedido + "/aceptacion").header("Authorization", "Bearer " + tokenRep))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("asignado"))
                .andExpect(jsonPath("$.idRepartidor").value(idRep));

        // En camino.
        mvc.perform(post("/api/v1/pedidos/" + idPedido + "/en-camino").header("Authorization", "Bearer " + tokenRep)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lat\":%s,\"lon\":%s}".formatted(LAT_REP, LON_REP)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("en_camino"));

        // Reportar ubicación (habilita la ruta).
        mvc.perform(put("/api/v1/repartidores/me/ubicacion").header("Authorization", "Bearer " + tokenRep)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lat\":%s,\"lon\":%s}".formatted(LAT_REP, LON_REP)))
                .andExpect(status().isNoContent());

        // Ruta A* sobre el grafo OSM real.
        mvc.perform(get("/api/v1/pedidos/" + idPedido + "/ruta").header("Authorization", "Bearer " + tokenRep))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encontrada").value(true))
                .andExpect(jsonPath("$.coordenadas.length()").value(Matchers.greaterThan(0)));

        // Entrega (a ≤50 m de la dirección) → entregado.
        mvc.perform(post("/api/v1/pedidos/" + idPedido + "/resultado").header("Authorization", "Bearer " + tokenRep)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resultado":"ENTREGADO","lat":%s,"lon":%s,
                                 "lineas":[{"idDetalle":%d,"cantidadEntregada":2,"agregadaEnSitio":false}]}"""
                                .formatted(LAT_DEST, LON_DEST, idDetalle)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("entregado"));

        // El cliente ve el pedido cerrado en su historial.
        mvc.perform(get("/api/v1/pedidos/" + idPedido).header("Authorization", "Bearer " + cli.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("entregado"));
    }

    @Test
    void creacionAbiertaYProgramada() throws Exception {
        Cliente cli = registrarCliente("e2e.cli2@test.mx", "5551000003", LAT_DEST, LON_DEST);
        Object[] neg = registrarNegocioAbierto("e2e.due2@test.mx", "5551000004", "Purif E2E2", "MarcaE2E2", "25.00");
        int idNegocio = (int) neg[2];
        int idMarca = (int) neg[3];

        // Abierta: precio máximo por encima del ofertado → se crea.
        mvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + cli.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoSolicitud":"abierta","precioMaximoGarrafon":30.00,"idDireccionEntrega":%d,
                                 "detalles":[{"idMarca":%d,"cantidad":2,"tieneEnvase":true}]}"""
                                .formatted(cli.idDireccion(), idMarca)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoSolicitud").value("abierta"));

        // Programada: directa con fecha futura → queda programado (no se dispara aún).
        String fecha = OffsetDateTime.now().plusDays(1).toString();
        mvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + cli.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoSolicitud":"directa","idNegocio":%d,"idDireccionEntrega":%d,
                                 "programado":{"fechaProgramada":"%s"},
                                 "detalles":[{"idMarca":%d,"cantidad":1,"tieneEnvase":false}]}"""
                                .formatted(idNegocio, cli.idDireccion(), fecha, idMarca)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.esProgramado").value(true));
    }
}
