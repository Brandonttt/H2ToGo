package com.h2togo.backend.pedidos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.h2togo.backend.catalogo.Marca;
import com.h2togo.backend.catalogo.MarcaRepository;
import com.h2togo.backend.catalogo.ProductoNegocio;
import com.h2togo.backend.catalogo.ProductoNegocioRepository;
import com.h2togo.backend.common.ConflictException;
import com.h2togo.backend.inventario.InventarioService;
import com.h2togo.backend.inventario.dto.CargaItem;
import com.h2togo.backend.inventario.dto.LoteEntradaRequest;
import com.h2togo.backend.repartidores.dto.IniciarJornadaRequest;
import com.h2togo.backend.usuarios.RepartidorRepository;
import com.h2togo.backend.usuarios.UsuarioRepository;
import com.h2togo.backend.vehiculos.VehiculoNegocio;
import com.h2togo.backend.vehiculos.VehiculoNegocioRepository;
import com.h2togo.backend.common.enums.TipoVehiculo;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
 * F7: asignación (CU-010) y entrega (CU-012). Flujo aceptar→en camino→entregar con
 * consumo de inventario, concurrencia RF-019, carga insuficiente, entrega a ≤50 m,
 * entregas parciales (RN-032) y suspensión por 2 ausencias (RN-006).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AsignacionEntregaFlowIT {

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
    @Autowired private AsignacionService asignacionService;

    private static final double LAT = 19.372, LON = -99.178;

    private record Cli(String token, int idCliente, int idDireccion) {
    }

    private record Rep(String token, int idRep, int idNegocio, int idMarca, int idVehiculo) {
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

    private Cli registrarCliente(String correo, String tel) throws Exception {
        String reg = """
                {"nombre":"Cli","apellidos":"Ente","correo":"%s","password":"password123",
                 "telefono":"%s","rol":"cliente"}""".formatted(correo, tel);
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(reg))
                .andExpect(status().isCreated());
        String token = verificarYLoguear(correo);
        int idCliente = usuarioRepository.findByCorreo(correo).orElseThrow().getId();
        String dir = """
                {"alias":"Casa","calle":"C","numeroExterior":"1","colonia":"Del Valle",
                 "codigoPostal":"03100","referencias":"r","lat":%s,"lon":%s}""".formatted(LAT, LON);
        String cuerpo = mvc.perform(post("/api/v1/clientes/me/direcciones").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(dir))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new Cli(token, idCliente, JsonPath.read(cuerpo, "$.id"));
    }

    private int seedVehiculo(int idNegocio, int cap) {
        VehiculoNegocio v = new VehiculoNegocio();
        v.setIdNegocio(idNegocio);
        v.setTipoVehiculo(TipoVehiculo.motocicleta);
        v.setMarca("Honda");
        v.setColor("Rojo");
        v.setCapacidadGarrafones(cap);
        return vehiculoRepository.save(v).getId();
    }

    /** Dueño con negocio, marca, producto, horario abierto, lote base y jornada con carga. */
    private Rep setupDueno(String correo, String tel, String negocio, String marcaNombre,
            String precio, int baseQty, int cargaQty) throws Exception {
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
        productoRepository.save(pn);
        int idVehiculo = seedVehiculo(idNegocio, 50);

        horarioAbierto(token);
        inventarioService.registrarLote(idRep,
                new LoteEntradaRequest(idMarca, baseQty, LocalDate.now().plusMonths(3), null, null, null));
        inventarioService.iniciarJornada(idRep,
                new IniciarJornadaRequest(idVehiculo, List.of(new CargaItem(idMarca, cargaQty))));
        return new Rep(token, idRep, idNegocio, idMarca, idVehiculo);
    }

    /** Repartidor miembro del negocio con su vehículo y jornada cargada desde la misma base. */
    private Rep addMiembro(int idNegocio, int idMarca, String correo, String tel, int cargaQty) throws Exception {
        String reg = """
                {"nombre":"Mie","apellidos":"Mbro","correo":"%s","password":"password123",
                 "telefono":"%s","rol":"repartidor","negocio":{"idExistente":%d}}"""
                .formatted(correo, tel, idNegocio);
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(reg))
                .andExpect(status().isCreated());
        String token = verificarYLoguear(correo);
        int idRep = usuarioRepository.findByCorreo(correo).orElseThrow().getId();
        int idVehiculo = seedVehiculo(idNegocio, 50);
        inventarioService.iniciarJornada(idRep,
                new IniciarJornadaRequest(idVehiculo, List.of(new CargaItem(idMarca, cargaQty))));
        return new Rep(token, idRep, idNegocio, idMarca, idVehiculo);
    }

    private void horarioAbierto(String tokenDueno) throws Exception {
        StringBuilder sb = new StringBuilder("[");
        for (int d = 1; d <= 7; d++) {
            if (d > 1) sb.append(",");
            sb.append("{\"diaSemana\":%d,\"cerrado\":false,\"horaApertura\":\"00:00:00\",\"horaCierre\":\"23:59:59\"}".formatted(d));
        }
        sb.append("]");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/api/v1/negocios/me/horarios").header("Authorization", "Bearer " + tokenDueno)
                .contentType(MediaType.APPLICATION_JSON).content(sb.toString())).andExpect(status().isOk());
    }

    /** Crea un pedido directo; devuelve [idPedido, idDetalle]. */
    private int[] crearPedido(String tokenCliente, int idNegocio, int idDireccion, int idMarca, int cantidad)
            throws Exception {
        String body = """
                {"tipoSolicitud":"directa","idNegocio":%d,"idDireccionEntrega":%d,
                 "detalles":[{"idMarca":%d,"cantidad":%d,"tieneEnvase":true}]}"""
                .formatted(idNegocio, idDireccion, idMarca, cantidad);
        String cuerpo = mvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new int[]{JsonPath.read(cuerpo, "$.id"), JsonPath.read(cuerpo, "$.detalles[0].id")};
    }

    private String entrega(double lat, double lon, int idDetalle, int cantidad) {
        return """
                {"resultado":"ENTREGADO","lat":%s,"lon":%s,
                 "lineas":[{"idDetalle":%d,"cantidadEntregada":%d,"agregadaEnSitio":false}]}"""
                .formatted(lat, lon, idDetalle, cantidad);
    }

    @Test
    void flujoAceptarEnCaminoEntregarConsumeInventario() throws Exception {
        Cli c = registrarCliente("f7c1@test.mx", "5557770001");
        Rep r = setupDueno("f7d1@test.mx", "5557770010", "Purif F7-1", "MarcaF71", "25.00", 100, 10);
        int[] ped = crearPedido(c.token(), r.idNegocio(), c.idDireccion(), r.idMarca(), 2);

        mvc.perform(post("/api/v1/pedidos/" + ped[0] + "/aceptacion").header("Authorization", "Bearer " + r.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("asignado"))
                .andExpect(jsonPath("$.idRepartidor").value(r.idRep()));

        mvc.perform(post("/api/v1/pedidos/" + ped[0] + "/en-camino").header("Authorization", "Bearer " + r.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lat\":%s,\"lon\":%s}".formatted(LAT, LON)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("en_camino"));

        mvc.perform(post("/api/v1/pedidos/" + ped[0] + "/resultado").header("Authorization", "Bearer " + r.token())
                        .contentType(MediaType.APPLICATION_JSON).content(entrega(LAT, LON, ped[1], 2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("entregado"));

        // El vehículo quedó con 8 (cargó 10, entregó 2).
        mvc.perform(get("/api/v1/inventario/vehiculo").header("Authorization", "Bearer " + r.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ocupado").value(8));
    }

    @Test
    void aceptacionConcurrenteSoloUnoGana() throws Exception {
        Cli c = registrarCliente("f7c2@test.mx", "5557770002");
        Rep a = setupDueno("f7d2@test.mx", "5557770020", "Purif F7-2", "MarcaF72", "25.00", 100, 10);
        Rep b = addMiembro(a.idNegocio(), a.idMarca(), "f7m2@test.mx", "5557770021", 10);
        int[] ped = crearPedido(c.token(), a.idNegocio(), c.idDireccion(), a.idMarca(), 2);

        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch arranque = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<Boolean> fA = pool.submit(() -> intentarAceptar(a.idRep(), ped[0], listos, arranque));
        Future<Boolean> fB = pool.submit(() -> intentarAceptar(b.idRep(), ped[0], listos, arranque));
        listos.await();
        arranque.countDown();

        int exitos = 0;
        try { if (Boolean.TRUE.equals(fA.get())) exitos++; } catch (Exception ignored) { }
        try { if (Boolean.TRUE.equals(fB.get())) exitos++; } catch (Exception ignored) { }
        pool.shutdown();
        assertThat(exitos).isEqualTo(1);
    }

    private boolean intentarAceptar(int idRep, int idPedido, CountDownLatch listos, CountDownLatch arranque)
            throws InterruptedException {
        listos.countDown();
        arranque.await();
        try {
            asignacionService.aceptar(idRep, idPedido);
            return true;
        } catch (ConflictException e) {
            return false;
        }
    }

    @Test
    void aceptarSinCargaSuficienteDevuelve422() throws Exception {
        Cli c = registrarCliente("f7c3@test.mx", "5557770003");
        Rep r = setupDueno("f7d3@test.mx", "5557770030", "Purif F7-3", "MarcaF73", "25.00", 100, 1); // solo 1 cargado
        int[] ped = crearPedido(c.token(), r.idNegocio(), c.idDireccion(), r.idMarca(), 5); // pide 5

        mvc.perform(post("/api/v1/pedidos/" + ped[0] + "/aceptacion").header("Authorization", "Bearer " + r.token()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("CARGA_INSUFICIENTE"));
    }

    @Test
    void entregaFueraDeRangoDevuelve422() throws Exception {
        Cli c = registrarCliente("f7c4@test.mx", "5557770004");
        Rep r = setupDueno("f7d4@test.mx", "5557770040", "Purif F7-4", "MarcaF74", "25.00", 100, 10);
        int[] ped = crearPedido(c.token(), r.idNegocio(), c.idDireccion(), r.idMarca(), 2);
        mvc.perform(post("/api/v1/pedidos/" + ped[0] + "/aceptacion").header("Authorization", "Bearer " + r.token()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/pedidos/" + ped[0] + "/en-camino").header("Authorization", "Bearer " + r.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lat\":%s,\"lon\":%s}".formatted(LAT, LON)))
                .andExpect(status().isOk());

        // Muy lejos del domicilio (~11 km).
        mvc.perform(post("/api/v1/pedidos/" + ped[0] + "/resultado").header("Authorization", "Bearer " + r.token())
                        .contentType(MediaType.APPLICATION_JSON).content(entrega(LAT + 0.1, LON, ped[1], 2)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("FUERA_DE_RANGO"));
    }

    @Test
    void entregaParcialRecalculaTotal() throws Exception {
        Cli c = registrarCliente("f7c5@test.mx", "5557770005");
        Rep r = setupDueno("f7d5@test.mx", "5557770050", "Purif F7-5", "MarcaF75", "25.00", 100, 10);
        int[] ped = crearPedido(c.token(), r.idNegocio(), c.idDireccion(), r.idMarca(), 4); // pide 4 (con envase propio)
        mvc.perform(post("/api/v1/pedidos/" + ped[0] + "/aceptacion").header("Authorization", "Bearer " + r.token()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/pedidos/" + ped[0] + "/en-camino").header("Authorization", "Bearer " + r.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lat\":%s,\"lon\":%s}".formatted(LAT, LON)))
                .andExpect(status().isOk());

        // Entrega solo 3 de 4 → total = 3 * 25 = 75 (con envase propio, sin cargo de envase).
        mvc.perform(post("/api/v1/pedidos/" + ped[0] + "/resultado").header("Authorization", "Bearer " + r.token())
                        .contentType(MediaType.APPLICATION_JSON).content(entrega(LAT, LON, ped[1], 3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("entregado"))
                .andExpect(jsonPath("$.totalPagar").value(75.00));

        // Vehículo: 10 - 3 entregados = 7 (el 4º reservado se liberó).
        mvc.perform(get("/api/v1/inventario/vehiculo").header("Authorization", "Bearer " + r.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ocupado").value(7));
    }

    @Test
    void dosNoEntregasConsecutivasSuspendenAlCliente() throws Exception {
        Cli c = registrarCliente("f7c6@test.mx", "5557770006");
        Rep r = setupDueno("f7d6@test.mx", "5557770060", "Purif F7-6", "MarcaF76", "25.00", 100, 30);

        for (int i = 0; i < 2; i++) {
            int[] ped = crearPedido(c.token(), r.idNegocio(), c.idDireccion(), r.idMarca(), 2);
            mvc.perform(post("/api/v1/pedidos/" + ped[0] + "/aceptacion").header("Authorization", "Bearer " + r.token()))
                    .andExpect(status().isOk());
            mvc.perform(post("/api/v1/pedidos/" + ped[0] + "/en-camino").header("Authorization", "Bearer " + r.token())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"lat\":%s,\"lon\":%s}".formatted(LAT, LON)))
                    .andExpect(status().isOk());
            mvc.perform(post("/api/v1/pedidos/" + ped[0] + "/resultado").header("Authorization", "Bearer " + r.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"resultado\":\"NO_ENTREGADO\",\"lat\":%s,\"lon\":%s,\"motivoNoEntrega\":\"cliente_ausente\"}".formatted(LAT, LON)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("no_entregado"));
        }

        // Tras 2 ausencias, el cliente está suspendido → no puede pedir (RN-006).
        String body = """
                {"tipoSolicitud":"directa","idNegocio":%d,"idDireccionEntrega":%d,
                 "detalles":[{"idMarca":%d,"cantidad":1,"tieneEnvase":true}]}"""
                .formatted(r.idNegocio(), c.idDireccion(), r.idMarca());
        mvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + c.token())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("CLIENTE_SUSPENDIDO"));
    }
}
