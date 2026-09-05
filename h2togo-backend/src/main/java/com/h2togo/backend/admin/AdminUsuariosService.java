package com.h2togo.backend.admin;

import com.h2togo.backend.admin.dto.AltaUsuarioRequest;
import com.h2togo.backend.admin.dto.BajaRequest;
import com.h2togo.backend.auth.AuthMapper;
import com.h2togo.backend.auth.dto.PerfilResponse;
import com.h2togo.backend.common.BusinessRuleException;
import com.h2togo.backend.common.ConflictException;
import com.h2togo.backend.common.NotFoundException;
import com.h2togo.backend.common.enums.RolUsuario;
import com.h2togo.backend.negocios.Negocio;
import com.h2togo.backend.negocios.NegocioRepository;
import com.h2togo.backend.usuarios.Administrador;
import com.h2togo.backend.usuarios.AdministradorRepository;
import com.h2togo.backend.usuarios.Cliente;
import com.h2togo.backend.usuarios.ClienteRepository;
import com.h2togo.backend.usuarios.Repartidor;
import com.h2togo.backend.usuarios.RepartidorRepository;
import com.h2togo.backend.usuarios.Usuario;
import com.h2togo.backend.usuarios.UsuarioRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Alta, baja lógica (con transferencia de negocio) y reactivación de usuarios (CU-014/015). */
@Service
public class AdminUsuariosService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final RepartidorRepository repartidorRepository;
    private final AdministradorRepository administradorRepository;
    private final NegocioRepository negocioRepository;
    private final PasswordEncoder passwordEncoder;
    private final NamedParameterJdbcTemplate jdbc;

    public AdminUsuariosService(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
            RepartidorRepository repartidorRepository, AdministradorRepository administradorRepository,
            NegocioRepository negocioRepository, PasswordEncoder passwordEncoder,
            NamedParameterJdbcTemplate jdbc) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.repartidorRepository = repartidorRepository;
        this.administradorRepository = administradorRepository;
        this.negocioRepository = negocioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
    }

    @Transactional
    public PerfilResponse alta(AltaUsuarioRequest req) {
        if (usuarioRepository.existsByCorreo(req.correo())) {
            throw new ConflictException("RN-015_CORREO_DUPLICADO", "El correo ya está registrado.");
        }
        if (usuarioRepository.existsByTelefono(req.telefono())) {
            throw new ConflictException("RN-015_TELEFONO_DUPLICADO", "El teléfono ya está registrado.");
        }
        Usuario u = new Usuario();
        u.setNombre(req.nombre());
        u.setApellidos(req.apellidos());
        u.setCorreo(req.correo());
        u.setTelefono(req.telefono());
        u.setPasswordHash(passwordEncoder.encode(req.password())); // RN-017
        u.setRol(req.rol());
        u.setTelefonoVerificado(true); // alta por admin: pre-verificado (§10 #30)
        u.setCuentaActiva(true);
        usuarioRepository.save(u);

        switch (req.rol()) {
            case cliente -> {
                Cliente c = new Cliente();
                c.setUsuario(u);
                clienteRepository.save(c);
            }
            case admin -> {
                Administrador a = new Administrador();
                a.setUsuario(u);
                administradorRepository.save(a);
            }
            case repartidor -> crearRepartidor(req, u);
        }
        return AuthMapper.toPerfil(u);
    }

    private void crearRepartidor(AltaUsuarioRequest req, Usuario u) {
        AltaUsuarioRequest.NegocioAlta n = req.negocio();
        boolean nuevo = n != null && n.nombreComercial() != null && !n.nombreComercial().isBlank();
        boolean existente = n != null && n.idExistente() != null;
        if (nuevo == existente) {
            throw new BusinessRuleException("NEGOCIO_REQUERIDO",
                    "Un repartidor debe crear un negocio nuevo o unirse a uno existente (exactamente uno).");
        }
        Repartidor r = new Repartidor();
        r.setUsuario(u);
        if (existente) {
            Negocio neg = negocioRepository.findById(n.idExistente())
                    .orElseThrow(() -> new NotFoundException("NEGOCIO_NO_ENCONTRADO", "El negocio no existe."));
            r.setIdNegocio(neg.getId());
            repartidorRepository.save(r);
        } else {
            Negocio negocio = new Negocio();
            negocio.setNombreComercial(n.nombreComercial());
            negocio.setIdDueno(u.getId()); // RN-024: queda como dueño
            negocioRepository.save(negocio);
            r.setIdNegocio(negocio.getId());
            repartidorRepository.save(r);
        }
    }

    @Transactional
    public void baja(int idUsuario, BajaRequest req) {
        Usuario u = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new NotFoundException("USUARIO_NO_ENCONTRADO", "Usuario no encontrado."));

        if (u.getRol() == RolUsuario.repartidor) {
            Integer enCamino = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM pedidos WHERE id_repartidor = :id AND estado_actual = 'en_camino'",
                    new MapSqlParameterSource("id", idUsuario), Integer.class);
            if (enCamino != null && enCamino > 0) {
                throw new BusinessRuleException("REPARTIDOR_CON_PEDIDOS_ACTIVOS",
                        "El repartidor tiene pedidos en camino; no se puede dar de baja aún.");
            }
            resolverNegociosEnPropiedad(idUsuario, req.idNuevoDueno());
        }

        u.setCuentaActiva(false);
        u.setMotivoBaja(req.motivo());
        u.setFechaBaja(OffsetDateTime.now());
        u.setTokenSesion(null);
        u.setTokenFcm(null);
        u.setSesionFechaCreacion(null);
        u.setSesionFechaExpiracion(null);
        usuarioRepository.save(u);
    }

    /** RN-024: cada negocio del que es dueño se transfiere o queda inactivo. */
    private void resolverNegociosEnPropiedad(int idUsuario, Integer idNuevoDueno) {
        List<Negocio> propios = negocioRepository.findAll().stream()
                .filter(n -> Integer.valueOf(idUsuario).equals(n.getIdDueno()))
                .toList();
        for (Negocio negocio : propios) {
            if (idNuevoDueno != null) {
                Repartidor nuevo = repartidorRepository.findById(idNuevoDueno)
                        .filter(r -> negocio.getId().equals(r.getIdNegocio()) && !r.getId().equals(idUsuario))
                        .orElseThrow(() -> new BusinessRuleException("NUEVO_DUENO_INVALIDO",
                                "El nuevo dueño debe ser otro repartidor del mismo negocio."));
                negocio.setIdDueno(nuevo.getId());
            } else {
                negocio.setActivo(false); // RN-024: sin transferencia, el negocio queda inactivo
            }
            negocioRepository.save(negocio);
        }
    }

    @Transactional
    public void reactivar(int idUsuario) {
        Usuario u = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new NotFoundException("USUARIO_NO_ENCONTRADO", "Usuario no encontrado."));
        u.setCuentaActiva(true);
        u.setMotivoBaja(null);
        u.setFechaBaja(null);
        usuarioRepository.save(u);
    }
}
