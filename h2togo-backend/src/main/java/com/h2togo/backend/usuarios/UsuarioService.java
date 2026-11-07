package com.h2togo.backend.usuarios;

import com.h2togo.backend.bd.TareaSistema;
import com.h2togo.backend.common.BusinessRuleException;
import com.h2togo.backend.common.NotFoundException;
import com.h2togo.backend.common.enums.RolUsuario;
import com.h2togo.backend.negocios.NegocioRepository;
import com.h2togo.backend.notificaciones.PushService;
import com.h2togo.backend.usuarios.dto.CambioContrasenaRequest;
import com.h2togo.backend.usuarios.dto.EliminarCuentaRequest;
import com.h2togo.backend.usuarios.dto.PerfilMeResponse;
import com.h2togo.backend.usuarios.dto.PerfilMeResponse.ClienteInfo;
import com.h2togo.backend.usuarios.dto.PerfilMeResponse.RepartidorInfo;
import com.h2togo.backend.usuarios.dto.PerfilUpdateRequest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Perfil propio del usuario autenticado (GET/PATCH /usuarios/me y baja voluntaria). */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClienteRepository clienteRepository;
    private final RepartidorRepository repartidorRepository;
    private final NegocioRepository negocioRepository;
    private final NamedParameterJdbcTemplate jdbc;
    private final TareaSistema tareaSistema;
    private final PushService pushService;

    public UsuarioService(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
            RepartidorRepository repartidorRepository, PasswordEncoder passwordEncoder) {
        this(usuarioRepository, clienteRepository, repartidorRepository, passwordEncoder,
                null, null, null, null);
    }

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
            RepartidorRepository repartidorRepository, PasswordEncoder passwordEncoder,
            NegocioRepository negocioRepository, NamedParameterJdbcTemplate jdbc,
            TareaSistema tareaSistema, PushService pushService) {
        this.passwordEncoder = passwordEncoder;
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.repartidorRepository = repartidorRepository;
        this.negocioRepository = negocioRepository;
        this.jdbc = jdbc;
        this.tareaSistema = tareaSistema;
        this.pushService = pushService;
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
                            r.getIdVehiculoActual(), esDueno(r), r.isAprobado()))
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

    /** Cambio de contraseña con sesión iniciada; exige la actual. La sesión actual sigue abierta. */
    @Transactional
    public void cambiarContrasena(int idUsuario, CambioContrasenaRequest req) {
        Usuario u = cargar(idUsuario);
        if (!passwordEncoder.matches(req.actual(), u.getPasswordHash())) {
            throw new BusinessRuleException("CONTRASENA_ACTUAL_INCORRECTA", "La contraseña actual no es correcta.");
        }
        if (req.actual().equals(req.nueva())) {
            throw new BusinessRuleException("CONTRASENA_IGUAL", "La nueva contraseña debe ser distinta de la actual.");
        }
        u.setPasswordHash(passwordEncoder.encode(req.nueva()));
        usuarioRepository.save(u);
    }

    /**
     * Baja voluntaria de la propia cuenta (cliente o repartidor). Corre bajo {@code h2togo_sistema}
     * porque los roles de cliente/repartidor no tienen GRANT UPDATE sobre {@code cuenta_activa},
     * {@code motivo_baja}, {@code fecha_baja}, {@code correo} ni {@code telefono}.
     */
    public void eliminarCuenta(int idUsuario, EliminarCuentaRequest req) {
        record AvisoRuta(int idRepartidor, int idPedido) {}

        Supplier<List<AvisoRuta>> trabajo = () -> {
            Usuario u = cargar(idUsuario);
            if (u.getRol() == RolUsuario.admin) {
                throw new BusinessRuleException("OPERACION_NO_PERMITIDA",
                        "Las cuentas de administrador no pueden eliminarse por este medio.");
            }
            if (!passwordEncoder.matches(req.password(), u.getPasswordHash())) {
                throw new BusinessRuleException("CONTRASENA_ACTUAL_INCORRECTA",
                        "La contraseña actual no es correcta.");
            }

            List<AvisoRuta> avisos = new ArrayList<>();
            var idParam = new MapSqlParameterSource("id", idUsuario);

            if (u.getRol() == RolUsuario.cliente) {
                if (jdbc != null) {
                    Integer enCurso = jdbc.queryForObject("""
                            SELECT COUNT(*) FROM pedidos
                            WHERE id_cliente = :id AND estado_actual IN ('asignado', 'en_camino')
                            """, idParam, Integer.class);
                    if (enCurso != null && enCurso > 0) {
                        throw new BusinessRuleException("PEDIDOS_EN_CURSO",
                                "No puedes eliminar tu cuenta mientras tengas un pedido asignado o en camino. Espera a que concluya la entrega.");
                    }

                    List<Map<String, Object>> pendientes = jdbc.queryForList("""
                            SELECT id_pedido, id_repartidor FROM pedidos
                            WHERE id_cliente = :id AND estado_actual IN ('pendiente', 'pendiente_programado')
                            FOR UPDATE
                            """, idParam);
                    for (Map<String, Object> fila : pendientes) {
                        int idPedido = ((Number) fila.get("id_pedido")).intValue();
                        var pParam = new MapSqlParameterSource("p", idPedido);
                        jdbc.queryForList("SELECT fn_liberar_apartados(:p)", pParam);
                        jdbc.update("""
                                UPDATE pedidos SET estado_actual = 'cancelado'::estado_pedido
                                WHERE id_pedido = :p
                                """, pParam);
                        jdbc.update("""
                                INSERT INTO historial_estados_pedido (id_pedido, estado, notas_adicionales)
                                VALUES (:p, 'cancelado'::estado_pedido, 'Cancelado por eliminación de cuenta del cliente')
                                """, pParam);
                        Object idRepObj = fila.get("id_repartidor");
                        if (idRepObj instanceof Number n) {
                            avisos.add(new AvisoRuta(n.intValue(), idPedido));
                        }
                    }

                    jdbc.update("""
                            UPDATE direcciones_clientes SET activo = FALSE
                            WHERE id_cliente = :id AND activo = TRUE
                            """, idParam);
                }
            } else if (u.getRol() == RolUsuario.repartidor) {
                if (jdbc != null) {
                    Integer enCurso = jdbc.queryForObject("""
                            SELECT COUNT(*) FROM pedidos
                            WHERE id_repartidor = :id AND estado_actual IN ('asignado', 'en_camino')
                            """, idParam, Integer.class);
                    if (enCurso != null && enCurso > 0) {
                        throw new BusinessRuleException("PEDIDOS_EN_CURSO",
                                "No puedes eliminar tu cuenta mientras tengas entregas asignadas o en camino. Finalízalas primero.");
                    }
                }

                Repartidor rep = repartidorRepository.findById(idUsuario).orElse(null);
                if (rep != null && rep.getIdVehiculoActual() != null && jdbc != null) {
                    Integer garrafonesVehiculo = jdbc.queryForObject("""
                            SELECT COALESCE(SUM(cantidad_actual), 0) FROM inventario_vehiculo
                            WHERE id_vehiculo = :veh
                            """, new MapSqlParameterSource("veh", rep.getIdVehiculoActual()), Integer.class);
                    if (garrafonesVehiculo != null && garrafonesVehiculo > 0) {
                        throw new BusinessRuleException("VEHICULO_CON_INVENTARIO",
                                "Devuelve primero los garrafones de tu vehículo al almacén base antes de eliminar tu cuenta.");
                    }
                }

                if (rep != null && rep.isEsDueno()) {
                    resolverPropiedadAlEliminar(rep);
                }

                if (jdbc != null) {
                    List<Integer> reservados = jdbc.queryForList("""
                            SELECT id_pedido FROM pedidos
                            WHERE id_repartidor = :id AND estado_actual = 'pendiente_programado'
                            FOR UPDATE
                            """, idParam, Integer.class);
                    for (Integer idPedido : reservados) {
                        var pParam = new MapSqlParameterSource("p", idPedido);
                        jdbc.queryForList("SELECT fn_liberar_apartados(:p)", pParam);
                        jdbc.update("UPDATE pedidos SET id_repartidor = NULL WHERE id_pedido = :p", pParam);
                    }

                    jdbc.update("""
                            UPDATE repartidores
                            SET estado_operativo = FALSE,
                                id_vehiculo_actual = NULL,
                                ubicacion_actual = NULL,
                                ubicacion_reportada_en = NULL
                            WHERE id_usuario = :id
                            """, idParam);
                } else if (rep != null) {
                    rep.setEstadoOperativo(false);
                    rep.setIdVehiculoActual(null);
                    rep.setUbicacionReportadaEn(null);
                    repartidorRepository.save(rep);
                }
            }

            OffsetDateTime ahora = OffsetDateTime.now();
            long ts = ahora.toEpochSecond();
            u.setCorreo("eliminado." + idUsuario + "." + ts + "@baja.local");
            u.setTelefono("baja-" + idUsuario + "-" + (ts % 1_000_000));
            u.setCuentaActiva(false);
            u.setMotivoBaja("Cuenta eliminada por el usuario");
            u.setFechaBaja(ahora);
            u.setCodigoVerificacion(null);
            u.setCodigoVerificacionExpiracion(null);
            u.setCodigoIntentos((short) 0);
            u.setTokenSesionHash(null);
            u.setTokenFcm(null);
            u.setSesionFechaCreacion(null);
            u.setSesionFechaExpiracion(null);
            usuarioRepository.save(u);

            return avisos;
        };

        List<AvisoRuta> avisos = tareaSistema != null ? tareaSistema.consultar(trabajo) : trabajo.get();
        if (pushService != null) {
            for (AvisoRuta aviso : avisos) {
                pushService.notificar(aviso.idRepartidor(), "Pedido cancelado",
                        "El cliente canceló el pedido #" + aviso.idPedido() + ". Recalculamos tu ruta.",
                        Map.of("tipo", "ruta_recalculada", "idPedido", String.valueOf(aviso.idPedido())));
            }
        }
    }

    /**
     * RN-024 + fn_valida_dueno_unico: todo negocio conserva exactamente un dueño. Si hay otro
     * repartidor activo y aprobado en el mismo negocio se le transfiere la titularidad; de lo
     * contrario el negocio queda inactivo conservando el vínculo histórico.
     */
    private void resolverPropiedadAlEliminar(Repartidor saliente) {
        Integer idSucesor = null;
        if (jdbc != null) {
            List<Integer> candidatos = jdbc.queryForList("""
                    SELECT r.id_usuario FROM repartidores r
                    JOIN usuarios u ON u.id_usuario = r.id_usuario
                    WHERE r.id_negocio = :neg
                      AND r.id_usuario <> :id
                      AND r.aprobado = TRUE
                      AND u.cuenta_activa = TRUE
                    ORDER BY r.id_usuario ASC
                    LIMIT 1
                    """, new MapSqlParameterSource()
                            .addValue("neg", saliente.getIdNegocio())
                            .addValue("id", saliente.getId()),
                    Integer.class);
            if (!candidatos.isEmpty()) {
                idSucesor = candidatos.get(0);
            }
        }
        if (idSucesor != null) {
            Repartidor nuevo = repartidorRepository.findById(idSucesor).orElse(null);
            if (nuevo != null) {
                saliente.setEsDueno(false);
                repartidorRepository.saveAndFlush(saliente);
                nuevo.setEsDueno(true);
                nuevo.setAprobado(true);
                repartidorRepository.save(nuevo);
                return;
            }
        }
        if (negocioRepository != null) {
            negocioRepository.findById(saliente.getIdNegocio()).ifPresent(negocio -> {
                negocio.setActivo(false);
                negocioRepository.save(negocio);
            });
        }
    }

    private boolean esDueno(Repartidor r) {
        return r.isEsDueno();
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

    @Transactional
    public void registrarDispositivo(int idUsuario, String tokenFcm) {
        Usuario u = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new NotFoundException("USUARIO_NO_ENCONTRADO", "Usuario no encontrado."));
        u.setTokenFcm(tokenFcm);
        usuarioRepository.save(u);
    }
}
