package com.h2togo.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.h2togo.backend.auth.dto.LoginRequest;
import com.h2togo.backend.auth.dto.OtpRequest;
import com.h2togo.backend.auth.dto.RegistroRequest;
import com.h2togo.backend.common.BusinessRuleException;
import com.h2togo.backend.common.ConflictException;
import com.h2togo.backend.common.UnauthorizedException;
import com.h2togo.backend.common.enums.RolUsuario;
import com.h2togo.backend.negocios.NegocioRepository;
import com.h2togo.backend.notificaciones.SmsService;
import com.h2togo.backend.usuarios.ClienteRepository;
import com.h2togo.backend.usuarios.RepartidorRepository;
import com.h2togo.backend.usuarios.Usuario;
import com.h2togo.backend.usuarios.UsuarioRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Pruebas unitarias (hermético, sin BD) de las reglas de auth de F2. El flujo completo
 * contra la BD real vive en {@code AuthFlowIT} (Testcontainers).
 */
class AuthServiceTest {

    private UsuarioRepository usuarioRepo;
    private ClienteRepository clienteRepo;
    private RepartidorRepository repartidorRepo;
    private NegocioRepository negocioRepo;
    private PasswordEncoder passwordEncoder;
    private SmsService smsService;
    private AuthService service;

    @BeforeEach
    void setUp() {
        usuarioRepo = mock(UsuarioRepository.class);
        clienteRepo = mock(ClienteRepository.class);
        repartidorRepo = mock(RepartidorRepository.class);
        negocioRepo = mock(NegocioRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        smsService = mock(SmsService.class);
        service = new AuthService(usuarioRepo, clienteRepo, repartidorRepo, negocioRepo,
                passwordEncoder, smsService, 6, 10, 30);
    }

    private RegistroRequest registroCliente() {
        return new RegistroRequest("Ana", "García", "ana@test.mx", "password123",
                "5550000001", RolUsuario.cliente, null);
    }

    private Usuario usuarioVerificado(String hash) {
        Usuario u = new Usuario();
        u.setId(1);
        u.setCorreo("ana@test.mx");
        u.setTelefono("5550000001");
        u.setPasswordHash(hash);
        u.setRol(RolUsuario.cliente);
        u.setTelefonoVerificado(true);
        u.setCuentaActiva(true);
        return u;
    }

    @Test
    void registroRechazaCorreoDuplicado() {
        when(usuarioRepo.existsByCorreo("ana@test.mx")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(registroCliente()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("correo");
        verify(smsService, never()).enviarCodigoVerificacion(anyString(), anyString());
    }

    @Test
    void registroRechazaRolAdmin() {
        RegistroRequest req = new RegistroRequest("A", "B", "admin@test.mx", "password123",
                "5550000009", RolUsuario.admin, null);

        assertThatThrownBy(() -> service.registrar(req))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void registroClienteEnviaOtpYQuedaSinVerificar() {
        when(usuarioRepo.existsByCorreo(anyString())).thenReturn(false);
        when(usuarioRepo.existsByTelefono(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$hash");

        service.registrar(registroCliente());

        verify(usuarioRepo).save(any(Usuario.class));
        verify(clienteRepo).save(any());
        verify(smsService).enviarCodigoVerificacion(eq("5550000001"), anyString());
    }

    @Test
    void loginConCredencialesInvalidasEsGenerico() {
        when(usuarioRepo.findByCorreo("ana@test.mx")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(
                new LoginRequest("ana@test.mx", "loQueSea", null, false)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("incorrectos");
    }

    @Test
    void loginExigeTelefonoVerificado() {
        Usuario u = usuarioVerificado("$2a$hash");
        u.setTelefonoVerificado(false);
        when(usuarioRepo.findByCorreo("ana@test.mx")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("password123", "$2a$hash")).thenReturn(true);

        assertThatThrownBy(() -> service.login(
                new LoginRequest("ana@test.mx", "password123", null, false)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("verificar");
    }

    @Test
    void loginConSesionActivaSinForzarDevuelveConflicto() {
        Usuario u = usuarioVerificado("$2a$hash");
        u.setTokenSesion("token-vigente");
        u.setSesionFechaExpiracion(OffsetDateTime.now().plusDays(10));
        when(usuarioRepo.findByCorreo("ana@test.mx")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("password123", "$2a$hash")).thenReturn(true);

        assertThatThrownBy(() -> service.login(
                new LoginRequest("ana@test.mx", "password123", null, false)))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("SESION_ACTIVA_EN_OTRO_DISPOSITIVO"));
    }

    @Test
    void loginConForzarInvalidaSesionAnteriorYGeneraToken() {
        Usuario u = usuarioVerificado("$2a$hash");
        u.setTokenSesion("token-viejo");
        u.setSesionFechaExpiracion(OffsetDateTime.now().plusDays(10));
        when(usuarioRepo.findByCorreo("ana@test.mx")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("password123", "$2a$hash")).thenReturn(true);
        when(clienteRepo.findById(1)).thenReturn(Optional.empty());

        var sesion = service.login(new LoginRequest("ana@test.mx", "password123", "fcm-1", true));

        assertThat(sesion.token()).isNotBlank().isNotEqualTo("token-viejo");
        assertThat(sesion.rol()).isEqualTo(RolUsuario.cliente);
        assertThat(u.getTokenSesion()).isEqualTo(sesion.token());
    }

    @Test
    void verificarOtpInvalidoFalla() {
        Usuario u = usuarioVerificado("$2a$hash");
        u.setTelefonoVerificado(false);
        u.setCodigoVerificacion("123456");
        u.setCodigoVerificacionExpiracion(OffsetDateTime.now().plusMinutes(10));
        when(usuarioRepo.findByCorreo("ana@test.mx")).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.verificarTelefono(new OtpRequest("ana@test.mx", "000000")))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(u.isTelefonoVerificado()).isFalse();
    }
}
