package com.h2togo.backend.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.h2togo.backend.bd.TareaSistema;
import com.h2togo.backend.common.BusinessRuleException;
import com.h2togo.backend.common.enums.RolUsuario;
import com.h2togo.backend.negocios.Negocio;
import com.h2togo.backend.negocios.NegocioRepository;
import com.h2togo.backend.notificaciones.PushService;
import com.h2togo.backend.usuarios.dto.EliminarCuentaRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

class UsuarioServiceTest {

    private UsuarioRepository usuarioRepo;
    private ClienteRepository clienteRepo;
    private RepartidorRepository repartidorRepo;
    private NegocioRepository negocioRepo;
    private PasswordEncoder passwordEncoder;
    private NamedParameterJdbcTemplate jdbc;
    private TareaSistema tareaSistema;
    private PushService pushService;
    private UsuarioService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        usuarioRepo = mock(UsuarioRepository.class);
        clienteRepo = mock(ClienteRepository.class);
        repartidorRepo = mock(RepartidorRepository.class);
        negocioRepo = mock(NegocioRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jdbc = mock(NamedParameterJdbcTemplate.class);
        tareaSistema = mock(TareaSistema.class);
        pushService = mock(PushService.class);

        when(tareaSistema.consultar(any(Supplier.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());

        service = new UsuarioService(usuarioRepo, clienteRepo, repartidorRepo, passwordEncoder,
                negocioRepo, jdbc, tareaSistema, pushService);
    }

    private Usuario crearUsuario(int id, RolUsuario rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre("Ana");
        u.setApellidos("García");
        u.setCorreo("ana@test.mx");
        u.setTelefono("5550000001");
        u.setPasswordHash("HASH_VALIDO");
        u.setRol(rol);
        u.setTelefonoVerificado(true);
        u.setCuentaActiva(true);
        u.setTokenSesionHash("a".repeat(64));
        u.setTokenFcm("fcm-token-123");
        u.setSesionFechaCreacion(OffsetDateTime.now().minusDays(1));
        u.setSesionFechaExpiracion(OffsetDateTime.now().plusDays(29));
        return u;
    }

    @Test
    void eliminarCuenta_contrasenaIncorrecta_lanza422() {
        Usuario u = crearUsuario(10, RolUsuario.cliente);
        when(usuarioRepo.findById(10)).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("incorrecta", "HASH_VALIDO")).thenReturn(false);

        assertThatThrownBy(() -> service.eliminarCuenta(10, new EliminarCuentaRequest("incorrecta")))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("codigo")
                .isEqualTo("CONTRASENA_ACTUAL_INCORRECTA");

        verify(usuarioRepo, never()).save(any());
    }

    @Test
    void eliminarCuenta_adminNoPermitido() {
        Usuario u = crearUsuario(1, RolUsuario.admin);
        when(usuarioRepo.findById(1)).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.eliminarCuenta(1, new EliminarCuentaRequest("secreta123")))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("codigo")
                .isEqualTo("OPERACION_NO_PERMITIDA");

        verify(usuarioRepo, never()).save(any());
    }

