package com.h2togo.backend.usuarios;

import com.h2togo.backend.common.NotFoundException;
import com.h2togo.backend.common.enums.RolUsuario;
import com.h2togo.backend.negocios.NegocioRepository;
import com.h2togo.backend.usuarios.dto.PerfilMeResponse;
import com.h2togo.backend.usuarios.dto.PerfilMeResponse.ClienteInfo;
import com.h2togo.backend.usuarios.dto.PerfilMeResponse.RepartidorInfo;
import com.h2togo.backend.usuarios.dto.PerfilUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Perfil propio del usuario autenticado (GET/PATCH /usuarios/me). */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final RepartidorRepository repartidorRepository;
    private final NegocioRepository negocioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
            RepartidorRepository repartidorRepository, NegocioRepository negocioRepository) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.repartidorRepository = repartidorRepository;
        this.negocioRepository = negocioRepository;
    }

    @Transactional(readOnly = true)
    public PerfilMeResponse perfil(int idUsuario) {
        Usuario u = cargar(idUsuario);
        ClienteInfo cliente = null;
        RepartidorInfo repartidor = null;
        if (u.getRol() == RolUsuario.cliente) {
            cliente = clienteRepository.findById(idUsuario)
                    .map(c -> new ClienteInfo(c.getAusenciasConsecutivas(), c.getSuspendidoHasta()))
                    .orElse(null);
        } else if (u.getRol() == RolUsuario.repartidor) {
            repartidor = repartidorRepository.findById(idUsuario)
                    .map(r -> new RepartidorInfo(r.getIdNegocio(), r.isEstadoOperativo(),
                            r.getIdVehiculoActual(), esDueno(r)))
                    .orElse(null);
        }
        return toPerfil(u, cliente, repartidor);
    }

    @Transactional
    public PerfilMeResponse actualizar(int idUsuario, PerfilUpdateRequest req) {
        Usuario u = cargar(idUsuario);
        if (req.nombre() != null) {
            u.setNombre(req.nombre());
        }
        if (req.apellidos() != null) {
            u.setApellidos(req.apellidos());
        }
        if (req.urlFotoPerfil() != null) {
            u.setUrlFotoPerfil(req.urlFotoPerfil());
        }
        usuarioRepository.save(u);
        return perfil(idUsuario);
    }

    private boolean esDueno(Repartidor r) {
        if (r.getIdNegocio() == null) {
            return false;
        }
        return negocioRepository.findById(r.getIdNegocio())
                .map(n -> r.getId().equals(n.getIdDueno()))
                .orElse(false);
    }

    private Usuario cargar(int idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new NotFoundException("USUARIO_NO_ENCONTRADO", "Usuario no encontrado."));
    }

    private static PerfilMeResponse toPerfil(Usuario u, ClienteInfo cliente, RepartidorInfo repartidor) {
        return new PerfilMeResponse(u.getId(), u.getNombre(), u.getApellidos(), u.getCorreo(),
                u.getTelefono(), u.getRol(), u.getUrlFotoPerfil(), u.isTelefonoVerificado(),
                cliente, repartidor);
    }
}
