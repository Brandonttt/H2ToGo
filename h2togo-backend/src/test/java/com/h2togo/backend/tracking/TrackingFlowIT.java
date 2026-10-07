package com.h2togo.backend.tracking;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

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
import com.h2togo.backend.usuarios.RepartidorRepository;
import com.h2togo.backend.usuarios.UsuarioRepository;
import com.h2togo.backend.vehiculos.VehiculoNegocio;
import com.h2togo.backend.vehiculos.VehiculoNegocioRepository;
import com.jayway.jsonpath.JsonPath;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * F8: rastreo y proximidad (CU-006, RF-005). Verifica el aviso de proximidad (≤500 m) por el
 * fallback REST y el relay de ubicación por el canal WebSocket STOMP.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class TrackingFlowIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"))
            .withCopyFileToContainer(MountableFile.forHostPath("../H2ToGo_v6_postgresql.sql"),
                    "/docker-entrypoint-initdb.d/01_schema.sql")
            .withCopyFileToContainer(MountableFile.forHostPath("sql/zona_benito_juarez.sql"),
                    "/docker-entrypoint-initdb.d/02_zona.sql");

    @Autowired private MockMvc mvc;
    @Autowired private org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate jdbc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RepartidorRepository repartidorRepository;
    @Autowired private MarcaRepository marcaRepository;
    @Autowired private ProductoNegocioRepository productoRepository;
    @Autowired private VehiculoNegocioRepository vehiculoRepository;
    @Autowired private InventarioService inventarioService;
    @MockitoBean private PushService pushService;
    @LocalServerPort private int port;

    private static final double LAT = 19.372, LON = -99.178;

    /** Deja un pedido en camino, asignado al repartidor, con domicilio en LAT/LON. Devuelve [idPedido, idCliente, idRep, tokenRep, tokenCli]. */
    private Object[] escenarioEnCamino(String sufijo) throws Exception {
        String correoCli = "trkc" + sufijo + "@test.mx";
        String cli = registro(correoCli, "55580" + sufijo, "cliente", null);
        String tokenCli = verificarYLoguear(correoCli);
        int idCliente = usuarioRepository.findByCorreo(correoCli).orElseThrow().getId();
        String dir = """
                {"alias":"Casa","calle":"C","numeroExterior":"1","colonia":"Del Valle",
                 "codigoPostal":"03100","referencias":"r","lat":%s,"lon":%s}""".formatted(LAT, LON);
        String cuerpoDir = mvc.perform(post("/api/v1/clientes/me/direcciones").header("Authorization", "Bearer " + tokenCli)
                        .contentType(MediaType.APPLICATION_JSON).content(dir))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int idDireccion = JsonPath.read(cuerpoDir, "$.id");

        String correoDue = "trkd" + sufijo + "@test.mx";
        registro(correoDue, "55581" + sufijo, "repartidor", "Purif Trk" + sufijo);
        String tokenRep = verificarYLoguear(correoDue);
        int idRep = usuarioRepository.findByCorreo(correoDue).orElseThrow().getId();
        int idNegocio = repartidorRepository.findById(idRep).orElseThrow().getIdNegocio();

        Marca marca = new Marca();
        marca.setNombre("MarcaTrk" + sufijo);
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
        horarioAbierto(tokenRep);
        inventarioService.registrarLote(idRep,
                new LoteEntradaRequest(idMarca, 50, LocalDate.now().plusMonths(3), null, null, null));
        inventarioService.iniciarJornada(idRep, new IniciarJornadaRequest(idVehiculo, List.of(new CargaItem(idMarca, 20))));

        String body = """
                {"tipoSolicitud":"directa","idNegocio":%d,"idDireccionEntrega":%d,
                 "detalles":[{"idMarca":%d,"cantidad":2,"tieneEnvase":true}]}"""
                .formatted(idNegocio, idDireccion, idMarca);
        String cuerpoPed = mvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + tokenCli)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int idPedido = JsonPath.read(cuerpoPed, "$.id");

        mvc.perform(post("/api/v1/pedidos/" + idPedido + "/aceptacion").header("Authorization", "Bearer " + tokenRep))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/pedidos/" + idPedido + "/en-camino").header("Authorization", "Bearer " + tokenRep)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lat\":%s,\"lon\":%s}".formatted(LAT + 0.05, LON)))
                .andExpect(status().isOk());
        return new Object[]{idPedido, idCliente, idRep, tokenRep, tokenCli};
    }

    @Test
    void avisoDeProximidadPorFallbackRest() throws Exception {
        Object[] esc = escenarioEnCamino("1");
        int idCliente = (int) esc[1];
        String tokenRep = (String) esc[3];

        // Lejos (~5.5 km): sin aviso de proximidad.
        mvc.perform(put("/api/v1/repartidores/me/ubicacion").header("Authorization", "Bearer " + tokenRep)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lat\":%s,\"lon\":%s}".formatted(LAT + 0.05, LON)))
                .andExpect(status().isNoContent());
        verify(pushService, never()).notificar(eq(idCliente), eq("Tu pedido está cerca"), anyString(), anyMap());

        // Cerca (en el domicilio): aviso una sola vez.
        mvc.perform(put("/api/v1/repartidores/me/ubicacion").header("Authorization", "Bearer " + tokenRep)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lat\":%s,\"lon\":%s}".formatted(LAT, LON)))
                .andExpect(status().isNoContent());
        verify(pushService, times(1)).notificar(eq(idCliente), eq("Tu pedido está cerca"), anyString(), anyMap());
    }

    @Test
    void relayDeUbicacionPorWebSocketStomp() throws Exception {
        Object[] esc = escenarioEnCamino("2");
        int idPedido = (int) esc[0];
        String tokenRep = (String) esc[3];

        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter());
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("token", tokenRep);

        StompSession session = client.connectAsync("ws://localhost:" + port + "/ws",
                new org.springframework.web.socket.WebSocketHttpHeaders(), connectHeaders,
                new StompSessionHandlerAdapter() { }).get(5, TimeUnit.SECONDS);

        CompletableFuture<Map<String, Object>> recibido = new CompletableFuture<>();
        session.subscribe("/topic/pedidos/" + idPedido + "/ubicacion", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                recibido.complete((Map<String, Object>) payload);
            }
        });

        session.send("/app/pedidos/" + idPedido + "/ubicacion", Map.of("lat", LAT, "lon", LON));

        Map<String, Object> msg = recibido.get(5, TimeUnit.SECONDS);
        assertThat(((Number) msg.get("lat")).doubleValue()).isEqualTo(LAT);
        session.disconnect();
    }

    @Test
    void reporteRestSeReenviaAlClienteYConsultaDeUltimaUbicacion() throws Exception {
        Object[] esc = escenarioEnCamino("3");
        int idPedido = (int) esc[0];
        String tokenRep = (String) esc[3];
        String tokenCli = (String) esc[4];

        StompSession sesionCli = conectar(tokenCli);
        CompletableFuture<Map<String, Object>> recibido = suscribir(sesionCli, idPedido);
        Thread.sleep(300); // da tiempo a que el SUBSCRIBE llegue antes de publicar

        // La app del repartidor reporta por REST; el cliente lo recibe por el WebSocket.
        mvc.perform(put("/api/v1/repartidores/me/ubicacion").header("Authorization", "Bearer " + tokenRep)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lat\":%s,\"lon\":%s}".formatted(LAT + 0.01, LON)))
                .andExpect(status().isNoContent());
        assertThat(((Number) recibido.get(5, TimeUnit.SECONDS).get("lat")).doubleValue()).isEqualTo(LAT + 0.01);
        sesionCli.disconnect();

        mvc.perform(get("/api/v1/pedidos/" + idPedido + "/ubicacion-repartidor").header("Authorization", "Bearer " + tokenCli))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lat").value(LAT + 0.01));
        // Otro cliente no puede consultarla.
        Object[] otro = escenarioEnCamino("4");
        mvc.perform(get("/api/v1/pedidos/" + idPedido + "/ubicacion-repartidor").header("Authorization", "Bearer " + otro[4]))
                .andExpect(status().isForbidden());
    }

    @Test
    void ajenosNoPuedenVerNiPublicarLaUbicacion() throws Exception {
        Object[] esc = escenarioEnCamino("5");
        int idPedido = (int) esc[0];
        String tokenRep = (String) esc[3];
        String tokenCli = (String) esc[4];
        Object[] otro = escenarioEnCamino("6");

        // Un cliente ajeno se suscribe: el servidor rechaza el SUBSCRIBE y no recibe nada.
        StompSession sesionAjena = conectar((String) otro[4]);
        CompletableFuture<Map<String, Object>> espia = suscribir(sesionAjena, idPedido);
        // El cliente dueño intenta publicar una ubicación falsa: se ignora.
        StompSession sesionCli = conectar(tokenCli);
        CompletableFuture<Map<String, Object>> legitimo = suscribir(sesionCli, idPedido);
        Thread.sleep(300);
        sesionCli.send("/app/pedidos/" + idPedido + "/ubicacion", Map.of("lat", 1.0, "lon", 1.0));
        Thread.sleep(500);
        assertThat(legitimo).isNotDone();

        // El repartidor asignado sí publica y solo el cliente dueño lo recibe.
        mvc.perform(put("/api/v1/repartidores/me/ubicacion").header("Authorization", "Bearer " + tokenRep)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"lat\":%s,\"lon\":%s}".formatted(LAT, LON)))
                .andExpect(status().isNoContent());
        assertThat(((Number) legitimo.get(5, TimeUnit.SECONDS).get("lat")).doubleValue()).isEqualTo(LAT);
        Thread.sleep(300);
        assertThat(espia).isNotDone();
    }

    @Test
    void clienteVeDatosRealesDelRepartidorEnElSeguimiento() throws Exception {
        Object[] esc = escenarioEnCamino("7");
        int idPedido = (int) esc[0];
        String tokenCli = (String) esc[4];

        // Escenario: repartidor "N A" del negocio "Purif Trk7" en motocicleta Honda roja.
        mvc.perform(get("/api/v1/pedidos/" + idPedido).header("Authorization", "Bearer " + tokenCli))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repartidor.nombre").value("N A"))
                .andExpect(jsonPath("$.repartidor.negocio").value("Purif Trk7"))
                .andExpect(jsonPath("$.repartidor.vehiculo").value("Motocicleta Honda Rojo"))
                .andExpect(jsonPath("$.repartidor.telefono").value("555817"))
                .andExpect(jsonPath("$.nombreNegocio").value("Purif Trk7"));

        // Cerrado el pedido, el teléfono del repartidor ya no se comparte.
        jdbc.update("UPDATE pedidos SET estado_actual = 'entregado'::estado_pedido WHERE id_pedido = :id",
                new MapSqlParameterSource("id", idPedido));
        mvc.perform(get("/api/v1/pedidos/" + idPedido).header("Authorization", "Bearer " + tokenCli))
                .andExpect(jsonPath("$.repartidor.nombre").value("N A"))
                .andExpect(jsonPath("$.repartidor.telefono").doesNotExist());
    }

    private StompSession conectar(String token) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter());
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("token", token);
        return client.connectAsync("ws://localhost:" + port + "/ws",
                new org.springframework.web.socket.WebSocketHttpHeaders(), connectHeaders,
                new StompSessionHandlerAdapter() { }).get(5, TimeUnit.SECONDS);
    }

    private CompletableFuture<Map<String, Object>> suscribir(StompSession sesion, int idPedido) {
        CompletableFuture<Map<String, Object>> recibido = new CompletableFuture<>();
        sesion.subscribe("/topic/pedidos/" + idPedido + "/ubicacion", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                recibido.complete((Map<String, Object>) payload);
            }
        });
        return recibido;
    }

    // ---- helpers de registro/login ----

    private String registro(String correo, String tel, String rol, String negocio) throws Exception {
        String neg = negocio == null ? "" : ",\"negocio\":{\"nombreComercial\":\"%s\"}".formatted(negocio);
        String reg = """
                {"nombre":"N","apellidos":"A","correo":"%s","password":"password123",
                 "telefono":"%s","rol":"%s"%s}""".formatted(correo, tel, rol, neg);
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(reg))
                .andExpect(status().isCreated());
        return correo;
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

    private void horarioAbierto(String tokenDueno) throws Exception {
        StringBuilder sb = new StringBuilder("[");
        for (int d = 1; d <= 7; d++) {
            if (d > 1) sb.append(",");
            sb.append("{\"diaSemana\":%d,\"cerrado\":false,\"horaApertura\":\"00:00:00\",\"horaCierre\":\"23:59:59\"}".formatted(d));
        }
        sb.append("]");
        mvc.perform(put("/api/v1/negocios/me/horarios").header("Authorization", "Bearer " + tokenDueno)
                .contentType(MediaType.APPLICATION_JSON).content(sb.toString())).andExpect(status().isOk());
    }
}
