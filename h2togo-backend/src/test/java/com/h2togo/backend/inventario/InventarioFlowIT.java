package com.h2togo.backend.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.h2togo.backend.catalogo.Marca;
import com.h2togo.backend.catalogo.MarcaRepository;
import com.h2togo.backend.catalogo.ProductoNegocio;
import com.h2togo.backend.catalogo.ProductoNegocioRepository;
import com.h2togo.backend.common.BusinessRuleException;
import com.h2togo.backend.common.enums.TipoVehiculo;
import com.h2togo.backend.inventario.dto.CargaItem;
import com.h2togo.backend.inventario.dto.LoteEntradaRequest;
import com.h2togo.backend.repartidores.dto.IniciarJornadaRequest;
import com.h2togo.backend.usuarios.RepartidorRepository;
import com.h2togo.backend.usuarios.UsuarioRepository;
import com.h2togo.backend.vehiculos.VehiculoNegocio;
import com.h2togo.backend.vehiculos.VehiculoNegocioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
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
 * F5: inventario y jornada (CU-018/019/008). RN-030 (caducidad), FIFO por caducidad,
 * capacidad del vehículo (RN-008), jornada/devolución y CONCURRENCIA (FOR UPDATE) de dos
 * cargas sobre el mismo lote. Se salta sin Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class InventarioFlowIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"))
            .withCopyFileToContainer(
                    MountableFile.forHostPath("../H2ToGo_v6_postgresql.sql"),
                    "/docker-entrypoint-initdb.d/01_schema.sql");

    @Autowired private MockMvc mvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RepartidorRepository repartidorRepository;
    @Autowired private MarcaRepository marcaRepository;
    @Autowired private ProductoNegocioRepository productoRepository;
    @Autowired private VehiculoNegocioRepository vehiculoRepository;
    @Autowired private InventarioService inventarioService;
    @Autowired private NamedParameterJdbcTemplate jdbc;

    private record Setup(String token, int idRepartidor, int idNegocio, int idMarca, int idVehiculo) {
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

    private int seedVehiculo(int idNegocio, int capacidad) {
        VehiculoNegocio v = new VehiculoNegocio();
        v.setIdNegocio(idNegocio);
        v.setTipoVehiculo(TipoVehiculo.motocicleta);
        v.setMarca("Honda");
        v.setColor("Rojo");
        v.setCapacidadGarrafones(capacidad);
        return vehiculoRepository.save(v).getId();
    }

    private Setup setupDueno(String correo, String tel, String negocio, String marcaNombre, int capVehiculo)
            throws Exception {
        String reg = """
                {"nombre":"Due","apellidos":"Ño","correo":"%s","password":"password123",
                 "telefono":"%s","rol":"repartidor","negocio":{"nombreComercial":"%s"}}"""
                .formatted(correo, tel, negocio);
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(reg))
                .andExpect(status().isCreated());
        String token = verificarYLoguear(correo);
        int idRepartidor = usuarioRepository.findByCorreo(correo).orElseThrow().getId();
        int idNegocio = repartidorRepository.findById(idRepartidor).orElseThrow().getIdNegocio();

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

        int idVehiculo = seedVehiculo(idNegocio, capVehiculo);
        return new Setup(token, idRepartidor, idNegocio, idMarca, idVehiculo);
    }

    private void registrarLote(int idRepartidor, int idMarca, int cantidad, int diasCaducidad) {
        inventarioService.registrarLote(idRepartidor,
                new LoteEntradaRequest(idMarca, cantidad, LocalDate.now().plusDays(diasCaducidad),
                        null, "Proveedor", null));
    }

    @Test
    void registrarLoteRespetaCaducidadRn030() throws Exception {
        Setup s = setupDueno("inv1@test.mx", "5554440001", "Purif Inv1", "MarcaInv1", 50);

        // Caducidad a 5 días → 422 RN-030.
        mvc.perform(post("/api/v1/inventario/lotes").header("Authorization", "Bearer " + s.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idMarca":%d,"cantidad":50,"fechaCaducidad":"%s"}"""
                                .formatted(s.idMarca(), LocalDate.now().plusDays(5))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("RN-030_CADUCIDAD_INVALIDA"));

        // Caducidad a 30 días → 201.
        mvc.perform(post("/api/v1/inventario/lotes").header("Authorization", "Bearer " + s.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idMarca":%d,"cantidad":50,"fechaCaducidad":"%s"}"""
                                .formatted(s.idMarca(), LocalDate.now().plusDays(30))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidadActual").value(50));

        mvc.perform(get("/api/v1/inventario/base").header("Authorization", "Bearer " + s.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lotes[0].cantidadActual").value(50));
    }

    @Test
    void cargaFifoConsumeCaducidadMasProximaPrimero() throws Exception {
        Setup s = setupDueno("inv2@test.mx", "5554440002", "Purif Inv2", "MarcaInv2", 50);
        registrarLote(s.idRepartidor(), s.idMarca(), 5, 10);  // loteA caduca antes
        registrarLote(s.idRepartidor(), s.idMarca(), 5, 30);  // loteB caduca después

        // Inicia jornada cargando 6 → debe tomar los 5 de A y 1 de B (FIFO).
        mvc.perform(post("/api/v1/repartidores/me/jornada").header("Authorization", "Bearer " + s.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idVehiculo":%d,"cargaInicial":[{"idMarca":%d,"cantidad":6}]}"""
                                .formatted(s.idVehiculo(), s.idMarca())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoOperativo").value(true))
                .andExpect(jsonPath("$.inventarioVehiculo.ocupado").value(6));

        // Vehículo: lote más próximo (A) cargado completo primero.
        mvc.perform(get("/api/v1/inventario/vehiculo").header("Authorization", "Bearer " + s.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ocupado").value(6))
                .andExpect(jsonPath("$.lotes[0].cantidadActual").value(5));

        // Base: A quedó en 0 (consumido primero), B en 4.
        mvc.perform(get("/api/v1/inventario/base").header("Authorization", "Bearer " + s.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lotes[0].cantidadActual").value(0))
                .andExpect(jsonPath("$.lotes[1].cantidadActual").value(4));
    }

    @Test
    void cargaQueExcedeCapacidadDevuelve422() throws Exception {
        Setup s = setupDueno("inv3@test.mx", "5554440003", "Purif Inv3", "MarcaInv3", 5); // capacidad 5
        registrarLote(s.idRepartidor(), s.idMarca(), 100, 30);

        // Inicia jornada sin carga (solo selecciona vehículo).
        mvc.perform(post("/api/v1/repartidores/me/jornada").header("Authorization", "Bearer " + s.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idVehiculo\":%d,\"cargaInicial\":[]}".formatted(s.idVehiculo())))
                .andExpect(status().isOk());

        // Cargar 10 con capacidad 5 → 422.
        mvc.perform(post("/api/v1/inventario/carga-vehiculo").header("Authorization", "Bearer " + s.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cargas\":[{\"idMarca\":%d,\"cantidad\":10}]}".formatted(s.idMarca())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("CAPACIDAD_VEHICULO_EXCEDIDA"));
    }

    @Test
    void jornadaYDevolucionRestauranLaBase() throws Exception {
        Setup s = setupDueno("inv4@test.mx", "5554440004", "Purif Inv4", "MarcaInv4", 50);
        registrarLote(s.idRepartidor(), s.idMarca(), 20, 30);

        mvc.perform(post("/api/v1/repartidores/me/jornada").header("Authorization", "Bearer " + s.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idVehiculo":%d,"cargaInicial":[{"idMarca":%d,"cantidad":8}]}"""
                                .formatted(s.idVehiculo(), s.idMarca())))
                .andExpect(status().isOk());

        // Devolución cierra jornada y regresa todo a la base.
        mvc.perform(post("/api/v1/inventario/devolucion").header("Authorization", "Bearer " + s.token()))
                .andExpect(status().isNoContent());

        var r = repartidorRepository.findById(s.idRepartidor()).orElseThrow();
        assertThat(r.isEstadoOperativo()).isFalse();
        assertThat(r.getIdVehiculoActual()).isNull();

        mvc.perform(get("/api/v1/inventario/base").header("Authorization", "Bearer " + s.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lotes[0].cantidadActual").value(20)); // restaurado
    }

    @Test
    void dosCargasConcurrentesSobreElMismoLote() throws Exception {
        // Dueño A + su vehículo; miembro B (mismo negocio) + su vehículo.
        Setup a = setupDueno("invconc@test.mx", "5554440005", "Purif Conc", "MarcaConc", 50);
        var loteId = new AtomicInteger();
        // Un solo lote de 10 en la base.
        var lote = inventarioService.registrarLote(a.idRepartidor(),
                new LoteEntradaRequest(a.idMarca(), 10, LocalDate.now().plusDays(30), null, null, null));
        loteId.set(lote.idLote());

        String regB = """
                {"nombre":"Mie","apellidos":"Mbro","correo":"invb@test.mx","password":"password123",
                 "telefono":"5554440050","rol":"repartidor","negocio":{"idExistente":%d}}"""
                .formatted(a.idNegocio());
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(regB))
                .andExpect(status().isCreated());
        verificarYLoguear("invb@test.mx");
        int idB = usuarioRepository.findByCorreo("invb@test.mx").orElseThrow().getId();
        int vehiculoB = seedVehiculo(a.idNegocio(), 50);

        // Ambos inician jornada (seleccionan su vehículo).
        inventarioService.iniciarJornada(a.idRepartidor(), new IniciarJornadaRequest(a.idVehiculo(), List.of()));
        inventarioService.iniciarJornada(idB, new IniciarJornadaRequest(vehiculoB, List.of()));

        // Dos cargas simultáneas de 6 sobre el único lote de 10 → solo una puede ganar.
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch arranque = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Callable<Boolean> tareaA = () -> intentarCarga(a.idRepartidor(), a.idMarca(), listos, arranque);
        Callable<Boolean> tareaB = () -> intentarCarga(idB, a.idMarca(), listos, arranque);

        Future<Boolean> fA = pool.submit(tareaA);
        Future<Boolean> fB = pool.submit(tareaB);
        listos.await();
        arranque.countDown(); // arrancan casi a la vez

        int exitos = 0;
        try {
            if (Boolean.TRUE.equals(fA.get())) exitos++;
        } catch (Exception ignored) { }
        try {
            if (Boolean.TRUE.equals(fB.get())) exitos++;
        } catch (Exception ignored) { }
        pool.shutdown();

        // Exactamente una carga tuvo éxito; el lote quedó en 10 - 6 = 4.
        assertThat(exitos).isEqualTo(1);
        Integer restante = jdbc.queryForObject(
                "SELECT cantidad_actual FROM lotes_inventario WHERE id_lote = :id",
                new MapSqlParameterSource("id", loteId.get()), Integer.class);
        assertThat(restante).isEqualTo(4);
    }

    /** Carga 6; devuelve true si tuvo éxito, false si falló por stock (BusinessRuleException). */
    private boolean intentarCarga(int idRepartidor, int idMarca, CountDownLatch listos, CountDownLatch arranque)
            throws InterruptedException {
        listos.countDown();
        arranque.await();
        try {
            inventarioService.cargarVehiculo(idRepartidor, List.of(new CargaItem(idMarca, 6)));
            return true;
        } catch (BusinessRuleException e) {
            return false; // STOCK_INSUFICIENTE
        }
    }
}
