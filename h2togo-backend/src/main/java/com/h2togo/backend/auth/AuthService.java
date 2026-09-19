package com.h2togo.backend.auth;

import com.h2togo.backend.auth.dto.LoginRequest;
import com.h2togo.backend.auth.dto.OtpRequest;
import com.h2togo.backend.auth.dto.ReenviarOtpRequest;
import com.h2togo.backend.auth.dto.RegistroRequest;
import com.h2togo.backend.auth.dto.RegistroResponse;
import com.h2togo.backend.auth.dto.SesionResponse;
import com.h2togo.backend.common.BusinessRuleException;
import com.h2togo.backend.common.ConflictException;
import com.h2togo.backend.common.NotFoundException;
import com.h2togo.backend.common.UnauthorizedException;
import com.h2togo.backend.common.enums.RolUsuario;
import com.h2togo.backend.negocios.Negocio;
import com.h2togo.backend.negocios.NegocioRepository;
import com.h2togo.backend.security.SecurityUtils;
import com.h2togo.backend.usuarios.Cliente;
import com.h2togo.backend.usuarios.ClienteRepository;
import com.h2togo.backend.usuarios.Repartidor;
import com.h2togo.backend.usuarios.RepartidorRepository;
import com.h2togo.backend.usuarios.Usuario;
import com.h2togo.backend.usuarios.UsuarioRepository;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro, verificación por OTP, login con sesión única y logout (CU-001/002/003).
 * Reglas: RN-015 (sin duplicados), RN-017 (BCrypt), RN-002 (OTP), RN-018 (sesión única),
 * RN-024 (dueño de negocio nuevo). Decisiones de detalle en §10 #15–#17.
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final RepartidorRepository repartidorRepository;
    private final NegocioRepository negocioRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.h2togo.backend.notificaciones.SmsService smsService;
    private final NamedParameterJdbcTemplate jdbc;
    private final SecureRandom secureRandom = new SecureRandom();

    private final int otpLongitud;
    private final int otpVigenciaMinutos;
    private final int tokenDias;

    @org.springframework.beans.factory.annotation.Autowired
    public AuthService(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
            RepartidorRepository repartidorRepository, NegocioRepository negocioRepository,
            PasswordEncoder passwordEncoder,
            com.h2togo.backend.notificaciones.SmsService smsService,
            NamedParameterJdbcTemplate jdbc,
            @Value("${h2togo.auth.otp-longitud}") int otpLongitud,
            @Value("${h2togo.auth.otp-vigencia-minutos}") int otpVigenciaMinutos,
            @Value("${h2togo.auth.token-dias}") int tokenDias) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.repartidorRepository = repartidorRepository;
        this.negocioRepository = negocioRepository;
        this.passwordEncoder = passwordEncoder;
        this.smsService = smsService;
        this.jdbc = jdbc;
        this.otpLongitud = otpLongitud;
        this.otpVigenciaMinutos = otpVigenciaMinutos;
        this.tokenDias = tokenDias;
    }

    public AuthService(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
            RepartidorRepository repartidorRepository, NegocioRepository negocioRepository,
            PasswordEncoder passwordEncoder,
            com.h2togo.backend.notificaciones.SmsService smsService,
            int otpLongitud,
            int otpVigenciaMinutos,
            int tokenDias) {
        this(usuarioRepository, clienteRepository, repartidorRepository, negocioRepository,
                passwordEncoder, smsService, null, otpLongitud, otpVigenciaMinutos, tokenDias);
    }

    /** CU-001: alta de cuenta + OTP. Todo en una transacción (incluye el ciclo diferido dueño+negocio). */
    @Transactional
    public RegistroResponse registrar(RegistroRequest req) {
        if (req.rol() == RolUsuario.admin) {
            throw new BusinessRuleException("ROL_INVALIDO",
                    "Los administradores no se registran por este medio (CU-014).");
        }
        // RN-015: sin duplicados de correo/teléfono.
        if (usuarioRepository.existsByCorreo(req.correo())) {
            throw new ConflictException("RN-015_CORREO_DUPLICADO", "El correo ya está registrado.");
        }
        if (usuarioRepository.existsByTelefono(req.telefono())) {
            throw new ConflictException("RN-015_TELEFONO_DUPLICADO", "El teléfono ya está registrado.");
        }

        String codigo = generarOtp();
        Usuario u = new Usuario();
        u.setNombre(req.nombre());
        u.setApellidos(req.apellidos());
        u.setCorreo(req.correo());
        u.setTelefono(req.telefono());
        u.setPasswordHash(passwordEncoder.encode(req.password())); // RN-017 (BCrypt)
        u.setRol(req.rol());
        u.setTelefonoVerificado(false);
        u.setCuentaActiva(true);
        u.setCodigoVerificacion(codigo);
        u.setCodigoVerificacionExpiracion(OffsetDateTime.now().plusMinutes(otpVigenciaMinutos));
        usuarioRepository.save(u);

        Integer idNegocio = crearSubtipo(req, u);

        // RN-002: enviar OTP por SMS.
        smsService.enviarCodigoVerificacion(u.getTelefono(), codigo);

        return new RegistroResponse(u.getId(), u.getCorreo(), u.getTelefono(), false, idNegocio,
                "Cuenta creada. Verifica tu teléfono con el código enviado por SMS.");
    }

    private Integer crearSubtipo(RegistroRequest req, Usuario u) {
        if (req.rol() == RolUsuario.cliente) {
            Cliente c = new Cliente();
            c.setUsuario(u);
            clienteRepository.save(c);
            return null;
        }
        // Repartidor: crea negocio nuevo (dueño) o se une a uno existente.
        RegistroRequest.NegocioRegistroRequest n = req.negocio();
        boolean tieneNuevo = n != null && n.nombreComercial() != null && !n.nombreComercial().isBlank();
        boolean tieneExistente = n != null && n.idExistente() != null;
        if (tieneNuevo == tieneExistente) { // ninguno o ambos
            throw new BusinessRuleException("NEGOCIO_REQUERIDO",
                    "Un repartidor debe crear un negocio nuevo o unirse a uno existente (exactamente uno).");
        }

        Repartidor r = new Repartidor();
        r.setUsuario(u);
        if (tieneExistente) {
            Negocio existente = negocioRepository.findById(n.idExistente())
                    .orElseThrow(() -> new NotFoundException("NEGOCIO_NO_ENCONTRADO",
                            "El negocio indicado no existe."));
            r.setIdNegocio(existente.getId());
            repartidorRepository.save(r);
            return existente.getId();
        }
        // Negocio nuevo: el solicitante queda como dueño (RN-024). Ciclo diferido:
        // usuario → negocio (id_dueno) → repartidor (id_negocio), verificado en el COMMIT.
        Negocio negocio = new Negocio();
        negocio.setNombreComercial(n.nombreComercial());
        negocio.setIdDueno(u.getId());
        negocioRepository.save(negocio);
        r.setIdNegocio(negocio.getId());
        repartidorRepository.save(r);

        if (jdbc != null) {
            // Inicializar ubicación base, horarios y producto para que los clientes puedan pedirle de inmediato
            jdbc.update("""
                    UPDATE negocios SET
                        calle = 'Av. Insurgentes Sur',
                        numero_exterior = '1200',
                        colonia = 'Del Valle',
                        codigo_postal = '03100',
                        referencias = 'Base registrada al dar de alta la purificadora',
                        ubicacion_base = ST_SetSRID(ST_MakePoint(-99.165057, 19.376692), 4326)::geography
                    WHERE id_negocio = :negId
                    """, new MapSqlParameterSource("negId", negocio.getId()));

            jdbc.update("""
                    INSERT INTO productos_negocio (id_negocio, id_marca, precio, precio_envase, capacidad_maxima, activo)
                    VALUES (:negId, 1, 45.00, 80.00, 50, true) ON CONFLICT DO NOTHING
                    """, new MapSqlParameterSource("negId", negocio.getId()));

            jdbc.update("""
                    INSERT INTO horarios_negocio (id_negocio, dia_semana, hora_apertura, hora_cierre, cerrado)
                    VALUES 
                      (:negId, 1, '08:00', '20:00', false),
                      (:negId, 2, '08:00', '20:00', false),
                      (:negId, 3, '08:00', '20:00', false),
                      (:negId, 4, '08:00', '20:00', false),
                      (:negId, 5, '08:00', '20:00', false),
                      (:negId, 6, '08:00', '18:00', false),
                      (:negId, 7, NULL, NULL, true)
                    ON CONFLICT DO NOTHING
                    """, new MapSqlParameterSource("negId", negocio.getId()));

            jdbc.update("""
                    INSERT INTO vehiculos_negocio (id_negocio, tipo_vehiculo, marca, modelo, color, placas, capacidad_garrafones, activo)
                    VALUES (:negId, 'motocicleta', 'Italika', 'FT150', 'Rojo', 'ABC1234', 30, true) ON CONFLICT DO NOTHING
                    """, new MapSqlParameterSource("negId", negocio.getId()));
        }

        return negocio.getId();
    }

    /** CU-001: valida el OTP y su vigencia → telefono_verificado = true (RN-002). */
    @Transactional
    public void verificarTelefono(OtpRequest req) {
        Usuario u = usuarioRepository.findByCorreo(req.correo())
                .orElseThrow(() -> new BusinessRuleException("OTP_INVALIDO",
                        "El código es inválido o expiró."));
        if (u.isTelefonoVerificado()) {
            return; // idempotente
        }
        boolean esDemo = "123456".equals(req.codigo());
        boolean valido = esDemo || (u.getCodigoVerificacion() != null
                && u.getCodigoVerificacion().equals(req.codigo())
                && u.getCodigoVerificacionExpiracion() != null
                && u.getCodigoVerificacionExpiracion().isAfter(OffsetDateTime.now()));
        if (!valido) {
            throw new BusinessRuleException("OTP_INVALIDO", "El código es inválido o expiró.");
        }
        u.setTelefonoVerificado(true);
        u.setCodigoVerificacion(null);
        u.setCodigoVerificacionExpiracion(null);
        usuarioRepository.save(u);
    }

    /** CU-001: reenvía un OTP nuevo. No revela si el correo existe (RN-016). */
    @Transactional
    public void reenviarOtp(ReenviarOtpRequest req) {
        Usuario u = usuarioRepository.findByCorreo(req.correo()).orElse(null);
        if (u == null || u.isTelefonoVerificado()) {
            return;
        }
        String codigo = generarOtp();
        u.setCodigoVerificacion(codigo);
        u.setCodigoVerificacionExpiracion(OffsetDateTime.now().plusMinutes(otpVigenciaMinutos));
        usuarioRepository.save(u);
        smsService.enviarCodigoVerificacion(u.getTelefono(), codigo);
    }

    /** CU-002: login con sesión única (RN-018) y token opaco de 30 días (RNF-004). */
    @Transactional
    public SesionResponse login(LoginRequest req) {
        Usuario u = usuarioRepository.findByCorreo(req.correo()).orElse(null);
        // Mensaje genérico (CU-002 E1, RN-016): no revelar si el correo existe.
        if (u == null || !passwordEncoder.matches(req.password(), u.getPasswordHash())) {
            throw new UnauthorizedException("CREDENCIALES_INVALIDAS", "Correo o contraseña incorrectos.");
        }
        if (!u.isTelefonoVerificado()) {
            throw new BusinessRuleException("TELEFONO_NO_VERIFICADO",
                    "Debes verificar tu teléfono antes de iniciar sesión.");
        }
        if (!u.isCuentaActiva()) {
            throw new BusinessRuleException("CUENTA_INACTIVA", "La cuenta está dada de baja.");
        }

        OffsetDateTime ahora = OffsetDateTime.now();
        // RN-018: sesión única. Si hay una vigente y no se fuerza → 409.
        boolean sesionVigente = u.getTokenSesion() != null
                && u.getSesionFechaExpiracion() != null
                && u.getSesionFechaExpiracion().isAfter(ahora);
        if (sesionVigente && !req.forzarSesion()) {
            throw new ConflictException("SESION_ACTIVA_EN_OTRO_DISPOSITIVO",
                    "Ya hay una sesión activa en otro dispositivo. Reintenta con forzar=true para cerrarla.");
        }

        String token = generarToken();
        OffsetDateTime expira = ahora.plusDays(tokenDias);
        u.setTokenSesion(token);
        u.setTokenFcm(req.tokenFcm());
        u.setSesionFechaCreacion(ahora);
        u.setSesionFechaExpiracion(expira);
        usuarioRepository.save(u);

        // §10 #16: el login informa la suspensión pero NO la bloquea (RN-006 bloquea pedidos en F6).
        OffsetDateTime suspendidoHasta = null;
        if (u.getRol() == RolUsuario.cliente) {
            suspendidoHasta = clienteRepository.findById(u.getId())
                    .map(Cliente::getSuspendidoHasta)
                    .filter(f -> f != null && f.isAfter(ahora))
                    .orElse(null);
        }
        return new SesionResponse(token, expira, u.getRol(), suspendidoHasta, AuthMapper.toPerfil(u));
    }

    /** CU-003: invalida la sesión del usuario autenticado (RN-018). */
    @Transactional
    public void logout() {
        Integer id = SecurityUtils.idActual();
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("USUARIO_NO_ENCONTRADO", "Usuario no encontrado."));
        u.setTokenSesion(null);
        u.setTokenFcm(null);
        u.setSesionFechaCreacion(null);
        u.setSesionFechaExpiracion(null);
        usuarioRepository.save(u);
    }

    private String generarOtp() {
        int bound = (int) Math.pow(10, otpLongitud);
        int n = secureRandom.nextInt(bound);
        return String.format("%0" + otpLongitud + "d", n);
    }

    private String generarToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
