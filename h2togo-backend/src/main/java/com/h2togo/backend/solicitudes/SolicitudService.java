package com.h2togo.backend.solicitudes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2togo.backend.common.BusinessRuleException;
import com.h2togo.backend.common.ConflictException;
import com.h2togo.backend.common.NotFoundException;
import com.h2togo.backend.common.enums.CodigoCambioPerfil;
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
        if (req.codigoCambio() == CodigoCambioPerfil.AGREGAR_PRODUCTO) {
            validarAgregarProducto(negocio.getId(), req.valorNuevo());
        } else if (req.codigoCambio() == CodigoCambioPerfil.AGREGAR_VEHICULO) {
            validarAgregarVehiculo(negocio.getId(), req.valorNuevo());
        } else if (req.codigoCambio() == CodigoCambioPerfil.DATOS_VEHICULO) {
            validarModificarVehiculo(negocio.getId(), req.idVehiculo(), req.valorNuevo());
        } else if (req.codigoCambio() == CodigoCambioPerfil.ELIMINAR_VEHICULO) {
            validarEliminarVehiculo(negocio.getId(), req.idVehiculo());
        } else if (req.codigoCambio() == CodigoCambioPerfil.NOMBRE_NEGOCIO) {
            validarNombreNegocio(negocio.getId(), req.valorNuevo());
        } else if (req.codigoCambio() == CodigoCambioPerfil.DIRECCION_BASE) {
            validarDireccionBase(negocio.getId(), req.valorNuevo());
        }

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

    /**
     * El dueño retira una solicitud que el admin aún no revisa. Se borra la fila: no hay estado
     * "cancelada" en el esquema y una solicitud pendiente todavía no cambió nada. Las de otro
     * negocio responden 404 para no revelar que existen.
     */
    @Transactional
    public void cancelar(int idRepartidor, int idSolicitud) {
        Negocio negocio = negocioComoDueno(idRepartidor); // RN-021
        SolicitudCambioPerfil s = solicitudRepository.findById(idSolicitud)
                .filter(x -> x.getIdNegocio().equals(negocio.getId()))
                .orElseThrow(() -> new NotFoundException("SOLICITUD_NO_ENCONTRADA", "Solicitud no encontrada."));
        if (s.getEstado() != EstadoSolicitud.pendiente) {
            throw new ConflictException("SOLICITUD_YA_RESUELTA", "La solicitud ya fue resuelta; no se puede cancelar.");
        }
        solicitudRepository.delete(s);
    }

    // ---------------------------------------------------------------- CU-021: resolver (admin)

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
                        colonia = :col, codigo_postal = :cp, referencias = :ref,
                        ubicacion_base = CASE
                            WHEN :lat::numeric IS NOT NULL AND :lon::numeric IS NOT NULL
                                THEN ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography
                            ELSE COALESCE(ubicacion_base, ST_SetSRID(ST_MakePoint(-99.1332, 19.4326), 4326)::geography)
                        END
                    WHERE id_negocio = :neg""",
                    new MapSqlParameterSource().addValue("calle", str(v, "calle")).addValue("ne", str(v, "numeroExterior"))
                            .addValue("ni", str(v, "numeroInterior")).addValue("col", str(v, "colonia"))
                            .addValue("cp", str(v, "codigoPostal")).addValue("ref", str(v, "referencias"))
                            .addValue("lon", dbl(v, "lon")).addValue("lat", dbl(v, "lat")).addValue("neg", idNegocio));
            case DATOS_VEHICULO -> {
                String rawTipo = str(v, "tipoVehiculo");
                var ps = new MapSqlParameterSource()
                        .addValue("marca", str(v, "marca"))
                        .addValue("modelo", str(v, "modelo"))
                        .addValue("color", str(v, "color"))
                        .addValue("placas", str(v, "placas"))
                        .addValue("cap", intg(v, "capacidadGarrafones"))
                        .addValue("veh", s.getIdVehiculo())
                        .addValue("neg", idNegocio);
                if (rawTipo != null && !rawTipo.isBlank()) {
                    ps.addValue("tipo", normalizarTipoVehiculo(rawTipo));
                    jdbc.update("""
                            UPDATE vehiculos_negocio SET
                                tipo_vehiculo = CAST(:tipo AS tipo_vehiculo),
                                marca = COALESCE(:marca, marca), modelo = COALESCE(:modelo, modelo),
                                color = COALESCE(:color, color), placas = COALESCE(:placas, placas),
                                capacidad_garrafones = COALESCE(:cap, capacidad_garrafones)
                            WHERE id_vehiculo = :veh AND id_negocio = :neg""", ps);
                } else {
                    jdbc.update("""
                            UPDATE vehiculos_negocio SET
                                marca = COALESCE(:marca, marca), modelo = COALESCE(:modelo, modelo),
                                color = COALESCE(:color, color), placas = COALESCE(:placas, placas),
                                capacidad_garrafones = COALESCE(:cap, capacidad_garrafones)
                            WHERE id_vehiculo = :veh AND id_negocio = :neg""", ps);
                }
            }
            case AGREGAR_VEHICULO -> jdbc.update("""
                    INSERT INTO vehiculos_negocio (id_negocio, tipo_vehiculo, marca, modelo, color, placas, capacidad_garrafones)
                    VALUES (:neg, CAST(:tipo AS tipo_vehiculo), :marca, :modelo, :color, :placas, :cap)""",
                    new MapSqlParameterSource().addValue("neg", idNegocio).addValue("tipo", normalizarTipoVehiculo(str(v, "tipoVehiculo")))
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

    /**
     * Valida al crear lo que el INSERT de la aprobación va a necesitar; si no, el error saldría
     * hasta que el admin aprueba. {@code valorNuevo} puede traer campos extra (p. ej. el nombre
     * de la marca para mostrarlo); solo se usan los que inserta {@link #aplicarCambio}.
     */
    private void validarAgregarProducto(int idNegocio, Map<String, Object> v) {
        Integer idMarca = v.get("idMarca") instanceof Number n ? n.intValue() : null;
        BigDecimal precio = v.get("precio") instanceof Number n ? new BigDecimal(n.toString()) : null;
        BigDecimal precioEnvase = v.get("precioEnvase") instanceof Number n ? new BigDecimal(n.toString()) : null;
        Integer capacidad = v.get("capacidadMaxima") instanceof Number n ? n.intValue() : null;
        if (idMarca == null || precio == null || precioEnvase == null || capacidad == null
                || precio.signum() <= 0 || precioEnvase.signum() < 0 || capacidad <= 0) {
            throw new BusinessRuleException("PRODUCTO_INVALIDO",
                    "Agregar producto requiere idMarca, precio (> 0), precioEnvase (≥ 0) y capacidadMaxima (> 0).");
        }
        var p = new MapSqlParameterSource().addValue("marca", idMarca).addValue("neg", idNegocio);
        Boolean marcaActiva = jdbc.query("SELECT activo FROM marcas WHERE id_marca = :marca", p,
                rs -> rs.next() ? rs.getBoolean(1) : null);
        if (!Boolean.TRUE.equals(marcaActiva)) {
            throw new NotFoundException("MARCA_NO_ENCONTRADA", "La marca no existe o está inactiva.");
        }
        Integer yaExiste = jdbc.queryForObject("""
                SELECT (SELECT COUNT(*) FROM productos_negocio WHERE id_negocio = :neg AND id_marca = :marca)
                     + (SELECT COUNT(*) FROM solicitudes_cambio_perfil
                        WHERE id_negocio = :neg AND estado = 'pendiente' AND codigo_cambio = 'AGREGAR_PRODUCTO'
                          AND (valor_nuevo::jsonb ->> 'idMarca')::int = :marca)""", p, Integer.class);
        if (yaExiste != null && yaExiste > 0) {
            throw new ConflictException("PRODUCTO_YA_EN_CATALOGO",
                    "Esa marca ya está en tu catálogo o tiene una solicitud pendiente.");
        }
    }

    private void validarAgregarVehiculo(int idNegocio, Map<String, Object> v) {
        String placas = str(v, "placas");
        String marca = str(v, "marca");
        String modelo = str(v, "modelo");
        Integer cap = intg(v, "capacidadGarrafones");

        if (placas == null || placas.isBlank() || marca == null || marca.isBlank()
                || modelo == null || modelo.isBlank() || cap == null || cap <= 0) {
            throw new BusinessRuleException("DATOS_VEHICULO_INVALIDOS",
                    "Agregar vehículo requiere marca, modelo, placas y capacidad mayor a 0.");
        }

        var p = new MapSqlParameterSource().addValue("placas", placas.trim().toUpperCase()).addValue("neg", idNegocio);
        Integer yaExiste = jdbc.queryForObject("""
                SELECT (SELECT COUNT(*) FROM vehiculos_negocio WHERE id_negocio = :neg AND UPPER(placas) = :placas AND activo = TRUE)
                     + (SELECT COUNT(*) FROM solicitudes_cambio_perfil
                        WHERE id_negocio = :neg AND estado = 'pendiente' AND codigo_cambio = 'AGREGAR_VEHICULO'
                          AND UPPER(valor_nuevo::jsonb ->> 'placas') = :placas)""", p, Integer.class);
        if (yaExiste != null && yaExiste > 0) {
            throw new ConflictException("VEHICULO_DUPLICADO",
                    "Ya existe un vehículo registrado con esas placas en tu negocio o tiene una solicitud pendiente.");
        }
    }

    private void validarModificarVehiculo(int idNegocio, Integer idVehiculo, Map<String, Object> v) {
        if (idVehiculo == null) {
            throw new BusinessRuleException("VEHICULO_REQUERIDO", "Se requiere el ID del vehículo a modificar.");
        }
        var p = new MapSqlParameterSource().addValue("veh", idVehiculo).addValue("neg", idNegocio);
        Boolean existe = jdbc.query("SELECT activo FROM vehiculos_negocio WHERE id_vehiculo = :veh AND id_negocio = :neg",
                p, rs -> rs.next() ? rs.getBoolean(1) : null);
        if (existe == null) {
            throw new NotFoundException("VEHICULO_NO_ENCONTRADO", "El vehículo no existe en tu negocio.");
        }

        String placas = str(v, "placas");
        if (placas != null && !placas.isBlank()) {
            var pPlacas = new MapSqlParameterSource().addValue("placas", placas.trim().toUpperCase())
                    .addValue("neg", idNegocio).addValue("veh", idVehiculo);
            Integer yaExiste = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM vehiculos_negocio
                    WHERE id_negocio = :neg AND UPPER(placas) = :placas AND id_vehiculo <> :veh AND activo = TRUE""",
                    pPlacas, Integer.class);
            if (yaExiste != null && yaExiste > 0) {
                throw new ConflictException("VEHICULO_DUPLICADO",
                        "Ya existe otro vehículo registrado con esas placas en tu negocio.");
            }
        }
    }

    private void validarEliminarVehiculo(int idNegocio, Integer idVehiculo) {
        if (idVehiculo == null) {
            throw new BusinessRuleException("VEHICULO_REQUERIDO", "Se requiere el ID del vehículo a eliminar.");
        }
        var p = new MapSqlParameterSource().addValue("veh", idVehiculo).addValue("neg", idNegocio);
        Boolean existe = jdbc.query("SELECT activo FROM vehiculos_negocio WHERE id_vehiculo = :veh AND id_negocio = :neg",
                p, rs -> rs.next() ? rs.getBoolean(1) : null);
        if (existe == null || !existe) {
            throw new NotFoundException("VEHICULO_NO_ENCONTRADO", "El vehículo no existe o ya está inactivo.");
        }
    }

    private void validarNombreNegocio(int idNegocio, Map<String, Object> v) {
        String nombre = str(v, "nombreComercial");
        if (nombre == null || nombre.trim().isBlank()) {
            throw new BusinessRuleException("NOMBRE_INVALIDO", "El nombre comercial no puede estar vacío.");
        }
        if (nombre.trim().length() > 150) {
            throw new BusinessRuleException("NOMBRE_INVALIDO", "El nombre comercial no puede exceder 150 caracteres.");
        }
        Integer yaPendiente = jdbc.queryForObject("""
                SELECT COUNT(*) FROM solicitudes_cambio_perfil
                WHERE id_negocio = :neg AND estado = 'pendiente' AND codigo_cambio = 'NOMBRE_NEGOCIO'""",
                new MapSqlParameterSource("neg", idNegocio), Integer.class);
        if (yaPendiente != null && yaPendiente > 0) {
            throw new ConflictException("SOLICITUD_PENDIENTE",
                    "Ya existe una solicitud pendiente para cambiar el nombre comercial.");
        }
    }

    private void validarDireccionBase(int idNegocio, Map<String, Object> v) {
        String calle = str(v, "calle");
        String numExt = str(v, "numeroExterior");
        String colonia = str(v, "colonia");
        String cp = str(v, "codigoPostal");
        if (calle == null || calle.trim().isBlank() || numExt == null || numExt.trim().isBlank()
                || colonia == null || colonia.trim().isBlank() || cp == null || cp.trim().isBlank()) {
            throw new BusinessRuleException("DIRECCION_INVALIDA",
                    "La dirección requiere calle, número exterior, colonia y código postal.");
        }
        Integer yaPendiente = jdbc.queryForObject("""
                SELECT COUNT(*) FROM solicitudes_cambio_perfil
                WHERE id_negocio = :neg AND estado = 'pendiente' AND codigo_cambio = 'DIRECCION_BASE'""",
                new MapSqlParameterSource("neg", idNegocio), Integer.class);
        if (yaPendiente != null && yaPendiente > 0) {
            throw new ConflictException("SOLICITUD_PENDIENTE",
                    "Ya existe una solicitud pendiente para cambiar la dirección de la base.");
        }
    }

    private String valorAnterior(SolicitudRequest req, Negocio negocio) {
        return switch (req.codigoCambio()) {
            case NOMBRE_NEGOCIO -> negocio.getNombreComercial();
            case FOTO_PERFIL -> jdbc.query(
                    "SELECT url_foto_perfil FROM usuarios WHERE id_usuario = :d",
                    new MapSqlParameterSource("d", negocio.getIdDueno()),
                    rs -> rs.next() ? rs.getString(1) : null);
            case DATOS_VEHICULO, ELIMINAR_VEHICULO -> req.idVehiculo() != null ? jdbc.query(
                    """
                    SELECT json_build_object(
                        'tipoVehiculo', tipo_vehiculo::text,
                        'marca', marca,
                        'modelo', modelo,
                        'color', color,
                        'placas', placas,
                        'capacidadGarrafones', capacidad_garrafones
                    )::text
                    FROM vehiculos_negocio
                    WHERE id_vehiculo = :v AND id_negocio = :n
                    """,
                    new MapSqlParameterSource("v", req.idVehiculo()).addValue("n", negocio.getId()),
                    rs -> rs.next() ? rs.getString(1) : null) : null;
            case DIRECCION_BASE -> jdbc.query(
                    """
                    SELECT json_build_object(
                        'calle', calle,
                        'numeroExterior', numero_exterior,
                        'numeroInterior', numero_interior,
                        'colonia', colonia,
                        'codigoPostal', codigo_postal,
                        'referencias', referencias
                    )::text
                    FROM negocios WHERE id_negocio = :n
                    """,
                    new MapSqlParameterSource("n", negocio.getId()),
                    rs -> rs.next() ? rs.getString(1) : null);
            default -> null;
        };
    }

    // ---------------------------------------------------------------- Helpers

    private static String normalizarTipoVehiculo(String tipo) {
        if (tipo == null || tipo.isBlank()) return "motocicleta";
        String t = tipo.trim().toLowerCase();
        if (t.contains("bici")) return "bicicleta_carga";
        if (t.contains("tri")) return "triciclo_carga";
        if (t.contains("camion")) return "camioneta";
        if (t.contains("auto") || t.contains("carr")) return "automovil";
        if (t.contains("moto")) return "motocicleta";
        try {
            return com.h2togo.backend.common.enums.TipoVehiculo.valueOf(t).name();
        } catch (IllegalArgumentException e) {
            return "motocicleta";
        }
    }

    private void notificarDueno(SolicitudCambioPerfil s) {
        try {
            Integer idDueno = negocioRepository.findById(s.getIdNegocio()).map(Negocio::getIdDueno).orElse(null);
            if (idDueno != null) {
                String resultado = s.getEstado() == EstadoSolicitud.aprobado ? "aprobada" : "rechazada";
                pushService.notificar(idDueno, "Solicitud " + resultado,
                        "Tu solicitud de cambio fue " + resultado
                                + (s.getComentarioAdmin() != null ? ": " + s.getComentarioAdmin() : "."),
                        Map.of("tipo", "solicitud_resuelta", "idSolicitud", String.valueOf(s.getId())));
            }
        } catch (Exception ignored) {
            // No bloquear la transacción si el servicio push falla
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