    @Test
    void eliminarCuenta_clienteConPedidoEnCurso_bloquea() {
        Usuario u = crearUsuario(10, RolUsuario.cliente);
        when(usuarioRepo.findById(10)).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("secreta123", "HASH_VALIDO")).thenReturn(true);
        when(jdbc.queryForObject(contains("WHERE id_cliente = :id"), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(1);

        assertThatThrownBy(() -> service.eliminarCuenta(10, new EliminarCuentaRequest("secreta123")))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("codigo")
                .isEqualTo("PEDIDOS_EN_CURSO");

        verify(usuarioRepo, never()).save(any());
    }

    @Test
    void eliminarCuenta_clienteExitoso_cancelaPendientesAnonimizaYCierraSesion() {
        Usuario u = crearUsuario(10, RolUsuario.cliente);
        when(usuarioRepo.findById(10)).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("secreta123", "HASH_VALIDO")).thenReturn(true);
        when(jdbc.queryForObject(contains("WHERE id_cliente = :id"), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(0);
        when(jdbc.queryForList(contains("SELECT id_pedido, id_repartidor FROM pedidos"), any(MapSqlParameterSource.class)))
                .thenReturn(List.of(Map.of("id_pedido", 88, "id_repartidor", 42)));

        service.eliminarCuenta(10, new EliminarCuentaRequest("secreta123"));

        assertThat(u.isCuentaActiva()).isFalse();
        assertThat(u.getMotivoBaja()).isEqualTo("Cuenta eliminada por el usuario");
        assertThat(u.getFechaBaja()).isNotNull();
        assertThat(u.getTokenSesionHash()).isNull();
        assertThat(u.getTokenFcm()).isNull();
        assertThat(u.getSesionFechaCreacion()).isNull();
        assertThat(u.getSesionFechaExpiracion()).isNull();
        assertThat(u.getCorreo()).startsWith("eliminado.10.").endsWith("@baja.local");
        assertThat(u.getTelefono()).startsWith("baja-10-");
        assertThat(u.getTelefono().length()).isLessThanOrEqualTo(20);

        verify(usuarioRepo).save(u);
        verify(jdbc).update(contains("UPDATE direcciones_clientes SET activo = FALSE"), any(MapSqlParameterSource.class));
        verify(pushService).notificar(eq(42), eq("Pedido cancelado"), anyString(), anyMap());
    }

    @Test
    void eliminarCuenta_repartidorConInventarioEnVehiculo_bloquea() {
        Usuario u = crearUsuario(20, RolUsuario.repartidor);
        Repartidor r = new Repartidor();
        r.setId(20);
        r.setIdNegocio(5);
        r.setIdVehiculoActual(7);
        when(usuarioRepo.findById(20)).thenReturn(Optional.of(u));
        when(repartidorRepo.findById(20)).thenReturn(Optional.of(r));
        when(passwordEncoder.matches("secreta123", "HASH_VALIDO")).thenReturn(true);
        when(jdbc.queryForObject(contains("WHERE id_repartidor = :id"), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(0);
        when(jdbc.queryForObject(contains("FROM inventario_vehiculo"), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(4);

        assertThatThrownBy(() -> service.eliminarCuenta(20, new EliminarCuentaRequest("secreta123")))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("codigo")
                .isEqualTo("VEHICULO_CON_INVENTARIO");

        verify(usuarioRepo, never()).save(any());
    }

    @Test
    void eliminarCuenta_repartidorUnicoDueno_desactivaNegocioYConservaDuenoUnico() {
        Usuario u = crearUsuario(20, RolUsuario.repartidor);
        Repartidor r = new Repartidor();
        r.setId(20);
        r.setIdNegocio(5);
        r.setEsDueno(true);
        Negocio neg = new Negocio();
        neg.setId(5);
        neg.setActivo(true);

        when(usuarioRepo.findById(20)).thenReturn(Optional.of(u));
        when(repartidorRepo.findById(20)).thenReturn(Optional.of(r));
        when(negocioRepo.findById(5)).thenReturn(Optional.of(neg));
        when(passwordEncoder.matches("secreta123", "HASH_VALIDO")).thenReturn(true);
        when(jdbc.queryForObject(contains("WHERE id_repartidor = :id"), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(0);
        when(jdbc.queryForList(contains("SELECT r.id_usuario FROM repartidores r"), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(List.of());
        when(jdbc.queryForList(contains("SELECT id_pedido FROM pedidos"), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(List.of());

        service.eliminarCuenta(20, new EliminarCuentaRequest("secreta123"));

        assertThat(r.isEsDueno()).isTrue();
        assertThat(neg.isActivo()).isFalse();
        verify(negocioRepo).save(neg);
        assertThat(u.isCuentaActiva()).isFalse();
        verify(usuarioRepo).save(u);
    }

    @Test
    void eliminarCuenta_repartidorDuenoConSucesor_transfierePropiedad() {
        Usuario u = crearUsuario(20, RolUsuario.repartidor);
        Repartidor saliente = new Repartidor();
        saliente.setId(20);
        saliente.setIdNegocio(5);
        saliente.setEsDueno(true);

        Repartidor sucesor = new Repartidor();
        sucesor.setId(21);
        sucesor.setIdNegocio(5);
        sucesor.setEsDueno(false);
        sucesor.setAprobado(true);

        when(usuarioRepo.findById(20)).thenReturn(Optional.of(u));
        when(repartidorRepo.findById(20)).thenReturn(Optional.of(saliente));
        when(repartidorRepo.findById(21)).thenReturn(Optional.of(sucesor));
        when(passwordEncoder.matches("secreta123", "HASH_VALIDO")).thenReturn(true);
        when(jdbc.queryForObject(contains("WHERE id_repartidor = :id"), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(0);
        when(jdbc.queryForList(contains("SELECT r.id_usuario FROM repartidores r"), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(List.of(21));
        when(jdbc.queryForList(contains("SELECT id_pedido FROM pedidos"), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(List.of());

        service.eliminarCuenta(20, new EliminarCuentaRequest("secreta123"));

        assertThat(saliente.isEsDueno()).isFalse();
        assertThat(sucesor.isEsDueno()).isTrue();
        verify(repartidorRepo).saveAndFlush(saliente);
        verify(repartidorRepo).save(sucesor);
        verify(negocioRepo, never()).save(any());
        verify(usuarioRepo).save(u);
    }
}
