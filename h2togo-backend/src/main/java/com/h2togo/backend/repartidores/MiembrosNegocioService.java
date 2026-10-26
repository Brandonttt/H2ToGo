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
                       u.fecha_registro, r.es_dueno, r.aprobado, r.estado_operativo
                FROM repartidores r JOIN usuarios u ON u.id_usuario = r.id_usuario
                WHERE r.id_negocio = :neg AND u.cuenta_activa
                ORDER BY r.aprobado, u.fecha_registro DESC""",
                new MapSqlParameterSource("neg", negocio.getId()),
                (rs, i) -> new MiembroNegocioResponse(
                        rs.getInt("id_usuario"), rs.getString("nombre"), rs.getString("apellidos"),
                        rs.getString("correo"), rs.getString("telefono"),
                        rs.getBoolean("es_dueno"),
                        rs.getBoolean("aprobado"), rs.getBoolean("estado_operativo"),
                        rs.getBoolean("telefono_verificado"),
                        rs.getObject("fecha_registro", OffsetDateTime.class)));
    }

    /**
     * Aprobar y rechazar cambian filas de otro usuario, algo que el rol del repartidor no puede
     * hacer en la BD: lo hacen las funciones {@code fn_aprobar_repartidor} y
     * {@code fn_rechazar_repartidor}, que vuelven a validar que quien llama es el dueño.
     */
    @Transactional
    public void aprobar(int idDueno, int idRepartidor) {
        pendienteDelNegocio(idDueno, idRepartidor);
        jdbc.queryForList("SELECT fn_aprobar_repartidor(:id)", new MapSqlParameterSource("id", idRepartidor));
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
        jdbc.queryForList("SELECT fn_rechazar_repartidor(:id)", new MapSqlParameterSource("id", idRepartidor));
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
