package com.h2togo.backend.auth;

import com.h2togo.backend.repartidores.MiembrosNegocioService;
import com.h2togo.backend.auth.dto.RecuperacionRequest;
import com.h2togo.backend.auth.dto.RecuperacionConfirmacionRequest;
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
import com.h2togo.backend.security.TokenSesion;
import com.h2togo.backend.usuarios.Cliente;
import com.h2togo.backend.usuarios.ClienteRepository;
import com.h2togo.backend.usuarios.Repartidor;
import com.h2togo.backend.usuarios.RepartidorRepository;
import com.h2togo.backend.usuarios.Usuario;
import com.h2togo.backend.usuarios.UsuarioRepository;
import java.security.SecureRandom;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro, verificación por OTP, login con sesión única y logout (CU-001/002/003).
 * Reglas: RN-015 (sin duplicados), RN-017 (Argon2id), RN-002 (OTP), RN-018 (sesión única),
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
    /** Código que siempre se acepta, solo para pruebas y demos. Vacío = desactivado. */
    private final String otpDemo;
    private final MiembrosNegocioService miembrosService;

    @org.springframework.beans.factory.annotation.Autowired
    public AuthService(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
            RepartidorRepository repartidorRepository, NegocioRepository negocioRepository,
            PasswordEncoder passwordEncoder,
            com.h2togo.backend.notificaciones.SmsService smsService,
            NamedParameterJdbcTemplate jdbc,
            @Value("${h2togo.auth.otp-longitud}") int otpLongitud,
            @Value("${h2togo.auth.otp-vigencia-minutos}") int otpVigenciaMinutos,
            @Value("${h2togo.auth.token-dias}") int tokenDias,
            @Value("${h2togo.auth.otp-demo:123456}") String otpDemo,
            MiembrosNegocioService miembrosService) {
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
        this.otpDemo = otpDemo;
        this.miembrosService = miembrosService;
    }

    public AuthService(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
            RepartidorRepository repartidorRepository, NegocioRepository negocioRepository,
            PasswordEncoder passwordEncoder,
            com.h2togo.backend.notificaciones.SmsService smsService,
            int otpLongitud,
            int otpVigenciaMinutos,
            int tokenDias) {
        this(usuarioRepository, clienteRepository, repartidorRepository, negocioRepository,
                passwordEncoder, smsService, null, otpLongitud, otpVigenciaMinutos, tokenDias, "123456", null);
    }

    /** Al quinto intento fallido el código SMS deja de servir y hay que pedir otro (v7 [C5]). */
    private static final int MAX_INTENTOS_CODIGO = 5;

    /** CU-001: alta de cuenta + OTP, en una transacción (la BD revisa al COMMIT que el negocio nuevo tenga dueño). */
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
        u.setPasswordHash(passwordEncoder.encode(req.password())); // RN-017 (Argon2id)
        u.setRol(req.rol());
        u.setTelefonoVerificado(false);
        u.setCuentaActiva(true);
        asignarCodigo(u, codigo);
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
            r.setAprobado(false); // el dueño debe aprobarlo antes de que tome pedidos
            repartidorRepository.save(r);
            return existente.getId();
        }
        // Negocio nuevo: el solicitante queda como dueño aprobado (RN-024).
        int idNegocio = insertarNegocio(n);
        r.setIdNegocio(idNegocio);
        r.setEsDueno(true);
        repartidorRepository.save(r);
        guardarHorario(idNegocio, n);
        // Productos y vehículos NO se siembran: el dueño los solicita después
        // (AGREGAR_PRODUCTO / AGREGAR_VEHICULO) y el admin los aprueba (RN-020).

        return idNegocio;
    }

    /**
     * Inserta el negocio de una vez, con la dirección base solo si viene completa (el CHECK del
     * esquema exige todo o nada): sin sesión no se puede modificar un negocio, solo crearlo. La
     * app no captura número exterior, así que se registra "S/N" en ese caso.
     */
    private int insertarNegocio(RegistroRequest.NegocioRegistroRequest n) {
        boolean conDireccion = !(vacio(n.calle()) || vacio(n.colonia()) || vacio(n.codigoPostal())
                || n.lat() == null || n.lon() == null);
        var params = new MapSqlParameterSource()
                .addValue("nombre", n.nombreComercial().trim())
                .addValue("calle", conDireccion ? n.calle().trim() : null)
                .addValue("numExt", !conDireccion ? null : vacio(n.numeroExterior()) ? "S/N" : n.numeroExterior().trim())
                .addValue("colonia", conDireccion ? n.colonia().trim() : null)
                .addValue("cp", conDireccion ? n.codigoPostal().trim() : null)
                .addValue("refs", conDireccion && !vacio(n.referencias()) ? n.referencias().trim() : null)
                .addValue("lat", conDireccion ? n.lat() : null)
                .addValue("lon", conDireccion ? n.lon() : null);
        return jdbc.queryForObject("""
                INSERT INTO negocios (nombre_comercial, calle, numero_exterior, colonia, codigo_postal,
                                      referencias, ubicacion_base)
                VALUES (:nombre, :calle, :numExt, :colonia, :cp, :refs,
                        CASE WHEN CAST(:lat AS double precision) IS NULL THEN NULL
                             ELSE ST_SetSRID(ST_MakePoint(CAST(:lon AS double precision),
                                                          CAST(:lat AS double precision)), 4326)::geography END)
                RETURNING id_negocio""", params, Integer.class);
    }

    /** Si se indicó horario, lo aplica de lunes a sábado; el domingo queda cerrado. */
    private void guardarHorario(int idNegocio, RegistroRequest.NegocioRegistroRequest n) {
        if (vacio(n.horarioApertura()) || vacio(n.horarioCierre())) {
            return;
        }
        LocalTime apertura = parsearHora(n.horarioApertura());
        LocalTime cierre = parsearHora(n.horarioCierre());
        if (!cierre.isAfter(apertura)) {
            throw new BusinessRuleException("HORARIO_INVALIDO", "La hora de cierre debe ser posterior a la de apertura.");
        }
        var params = new MapSqlParameterSource()
                .addValue("negId", idNegocio).addValue("ap", apertura).addValue("ci", cierre);
        jdbc.update("""
                INSERT INTO horarios_negocio (id_negocio, dia_semana, hora_apertura, hora_cierre, cerrado)
                SELECT :negId, d, :ap, :ci, false FROM generate_series(1, 6) AS d
                ON CONFLICT DO NOTHING""", params);
        jdbc.update("""
                INSERT INTO horarios_negocio (id_negocio, dia_semana, hora_apertura, hora_cierre, cerrado)
                VALUES (:negId, 7, NULL, NULL, true) ON CONFLICT DO NOTHING""", params);
    }

    /** Acepta "8:00", "08:00" u "08:00:00". */
    private static LocalTime parsearHora(String valor) {
        String v = valor.trim();
        if (v.matches("\\d:\\d{2}(:\\d{2})?")) {
            v = "0" + v;
        }
        try {
            return LocalTime.parse(v);
        } catch (DateTimeParseException e) {
            throw new BusinessRuleException("HORARIO_INVALIDO", "Horario inválido: '" + valor + "'. Usa el formato HH:mm.");
        }
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    /** CU-001: valida el OTP y su vigencia → telefono_verificado = true (RN-002). */
    @Transactional(noRollbackFor = BusinessRuleException.class) // se guarda el intento fallido
    public void verificarTelefono(OtpRequest req) {
        Usuario u = usuarioRepository.findByCorreo(req.correo())
                .orElseThrow(() -> new BusinessRuleException("OTP_INVALIDO",
                        "El código es inválido o expiró."));
        if (u.isTelefonoVerificado()) {
            return; // idempotente
        }
        if (!codigoValido(u, req.codigo())) {
            throw new BusinessRuleException("OTP_INVALIDO", "El código es inválido o expiró.");
        }
        u.setTelefonoVerificado(true);
        asignarCodigo(u, null);
        usuarioRepository.save(u);
        // Quien pidió unirse a una purificadora ya es una persona verificada: se avisa al dueño.
        if (u.getRol() == RolUsuario.repartidor && miembrosService != null) {
            miembrosService.avisarSolicitud(u.getId(), u.getNombre() + " " + u.getApellidos());
        }
    }

    /**
     * "¿Olvidaste tu contraseña?": manda un código por SMS al teléfono de la cuenta. No revela si
     * el correo existe (RN-016): siempre responde igual.
     */
    @Transactional
    public void solicitarRecuperacion(RecuperacionRequest req) {
        Usuario u = usuarioRepository.findByCorreo(req.correo()).orElse(null);
        if (u == null || !u.isTelefonoVerificado() || !u.isCuentaActiva()) {
            return;
        }
        String codigo = generarOtp();
        asignarCodigo(u, codigo);
        usuarioRepository.save(u);
        smsService.enviarCodigoVerificacion(u.getTelefono(), codigo);
    }

    /** Valida el código y guarda la nueva contraseña. Cierra la sesión abierta en cualquier dispositivo. */
    @Transactional(noRollbackFor = BusinessRuleException.class) // se guarda el intento fallido
    public void confirmarRecuperacion(RecuperacionConfirmacionRequest req) {
        Usuario u = usuarioRepository.findByCorreo(req.correo())
                .filter(x -> x.isTelefonoVerificado() && x.isCuentaActiva())
                .orElseThrow(() -> new BusinessRuleException("OTP_INVALIDO", "El código es inválido o expiró."));
        if (!codigoValido(u, req.codigo())) {
            throw new BusinessRuleException("OTP_INVALIDO", "El código es inválido o expiró.");
        }
        u.setPasswordHash(passwordEncoder.encode(req.nuevaContrasena()));
        asignarCodigo(u, null);
        u.setTokenSesionHash(null);
        u.setTokenFcm(null);
        u.setSesionFechaCreacion(null);
        u.setSesionFechaExpiracion(null);
        usuarioRepository.save(u);
    }

    /**
     * OTP válido: el de la base sin vencer, el de Twilio Verify o el código demo (si está activo).
     * Cada fallo cuenta; al llegar a {@link #MAX_INTENTOS_CODIGO} el código se borra para que no
     * se pueda adivinar probando (son solo 10^6 combinaciones).
     */
    private boolean codigoValido(Usuario u, String codigo) {
        boolean esDemo = otpDemo != null && !otpDemo.isBlank() && otpDemo.equals(codigo);
        boolean validoDb = u.getCodigoVerificacion() != null
                && u.getCodigoVerificacion().equals(codigo)
                && u.getCodigoVerificacionExpiracion() != null
                && u.getCodigoVerificacionExpiracion().isAfter(OffsetDateTime.now());
        if (esDemo || validoDb || smsService.verificarCodigo(u.getTelefono(), codigo)) {
            return true;
        }
        if (u.getCodigoVerificacion() != null) {
            u.setCodigoIntentos((short) (u.getCodigoIntentos() + 1));
            if (u.getCodigoIntentos() >= MAX_INTENTOS_CODIGO) {
                asignarCodigo(u, null);
            }
            usuarioRepository.save(u);
        }
        return false;
    }

    /** Fija (o borra, con null) el código SMS vigente y reinicia sus intentos. */
    private void asignarCodigo(Usuario u, String codigo) {
        u.setCodigoVerificacion(codigo);
        u.setCodigoVerificacionExpiracion(codigo == null ? null : OffsetDateTime.now().plusMinutes(otpVigenciaMinutos));
        u.setCodigoIntentos((short) 0);
    }

    /** CU-001: reenvía un OTP nuevo. No revela si el correo existe (RN-016). */
    @Transactional
    public void reenviarOtp(ReenviarOtpRequest req) {
        Usuario u = usuarioRepository.findByCorreo(req.correo()).orElse(null);
        if (u == null || u.isTelefonoVerificado()) {
            return;
        }
        String codigo = generarOtp();
        asignarCodigo(u, codigo);
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
        boolean sesionVigente = u.getTokenSesionHash() != null
                && u.getSesionFechaExpiracion() != null
                && u.getSesionFechaExpiracion().isAfter(ahora);
        if (sesionVigente && !req.forzarSesion()) {
            throw new ConflictException("SESION_ACTIVA_EN_OTRO_DISPOSITIVO",
                    "Ya hay una sesión activa en otro dispositivo. Reintenta con forzar=true para cerrarla.");
        }

        String token = generarToken();
        OffsetDateTime expira = ahora.plusDays(tokenDias);
        u.setTokenSesionHash(TokenSesion.hash(token)); // el token en claro solo va en la respuesta
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
        u.setTokenSesionHash(null);
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
