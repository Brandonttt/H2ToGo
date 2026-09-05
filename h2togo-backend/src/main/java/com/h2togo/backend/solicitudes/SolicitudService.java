package com.h2togo.backend.solicitudes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2togo.backend.common.BusinessRuleException;
import com.h2togo.backend.common.ConflictException;
import com.h2togo.backend.common.NotFoundException;
import com.h2togo.backend.common.enums.EstadoSolicitud;
import com.h2togo.backend.negocios.Negocio;
import com.h2togo.backend.negocios.NegocioRepository;
import com.h2togo.backend.notificaciones.PushService;
import com.h2togo.backend.solicitudes.dto.ResolucionRequest;
import com.h2togo.backend.solicitudes.dto.ResolucionRequest.Decision;
import com.h2togo.backend.solicitudes.dto.SolicitudRequest;
import com.h2togo.backend.solicitudes.dto.SolicitudResponse;
import com.h2togo.backend.usuarios.RepartidorRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Solicitudes de cambio de perfil (CU-020/021). El dueño (RN-021) crea; el admin aprueba
 * (aplica el cambio en la misma transacción, RN-019) o rechaza (con comentario). El
 * {@code valor_nuevo} viaja como JSON; cada {@code codigo_cambio} tiene su aplicación (§10 #30).
 */
@Service
public class SolicitudService {

    private final SolicitudCambioPerfilRepository solicitudRepository;
    private final RepartidorRepository repartidorRepository;
    private final NegocioRepository negocioRepository;
    private final PushService pushService;
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();

    public SolicitudService(SolicitudCambioPerfilRepository solicitudRepository,
            RepartidorRepository repartidorRepository, NegocioRepository negocioRepository,
            PushService pushService, NamedParameterJdbcTemplate jdbc) {
        this.solicitudRepository = solicitudRepository;
        this.repartidorRepository = repartidorRepository;
        this.negocioRepository = negocioRepository;
        this.pushService = pushService;
        this.jdbc = jdbc;
    }

    // ---------------------------------------------------------------- CU-020: crear

    @Transactional
    public SolicitudResponse crear(int idRepartidor, SolicitudRequest req) {
        Negocio negocio = negocioComoDueno(idRepartidor); // RN-021

        SolicitudCambioPerfil s = new SolicitudCambioPerfil();
        s.setCodigoCambio(req.codigoCambio());
        s.setIdNegocio(negocio.getId());
        s.setIdVehiculo(req.idVehiculo());
        s.setIdProductoNegocio(req.idProductoNegocio());
        s.setValorAnterior(valorAnterior(req, negocio));
        s.setValorNuevo(toJson(req.valorNuevo()));
        s.setEstado(EstadoSolicitud.pendiente);
        solicitudRepository.save(s);
        return toResponse(s);
    }

    @Transactional(readOnly = true)
    public List<SolicitudResponse> misSolicitudes(int idRepartidor) {
        int idNegocio = negocioDelRepartidor(idRepartidor).getId();
        return solicitudRepository.findByIdNegocio(idNegocio).stream().map(SolicitudService::toResponse).toList();
    }

    // ---------------------------------------------------------------- CU-021: resolver (admin)

    @Transactional(readOnly = true)
    public List<SolicitudResponse> pendientes(EstadoSolicitud estado) {
        return solicitudRepository.findByEstado(estado == null ? EstadoSolicitud.pendiente : estado)
                .stream().map(SolicitudService::toResponse).toList();
    }

    @Transactional
    public SolicitudResponse resolver(int idAdmin, int idSolicitud, ResolucionRequest req) {
        SolicitudCambioPerfil s = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new NotFoundException("SOLICITUD_NO_ENCONTRADA", "Solicitud no encontrada."));
        if (s.getEstado() != EstadoSolicitud.pendiente) {
            throw new ConflictException("SOLICITUD_YA_RESUELTA", "La solicitud ya fue resuelta.");
        }

        if (req.decision() == Decision.RECHAZADO) {
            if (req.comentario() == null || req.comentario().isBlank()) {
                throw new BusinessRuleException("COMENTARIO_REQUERIDO", "Rechazar exige un comentario.");
            }
            s.setEstado(EstadoSolicitud.rechazado);
        } else {
            aplicarCambio(s); // RN-019: el cambio se efectiviza en esta misma transacción
            s.setEstado(EstadoSolicitud.aprobado);
        }
        s.setComentarioAdmin(req.comentario());
        s.setIdAdminRevisor(idAdmin);
        s.setFechaResolucion(OffsetDateTime.now());
        solicitudRepository.save(s);

        notificarDueno(s);
        return toResponse(s);
    }

    // ---------------------------------------------------------------- Aplicación del cambio

    private void aplicarCambio(SolicitudCambioPerfil s) {
        Map<String, Object> v = fromJson(s.getValorNuevo());
        int idNegocio = s.getIdNegocio();
        switch (s.getCodigoCambio()) {
            case NOMBRE_NEGOCIO -> jdbc.update(
                    "UPDATE negocios SET nombre_comercial = :val WHERE id_negocio = :neg",
                    new MapSqlParameterSource().addValue("val", str(v, "nombreComercial")).addValue("neg", idNegocio));
            case FOTO_PERFIL -> jdbc.update("""
                    UPDATE usuarios SET url_foto_perfil = :url
                    WHERE id_usuario = (SELECT id_dueno FROM negocios WHERE id_negocio = :neg)""",
                    new MapSqlParameterSource().addValue("url", str(v, "urlFotoPerfil")).addValue("neg", idNegocio));
            case DIRECCION_BASE -> jdbc.update("""
                    UPDATE negocios SET calle = :calle, numero_exterior = :ne, numero_interior = :ni,
                        colonia = :col, codigo_postal = :cp,
                        ubicacion_base = ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography
                    WHERE id_negocio = :neg""",
                    new MapSqlParameterSource().addValue("calle", str(v, "calle")).addValue("ne", str(v, "numeroExterior"))
                            .addValue("ni", str(v, "numeroInterior")).addValue("col", str(v, "colonia"))
                            .addValue("cp", str(v, "codigoPostal")).addValue("lon", dbl(v, "lon"))
                            .addValue("lat", dbl(v, "lat")).addValue("neg", idNegocio));
            case DATOS_VEHICULO -> jdbc.update("""
                    UPDATE vehiculos_negocio SET
                        marca = COALESCE(:marca, marca), modelo = COALESCE(:modelo, modelo),
                        color = COALESCE(:color, color), placas = COALESCE(:placas, placas),
                        capacidad_garrafones = COALESCE(:cap, capacidad_garrafones)
                    WHERE id_vehiculo = :veh AND id_negocio = :neg""",
                    new MapSqlParameterSource().addValue("marca", str(v, "marca")).addValue("modelo", str(v, "modelo"))
                            .addValue("color", str(v, "color")).addValue("placas", str(v, "placas"))
                            .addValue("cap", intg(v, "capacidadGarrafones")).addValue("veh", s.getIdVehiculo())
                            .addValue("neg", idNegocio));
            case AGREGAR_VEHICULO -> jdbc.update("""
                    INSERT INTO vehiculos_negocio (id_negocio, tipo_vehiculo, marca, modelo, color, placas, capacidad_garrafones)
                    VALUES (:neg, CAST(:tipo AS tipo_vehiculo), :marca, :modelo, :color, :placas, :cap)""",
                    new MapSqlParameterSource().addValue("neg", idNegocio).addValue("tipo", str(v, "tipoVehiculo"))
                            .addValue("marca", str(v, "marca")).addValue("modelo", str(v, "modelo"))
                            .addValue("color", str(v, "color")).addValue("placas", str(v, "placas"))
                            .addValue("cap", intg(v, "capacidadGarrafones")));
            case ELIMINAR_VEHICULO -> jdbc.update(
                    "UPDATE vehiculos_negocio SET activo = FALSE WHERE id_vehiculo = :veh AND id_negocio = :neg",
                    new MapSqlParameterSource().addValue("veh", s.getIdVehiculo()).addValue("neg", idNegocio));
            case AGREGAR_PRODUCTO -> jdbc.update("""
                    INSERT INTO productos_negocio (id_negocio, id_marca, precio, precio_envase, capacidad_maxima)
                    VALUES (:neg, :marca, :precio, :precioEnvase, :cap)""",
                    new MapSqlParameterSource().addValue("neg", idNegocio).addValue("marca", intg(v, "idMarca"))
                            .addValue("precio", bdec(v, "precio")).addValue("precioEnvase", bdec(v, "precioEnvase"))
                            .addValue("cap", intg(v, "capacidadMaxima")));
        }
    }

    private String valorAnterior(SolicitudRequest req, Negocio negocio) {
        return switch (req.codigoCambio()) {
            case NOMBRE_NEGOCIO -> negocio.getNombreComercial();
            case FOTO_PERFIL -> jdbc.query(
                    "SELECT url_foto_perfil FROM usuarios WHERE id_usuario = :d",
                    new MapSqlParameterSource("d", negocio.getIdDueno()),
                    rs -> rs.next() ? rs.getString(1) : null);
            default -> null;
        };
    }

    // ---------------------------------------------------------------- Helpers

    private void notificarDueno(SolicitudCambioPerfil s) {
        Integer idDueno = negocioRepository.findById(s.getIdNegocio()).map(Negocio::getIdDueno).orElse(null);
        if (idDueno != null) {
            pushService.notificar(idDueno, "Solicitud " + s.getEstado(),
                    "Tu solicitud de cambio fue " + s.getEstado()
                            + (s.getComentarioAdmin() != null ? ": " + s.getComentarioAdmin() : "."));
        }
    }

    private Negocio negocioDelRepartidor(int idRepartidor) {
        var r = repartidorRepository.findById(idRepartidor)
                .orElseThrow(() -> new NotFoundException("REPARTIDOR_NO_ENCONTRADO", "No es repartidor."));
        return negocioRepository.findById(r.getIdNegocio())
                .orElseThrow(() -> new NotFoundException("NEGOCIO_NO_ENCONTRADO", "Sin negocio."));
    }

    private Negocio negocioComoDueno(int idRepartidor) {
        Negocio negocio = negocioDelRepartidor(idRepartidor);
        if (!Integer.valueOf(idRepartidor).equals(negocio.getIdDueno())) {
            throw new AccessDeniedException("Solo el dueño puede solicitar cambios (RN-021).");
        }
        return negocio;
    }

    private String toJson(Map<String, Object> map) {
        try {
            return json.writeValueAsString(map);
        } catch (Exception e) {
            throw new BusinessRuleException("VALOR_NUEVO_INVALIDO", "No se pudo serializar el cambio.");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJson(String texto) {
        try {
            return json.readValue(texto, Map.class);
        } catch (Exception e) {
            throw new BusinessRuleException("VALOR_NUEVO_INVALIDO", "El valor del cambio no es válido.");
        }
    }

    private static String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : v.toString();
    }

    private static Integer intg(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : ((Number) v).intValue();
    }

    private static Double dbl(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : ((Number) v).doubleValue();
    }

    private static BigDecimal bdec(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : new BigDecimal(v.toString());
    }

    private static SolicitudResponse toResponse(SolicitudCambioPerfil s) {
        return new SolicitudResponse(s.getId(), s.getCodigoCambio(), s.getIdNegocio(), s.getIdVehiculo(),
                s.getIdProductoNegocio(), s.getValorAnterior(), s.getValorNuevo(), s.getEstado(),
                s.getComentarioAdmin(), s.getFechaSolicitud(), s.getFechaResolucion());
    }
}
