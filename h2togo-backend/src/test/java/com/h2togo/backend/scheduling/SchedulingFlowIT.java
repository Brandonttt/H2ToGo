package com.h2togo.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.h2togo.backend.catalogo.Marca;
import com.h2togo.backend.catalogo.MarcaRepository;
import com.h2togo.backend.catalogo.ProductoNegocio;
import com.h2togo.backend.catalogo.ProductoNegocioRepository;
import com.h2togo.backend.inventario.InventarioService;
import com.h2togo.backend.inventario.dto.LoteEntradaRequest;
import com.h2togo.backend.notificaciones.PushService;
import com.h2togo.backend.usuarios.RepartidorRepository;
import com.h2togo.backend.usuarios.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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

/** F11: tareas programadas (RF-025). Jobs invocados manualmente (scheduling off en test). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class SchedulingFlowIT {

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
    @Autowired private InventarioService inventarioService;
    @Autowired private NamedParameterJdbcTemplate jdbc;
    @Autowired private PedidosProgramadosJob pedidosProgramadosJob;
    @Autowired private SesionesExpiradasJob sesionesExpiradasJob;
    @Autowired private LotesPorCaducarJob lotesPorCaducarJob;
    @MockitoBean private PushService pushService;

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

    @Test
    void pedidoProgramadoSeActivaUnaSolaVez() throws Exception {
        // Cliente con dirección.
        String cli = "sc_cli@test.mx";
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"C","apellidos":"E","correo":"%s","password":"password123",
                         "telefono":"5552220101","rol":"cliente"}""".formatted(cli))).andExpect(status().isCreated());
        String tokenCli = verificarYLoguear(cli);
        String cuerpoDir = mvc.perform(post("/api/v1/clientes/me/direcciones").header("Authorization", "Bearer " + tokenCli)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"alias":"Casa","calle":"C","numeroExterior":"1","colonia":"Del Valle",
                                 "codigoPostal":"03100","referencias":"r","lat":19.372,"lon":-99.178}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int idDir = JsonPath.read(cuerpoDir, "$.id");

        // Dueño con producto y horario abierto.
        String due = "sc_due@test.mx";
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"D","apellidos":"U","correo":"%s","password":"password123",
                         "telefono":"5552220102","rol":"repartidor","negocio":{"nombreComercial":"Prog"}}""".formatted(due)))
                .andExpect(status().isCreated());
        String tokenDue = verificarYLoguear(due);
        int idRep = usuarioRepository.findByCorreo(due).orElseThrow().getId();
        int idNegocio = repartidorRepository.findById(idRep).orElseThrow().getIdNegocio();
        int idMarca = seedProductoYHorario(tokenDue, idNegocio);

        // Pedido programado (fecha futura al crear).
        String cuerpoPed = mvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + tokenCli)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoSolicitud":"directa","idNegocio":%d,"idDireccionEntrega":%d,
                                 "programado":{"fechaProgramada":"%s"},
                                 "detalles":[{"idMarca":%d,"cantidad":1,"tieneEnvase":true}]}"""
                                .formatted(idNegocio, idDir, OffsetDateTime.now().plusDays(1), idMarca)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int idPedido = JsonPath.read(cuerpoPed, "$.id");
        // Adelantamos la fecha programada al pasado para que el job lo active.
        jdbc.update("UPDATE pedidos SET fecha_programada = now() - interval '1 minute' WHERE id_pedido = :id",
                new MapSqlParameterSource("id", idPedido));

        pedidosProgramadosJob.activar();
        pedidosProgramadosJob.activar(); // segunda pasada: no debe reactivar ni re-notificar

        Boolean activado = jdbc.queryForObject(
                "SELECT estado_actual = 'pendiente' AND notificado_programado FROM pedidos WHERE id_pedido = :id",
                new MapSqlParameterSource("id", idPedido), Boolean.class);
        assertThat(activado).isTrue();
        // El repartidor del negocio recibe exactamente un aviso.
        verify(pushService, times(1)).notificar(eq(idRep), eq("Nuevo pedido disponible"), anyString());
    }

    @Test
    void sesionExpiradaSeLimpia() throws Exception {
        String correo = "sc_ses@test.mx";
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"S","apellidos":"E","correo":"%s","password":"password123",
                         "telefono":"5552220103","rol":"cliente"}""".formatted(correo))).andExpect(status().isCreated());
        verificarYLoguear(correo);
        assertThat(usuarioRepository.findByCorreo(correo).orElseThrow().getTokenSesion()).isNotNull();

        jdbc.update("UPDATE usuarios SET sesion_fecha_expiracion = now() - interval '1 day' WHERE correo = :c",
                new MapSqlParameterSource("c", correo));
        sesionesExpiradasJob.limpiar();

        assertThat(usuarioRepository.findByCorreo(correo).orElseThrow().getTokenSesion()).isNull();
    }

    @Test
    void loteVencidoSeDaDeBajaYPorCaducarAvisa() throws Exception {
        String due = "sc_lot@test.mx";
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"D","apellidos":"U","correo":"%s","password":"password123",
                         "telefono":"5552220104","rol":"repartidor","negocio":{"nombreComercial":"Lotes"}}""".formatted(due)))
                .andExpect(status().isCreated());
        verificarYLoguear(due);
        int idRep = usuarioRepository.findByCorreo(due).orElseThrow().getId();
        int idNegocio = repartidorRepository.findById(idRep).orElseThrow().getIdNegocio();
        Marca marca = new Marca();
        marca.setNombre("MarcaLote");
        int idMarca = marcaRepository.save(marca).getId();
        ProductoNegocio pn = new ProductoNegocio();
        pn.setIdNegocio(idNegocio);
        pn.setIdMarca(idMarca);
        pn.setPrecio(new BigDecimal("25.00"));
        pn.setPrecioEnvase(new BigDecimal("30.00"));
        pn.setCapacidadMaxima(1000);
        productoRepository.save(pn);

        // Dos lotes (RN-030 exige > hoy+7 al registrar); luego adelantamos su caducidad.
        var lote1 = inventarioService.registrarLote(idRep,
                new LoteEntradaRequest(idMarca, 10, LocalDate.now().plusMonths(3), null, null, null));
        var lote2 = inventarioService.registrarLote(idRep,
                new LoteEntradaRequest(idMarca, 5, LocalDate.now().plusMonths(3), null, null, null));
        jdbc.update("UPDATE lotes_inventario SET fecha_caducidad = CURRENT_DATE - 1 WHERE id_lote = :id",
                new MapSqlParameterSource("id", lote1.idLote())); // vencido
        jdbc.update("UPDATE lotes_inventario SET fecha_caducidad = CURRENT_DATE + 3 WHERE id_lote = :id",
                new MapSqlParameterSource("id", lote2.idLote())); // por caducar

        lotesPorCaducarJob.procesar();

        // Lote vencido: inactivo, en cero, con movimiento de merma.
        Boolean baja = jdbc.queryForObject(
                "SELECT NOT activo AND cantidad_actual = 0 FROM lotes_inventario WHERE id_lote = :id",
                new MapSqlParameterSource("id", lote1.idLote()), Boolean.class);
        assertThat(baja).isTrue();
        Integer mermas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM movimientos_inventario WHERE id_lote_base = :id AND tipo = 'salida_manual'",
                new MapSqlParameterSource("id", lote1.idLote()), Integer.class);
        assertThat(mermas).isEqualTo(1);
        // El dueño recibe aviso por el lote próximo a caducar.
        verify(pushService, atLeastOnce()).notificar(eq(idRep), eq("Lotes por caducar"), anyString());
    }

    private int seedProductoYHorario(String tokenDueno, int idNegocio) throws Exception {
        Marca marca = new Marca();
        marca.setNombre("MarcaProg-" + idNegocio);
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
