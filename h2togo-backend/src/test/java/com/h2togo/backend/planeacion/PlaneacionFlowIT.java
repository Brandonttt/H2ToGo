package com.h2togo.backend.planeacion;

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

import com.h2togo.backend.catalogo.Marca;
import com.h2togo.backend.catalogo.MarcaRepository;
import com.h2togo.backend.catalogo.ProductoNegocio;
import com.h2togo.backend.catalogo.ProductoNegocioRepository;
import com.h2togo.backend.common.enums.TipoVehiculo;
import com.h2togo.backend.inventario.InventarioService;
import com.h2togo.backend.inventario.dto.CargaItem;
import com.h2togo.backend.inventario.dto.LoteEntradaRequest;
import com.h2togo.backend.notificaciones.PushService;
import com.h2togo.backend.repartidores.dto.IniciarJornadaRequest;
import com.h2togo.backend.scheduling.PedidosProgramadosJob;
import com.h2togo.backend.usuarios.RepartidorRepository;
import com.h2togo.backend.usuarios.UsuarioRepository;
import com.h2togo.backend.vehiculos.VehiculoNegocio;
import com.h2togo.backend.vehiculos.VehiculoNegocioRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Agenda de programados y ruta óptima del día: reserva con concurrencia, ruta con OR-Tools sobre
 * el grafo OSM, recálculo al cancelar (RN-009), capacidad del vehículo, aceptación anticipada del
 * reservado, activación sin ofrecerlo a otros y liberación.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PlaneacionFlowIT {

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
    @Autowired private PedidosProgramadosJob pedidosProgramadosJob;
    @Autowired private NamedParameterJdbcTemplate jdbc;
    @MockitoBean private PushService pushService;

    /** Tres domicilios en la colonia Del Valle; el repartidor sale del primero. */
    private static final double[][] CASAS = {{19.3720, -99.1780}, {19.3800, -99.1700}, {19.3650, -99.1600}};

    private record Rep(String token, int idRep, int idNegocio, int idMarca, int idVehiculo) {
    }

    @Test
    void agendaReservaRutaOptimaYRecalculo() throws Exception {
        Rep a = setupDueno("pl-due@test.mx", "5558880010", "Purif Plan", "MarcaPlan");
        Rep b = addMiembro(a.idNegocio(), a.idMarca(), "pl-mie@test.mx", "5558880011");

        String[] clientes = new String[3];
        int[] pedidos = new int[3];
        for (int i = 0; i < 3; i++) {
            clientes[i] = registrarCliente("pl-c%d@test.mx".formatted(i), "555888002" + i, CASAS[i]);
            pedidos[i] = crearProgramado(clientes[i], a.idNegocio(), idDireccion(clientes[i]), a.idMarca());
        }

        // La agenda muestra los tres programados de hoy, todavía libres.
        mvc.perform(get("/api/v1/repartidores/me/agenda").header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].reservadoPorMi").value(false));

        // A reserva los tres; B ya no puede reservar ni ve los ajenos.
        for (int id : pedidos) {
            mvc.perform(post("/api/v1/pedidos/" + id + "/reserva").header("Authorization", "Bearer " + a.token()))
                    .andExpect(status().isNoContent());
        }
        mvc.perform(post("/api/v1/pedidos/" + pedidos[0] + "/reserva").header("Authorization", "Bearer " + b.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PEDIDO_YA_RESERVADO"));
        mvc.perform(get("/api/v1/repartidores/me/agenda").header("Authorization", "Bearer " + b.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        // Ruta óptima desde la primera casa: es la primera parada y se incluyen las tres.
        mvc.perform(put("/api/v1/repartidores/me/ubicacion").header("Authorization", "Bearer " + a.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lat\":%s,\"lon\":%s}".formatted(CASAS[0][0], CASAS[0][1])))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/repartidores/me/ruta-optima").header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paradas.length()").value(3))
                .andExpect(jsonPath("$.paradas[0].idPedido").value(pedidos[0]))
                .andExpect(jsonPath("$.paradas[0].orden").value(1))
                .andExpect(jsonPath("$.omitidos.length()").value(0))
                .andExpect(jsonPath("$.distanciaTotalKm").value(org.hamcrest.Matchers.greaterThan(0.5)))
                .andExpect(jsonPath("$.coordenadas.length()").value(org.hamcrest.Matchers.greaterThan(2)))
                .andExpect(jsonPath("$.algoritmo").value(org.hamcrest.Matchers.containsString("OR-Tools")));

        // RN-009: el cliente cancela un reservado → se avisa al repartidor y sale de la ruta.
        mvc.perform(post("/api/v1/pedidos/" + pedidos[1] + "/cancelacion").header("Authorization", "Bearer " + clientes[1])
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Ya no lo necesito\"}"))
                .andExpect(status().isOk());
        verify(pushService).notificar(eq(a.idRep()), eq("Pedido cancelado"), anyString(), anyMap());
        mvc.perform(get("/api/v1/repartidores/me/ruta-optima").header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paradas.length()").value(2))
                .andExpect(jsonPath("$.paradas[?(@.idPedido == %d)]".formatted(pedidos[1])).isEmpty());

        // Capacidad: 2 + 2 garrafones en un vehículo de 3 → uno queda fuera con su motivo.
        jdbc.update("UPDATE vehiculos_negocio SET capacidad_garrafones = 3 WHERE id_vehiculo = :v",
                new MapSqlParameterSource("v", a.idVehiculo()));
        mvc.perform(get("/api/v1/repartidores/me/ruta-optima").header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paradas.length()").value(1))
                .andExpect(jsonPath("$.omitidos[0].motivo").value(org.hamcrest.Matchers.containsString("No cabe")));
        jdbc.update("UPDATE vehiculos_negocio SET capacidad_garrafones = 50 WHERE id_vehiculo = :v",
                new MapSqlParameterSource("v", a.idVehiculo()));

        // A acepta antes de su hora el programado que reservó; B no puede aceptarlo.
        mvc.perform(post("/api/v1/pedidos/" + pedidos[2] + "/aceptacion").header("Authorization", "Bearer " + b.token()))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/pedidos/" + pedidos[2] + "/aceptacion").header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("asignado"));

        // Al llegar su hora, el reservado se activa sin ofrecerse a los demás.
        jdbc.update("UPDATE pedidos SET fecha_programada = now() - interval '1 minute' WHERE id_pedido = :id",
                new MapSqlParameterSource("id", pedidos[0]));
        pedidosProgramadosJob.activar();
        assertThat(estado(pedidos[0])).isEqualTo("pendiente");
        assertThat(disponibles(b.token())).doesNotContain(pedidos[0]);
        assertThat(disponibles(a.token())).contains(pedidos[0]);

        // Al liberarlo vuelve a estar disponible para todos.
        mvc.perform(delete("/api/v1/pedidos/" + pedidos[0] + "/reserva").header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isNoContent());
        assertThat(disponibles(b.token())).contains(pedidos[0]);
    }

    // ---------------------------------------------------------------- Apoyo

    private List<Integer> disponibles(String token) throws Exception {
        String cuerpo = mvc.perform(get("/api/v1/repartidores/me/pedidos-disponibles").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$[*].idPedido");
    }

    private String estado(int idPedido) {
        return jdbc.queryForObject("SELECT estado_actual::text FROM pedidos WHERE id_pedido = :id",
                new MapSqlParameterSource("id", idPedido), String.class);
    }

    private int idDireccion(String tokenCliente) throws Exception {
        String cuerpo = mvc.perform(get("/api/v1/clientes/me/direcciones").header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$[0].id");
    }

    /** Programado directo de 2 garrafones; la fecha se mueve a "ahora" para que sea de hoy. */
    private int crearProgramado(String tokenCliente, int idNegocio, int idDireccion, int idMarca) throws Exception {
        String cuerpo = mvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoSolicitud":"directa","idNegocio":%d,"idDireccionEntrega":%d,
                                 "programado":{"fechaProgramada":"%s"},
                                 "detalles":[{"idMarca":%d,"cantidad":2,"tieneEnvase":true}]}"""
                                .formatted(idNegocio, idDireccion, OffsetDateTime.now().plusDays(1), idMarca)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(cuerpo, "$.id");
        jdbc.update("UPDATE pedidos SET fecha_programada = now() WHERE id_pedido = :id",
                new MapSqlParameterSource("id", id));
        return id;
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

    private String registrarCliente(String correo, String tel, double[] casa) throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Cli","apellidos":"Ente","correo":"%s","password":"password123",
                         "telefono":"%s","rol":"cliente"}""".formatted(correo, tel)))
                .andExpect(status().isCreated());
        String token = verificarYLoguear(correo);
        mvc.perform(post("/api/v1/clientes/me/direcciones").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"alias":"Casa","calle":"C","numeroExterior":"1","colonia":"Del Valle",
                                 "codigoPostal":"03100","referencias":"r","lat":%s,"lon":%s}""".formatted(casa[0], casa[1])))
                .andExpect(status().isCreated());
        return token;
    }

    private int seedVehiculo(int idNegocio) {
        VehiculoNegocio v = new VehiculoNegocio();
        v.setIdNegocio(idNegocio);
        v.setTipoVehiculo(TipoVehiculo.motocicleta);
        v.setMarca("Honda");
        v.setColor("Rojo");
        v.setCapacidadGarrafones(50);
        return vehiculoRepository.save(v).getId();
    }

    private Rep setupDueno(String correo, String tel, String negocio, String marcaNombre) throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Due","apellidos":"Ño","correo":"%s","password":"password123",
                         "telefono":"%s","rol":"repartidor","negocio":{"nombreComercial":"%s"}}"""
                        .formatted(correo, tel, negocio)))
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
        pn.setPrecio(new BigDecimal("25.00"));
        pn.setPrecioEnvase(new BigDecimal("30.00"));
        pn.setCapacidadMaxima(1000);
        productoRepository.save(pn);
        int idVehiculo = seedVehiculo(idNegocio);

        StringBuilder horario = new StringBuilder("[");
        for (int d = 1; d <= 7; d++) {
            horario.append(d > 1 ? "," : "").append(
                    "{\"diaSemana\":%d,\"cerrado\":false,\"horaApertura\":\"00:00:00\",\"horaCierre\":\"23:59:59\"}".formatted(d));
        }
        mvc.perform(put("/api/v1/negocios/me/horarios").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(horario.append("]").toString()))
                .andExpect(status().isOk());
        inventarioService.registrarLote(idRep,
                new LoteEntradaRequest(idMarca, 100, LocalDate.now().plusMonths(3), null, null, null));
        inventarioService.iniciarJornada(idRep,
                new IniciarJornadaRequest(idVehiculo, List.of(new CargaItem(idMarca, 20))));
        return new Rep(token, idRep, idNegocio, idMarca, idVehiculo);
    }

    private Rep addMiembro(int idNegocio, int idMarca, String correo, String tel) throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Mie","apellidos":"Mbro","correo":"%s","password":"password123",
                         "telefono":"%s","rol":"repartidor","negocio":{"idExistente":%d}}"""
                        .formatted(correo, tel, idNegocio)))
                .andExpect(status().isCreated());
        String token = verificarYLoguear(correo);
        int idRep = usuarioRepository.findByCorreo(correo).orElseThrow().getId();
        int idVehiculo = seedVehiculo(idNegocio);
        inventarioService.iniciarJornada(idRep,
                new IniciarJornadaRequest(idVehiculo, List.of(new CargaItem(idMarca, 20))));
        return new Rep(token, idRep, idNegocio, idMarca, idVehiculo);
    }
}
