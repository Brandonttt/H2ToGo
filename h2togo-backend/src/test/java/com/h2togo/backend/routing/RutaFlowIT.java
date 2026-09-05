package com.h2togo.backend.routing;

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
import java.util.List;
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
 * F9: ruta de entrega (CU-011). Con un pedido asignado y la ubicación del repartidor,
 * {@code GET /pedidos/{id}/ruta} corre A* sobre el grafo OSM real de Benito Juárez.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class RutaFlowIT {

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

    // Dos nodos REALES y conectados del grafo OSM (extremos de una misma vía en el centro de BJ),
    // para que A* encuentre una ruta determinista y ambos caigan en zona de cobertura.
    private static final double LAT_DEST = 19.377934, LON_DEST = -99.171138;
    private static final double LAT_REP = 19.376692, LON_REP = -99.165057;

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

    /** Deja un pedido asignado al repartidor y devuelve [idPedido, tokenRepartidor]. */
    private Object[] pedidoAsignado() throws Exception {
        String correoCli = "rutac@test.mx";
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"Cli","apellidos":"E","correo":"%s","password":"password123",
                         "telefono":"5559990001","rol":"cliente"}""".formatted(correoCli)))
                .andExpect(status().isCreated());
        String tokenCli = verificarYLoguear(correoCli);
        String dir = """
                {"alias":"Casa","calle":"C","numeroExterior":"1","colonia":"Del Valle",
                 "codigoPostal":"03100","referencias":"r","lat":%s,"lon":%s}""".formatted(LAT_DEST, LON_DEST);
        String cuerpoDir = mvc.perform(post("/api/v1/clientes/me/direcciones").header("Authorization", "Bearer " + tokenCli)
                        .contentType(MediaType.APPLICATION_JSON).content(dir))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int idDireccion = JsonPath.read(cuerpoDir, "$.id");

        String correoDue = "ruted@test.mx";
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"Due","apellidos":"Ño","correo":"%s","password":"password123",
                         "telefono":"5559990002","rol":"repartidor","negocio":{"nombreComercial":"Purif Ruta"}}""".formatted(correoDue)))
                .andExpect(status().isCreated());
        String tokenRep = verificarYLoguear(correoDue);
        int idRep = usuarioRepository.findByCorreo(correoDue).orElseThrow().getId();
        int idNegocio = repartidorRepository.findById(idRep).orElseThrow().getIdNegocio();

        Marca marca = new Marca();
        marca.setNombre("MarcaRuta");
        int idMarca = marcaRepository.save(marca).getId();
        ProductoNegocio pn = new ProductoNegocio();
        pn.setIdNegocio(idNegocio);
        pn.setIdMarca(idMarca);
        pn.setPrecio(new BigDecimal("25.00"));
        pn.setPrecioEnvase(new BigDecimal("30.00"));
        pn.setCapacidadMaxima(1000);
        productoRepository.save(pn);
        VehiculoNegocio v = new VehiculoNegocio();
        v.setIdNegocio(idNegocio);
        v.setTipoVehiculo(TipoVehiculo.motocicleta);
        v.setMarca("Honda");
        v.setColor("Rojo");
        v.setCapacidadGarrafones(50);
        int idVehiculo = vehiculoRepository.save(v).getId();

        StringBuilder h = new StringBuilder("[");
        for (int d = 1; d <= 7; d++) {
            if (d > 1) h.append(",");
            h.append("{\"diaSemana\":%d,\"cerrado\":false,\"horaApertura\":\"00:00:00\",\"horaCierre\":\"23:59:59\"}".formatted(d));
        }
        h.append("]");
        mvc.perform(put("/api/v1/negocios/me/horarios").header("Authorization", "Bearer " + tokenRep)
                .contentType(MediaType.APPLICATION_JSON).content(h.toString())).andExpect(status().isOk());
        inventarioService.registrarLote(idRep,
                new LoteEntradaRequest(idMarca, 50, LocalDate.now().plusMonths(3), null, null, null));
        inventarioService.iniciarJornada(idRep, new IniciarJornadaRequest(idVehiculo, List.of(new CargaItem(idMarca, 20))));

        String body = """
                {"tipoSolicitud":"directa","idNegocio":%d,"idDireccionEntrega":%d,
                 "detalles":[{"idMarca":%d,"cantidad":2,"tieneEnvase":true}]}""".formatted(idNegocio, idDireccion, idMarca);
        String cuerpoPed = mvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + tokenCli)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int idPedido = JsonPath.read(cuerpoPed, "$.id");
        mvc.perform(post("/api/v1/pedidos/" + idPedido + "/aceptacion").header("Authorization", "Bearer " + tokenRep))
                .andExpect(status().isOk());
        return new Object[]{idPedido, tokenRep};
    }

    @Test
    void sinUbicacionDevuelve422YConUbicacionCalculaRuta() throws Exception {
        Object[] esc = pedidoAsignado();
        int idPedido = (int) esc[0];
        String tokenRep = (String) esc[1];

        // Aún sin ubicación reportada → 422.
        mvc.perform(get("/api/v1/pedidos/" + idPedido + "/ruta").header("Authorization", "Bearer " + tokenRep))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("SIN_UBICACION"));

        // Reporta ubicación y pide la ruta → A* sobre el grafo OSM de BJ.
        mvc.perform(put("/api/v1/repartidores/me/ubicacion").header("Authorization", "Bearer " + tokenRep)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lat\":%s,\"lon\":%s}".formatted(LAT_REP, LON_REP)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/pedidos/" + idPedido + "/ruta").header("Authorization", "Bearer " + tokenRep))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encontrada").value(true))
                .andExpect(jsonPath("$.coordenadas.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }
}
