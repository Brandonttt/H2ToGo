package com.h2togo.backend.repartidores;

import com.h2togo.backend.common.BusinessRuleException;
import com.h2togo.backend.common.NotFoundException;
import com.h2togo.backend.negocios.Negocio;
import com.h2togo.backend.negocios.NegocioRepository;
import com.h2togo.backend.notificaciones.PushService;
import com.h2togo.backend.repartidores.dto.MiembroNegocioResponse;
import com.h2togo.backend.usuarios.Repartidor;
import com.h2togo.backend.usuarios.RepartidorRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repartidores que se unen a una purificadora existente (CU-001): quedan pendientes hasta que el
 * dueño los aprueba. Mientras tanto no pueden iniciar jornada ni ver pedidos del negocio, porque
 * esos pedidos traen nombre, teléfono y domicilio de los clientes.
 */
@Service
public class MiembrosNegocioService {

    private final NamedParameterJdbcTemplate jdbc;
    private final RepartidorRepository repartidorRepository;
    private final NegocioRepository negocioRepository;
    private final PushService pushService;

    public MiembrosNegocioService(NamedParameterJdbcTemplate jdbc, RepartidorRepository repartidorRepository,
            NegocioRepository negocioRepository, PushService pushService) {
        this.jdbc = jdbc;
        this.repartidorRepository = repartidorRepository;
        this.negocioRepository = negocioRepository;
        this.pushService = pushService;
    }

    /** Lanza 422 PENDIENTE_APROBACION si el repartidor aún no fue aprobado por el dueño. */
    @Transactional(readOnly = true)
    public void exigirAprobado(int idRepartidor) {
        Boolean aprobado = jdbc.query("SELECT aprobado FROM repartidores WHERE id_usuario = :id",
                new MapSqlParameterSource("id", idRepartidor), rs -> rs.next() ? rs.getBoolean(1) : null);
        if (Boolean.FALSE.equals(aprobado)) {
            throw new BusinessRuleException("PENDIENTE_APROBACION",
                    "El dueño de la purificadora aún no aprueba tu solicitud para unirte.");
        }
    }

    /** Avisa al dueño que alguien pidió unirse (se llama cuando el solicitante verifica su teléfono). */
    public void avisarSolicitud(int idRepartidor, String nombreCompleto) {
        repartidorRepository.findById(idRepartidor)
                .filter(r -> !r.isAprobado())
                .flatMap(r -> negocioRepository.findById(r.getIdNegocio()))
                .ifPresent(n -> pushService.notificar(n.getIdDueno(), "Nueva solicitud de repartidor",
                        nombreCompleto + " quiere unirse a " + n.getNombreComercial() + ". Revísala en tu perfil.",
                        Map.of("tipo", "solicitud_repartidor", "idRepartidor", String.valueOf(idRepartidor))));
    }

    /** Repartidores del negocio del dueño, pendientes primero. */
    @Transactional(readOnly = true)
    public List<MiembroNegocioResponse> miembros(int idDueno) {
        Negocio negocio = negocioComoDueno(idDueno);
        return jdbc.query("""
                SELECT u.id_usuario, u.nombre, u.apellidos, u.correo, u.telefono, u.telefono_verificado,
                       u.fecha_registro, r.aprobado, r.estado_operativo
                FROM repartidores r JOIN usuarios u ON u.id_usuario = r.id_usuario
                WHERE r.id_negocio = :neg AND u.cuenta_activa
                ORDER BY r.aprobado, u.fecha_registro DESC""",
                new MapSqlParameterSource("neg", negocio.getId()),
                (rs, i) -> new MiembroNegocioResponse(
                        rs.getInt("id_usuario"), rs.getString("nombre"), rs.getString("apellidos"),
                        rs.getString("correo"), rs.getString("telefono"),
                        rs.getInt("id_usuario") == negocio.getIdDueno(),
                        rs.getBoolean("aprobado"), rs.getBoolean("estado_operativo"),
                        rs.getBoolean("telefono_verificado"),
                        rs.getObject("fecha_registro", OffsetDateTime.class)));
    }

    @Transactional
    public void aprobar(int idDueno, int idRepartidor) {
        Repartidor r = pendienteDelNegocio(idDueno, idRepartidor);
        r.setAprobado(true);
        repartidorRepository.save(r);
        pushService.notificar(idRepartidor, "Solicitud aprobada",
                "El dueño aprobó tu solicitud. Ya puedes iniciar tu jornada.",
                Map.of("tipo", "repartidor_aprobado"));
    }

    /**
     * Rechazar = dar de baja la cuenta recién creada (no tiene pedidos ni historial). Se usa baja
     * lógica, como en el panel de administración, para no borrar datos.
     */
    @Transactional
    public void rechazar(int idDueno, int idRepartidor) {
        pendienteDelNegocio(idDueno, idRepartidor);
        jdbc.update("""
                UPDATE usuarios SET cuenta_activa = FALSE, motivo_baja = 'Solicitud rechazada por el dueño',
                    fecha_baja = now(), token_sesion = NULL, token_fcm = NULL,
                    sesion_fecha_creacion = NULL, sesion_fecha_expiracion = NULL
                WHERE id_usuario = :id""", new MapSqlParameterSource("id", idRepartidor));
    }

    private Repartidor pendienteDelNegocio(int idDueno, int idRepartidor) {
        Negocio negocio = negocioComoDueno(idDueno);
        Repartidor r = repartidorRepository.findById(idRepartidor)
                .filter(x -> negocio.getId().equals(x.getIdNegocio()))
                .orElseThrow(() -> new NotFoundException("REPARTIDOR_NO_ENCONTRADO",
                        "Ese repartidor no pertenece a tu purificadora."));
        if (r.isAprobado()) {
            throw new BusinessRuleException("YA_APROBADO", "Ese repartidor ya está aprobado.");
        }
        return r;
    }

    private Negocio negocioComoDueno(int idRepartidor) {
        Repartidor r = repartidorRepository.findById(idRepartidor)
                .orElseThrow(() -> new NotFoundException("REPARTIDOR_NO_ENCONTRADO", "El usuario no es un repartidor."));
        Negocio negocio = negocioRepository.findById(r.getIdNegocio())
                .orElseThrow(() -> new NotFoundException("NEGOCIO_NO_ENCONTRADO", "Negocio no encontrado."));
        if (!Integer.valueOf(idRepartidor).equals(negocio.getIdDueno())) {
            throw new AccessDeniedException("Solo el dueño de la purificadora puede gestionar sus repartidores (RN-021).");
        }
        return negocio;
    }
}
