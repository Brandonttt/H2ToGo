package com.h2togo.backend.admin;

import com.h2togo.backend.admin.dto.NegocioAdminFila;
import com.h2togo.backend.admin.dto.SolicitudAdminFila;
import com.h2togo.backend.admin.dto.UsuarioAdminFila;
import com.h2togo.backend.common.PagedResponse;
import com.h2togo.backend.common.enums.CodigoCambioPerfil;
import com.h2togo.backend.common.enums.EstadoSolicitud;
import com.h2togo.backend.common.enums.RolUsuario;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Listados de solo lectura del panel de administración (usuarios, negocios, solicitudes). Solo ADMIN. */
@Service
public class AdminConsultasService {

    private static final int TAM_MAX = 100;

    private final NamedParameterJdbcTemplate jdbc;

    public AdminConsultasService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Usuarios con filtros combinables: rol, activo, negocio y texto libre (nombre, apellidos, correo o teléfono). */
    @Transactional(readOnly = true)
    public PagedResponse<UsuarioAdminFila> usuarios(RolUsuario rol, Boolean activo, Integer idNegocio, String q,
            Pageable pageable) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        var params = new MapSqlParameterSource();
        if (rol != null) {
            where.append(" AND u.rol = CAST(:rol AS rol_usuario)");
            params.addValue("rol", rol.name());
        }
        if (activo != null) {
            where.append(" AND u.cuenta_activa = :activo");
            params.addValue("activo", activo);
        }
        if (idNegocio != null) {
            where.append(" AND u.id_usuario IN (SELECT id_usuario FROM repartidores WHERE id_negocio = :neg)");
            params.addValue("neg", idNegocio);
        }
        if (q != null && !q.isBlank()) {
            where.append(" AND (u.nombre || ' ' || u.apellidos ILIKE :q OR u.correo ILIKE :q"
                    + " OR u.telefono ILIKE :q OR CAST(u.id_usuario AS text) = :qExacto)");
            params.addValue("q", "%" + q.trim() + "%").addValue("qExacto", q.trim());
        }

        long total = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios u" + where, params, Long.class);
        int size = Math.min(pageable.getPageSize(), TAM_MAX);
        int page = pageable.getPageNumber();
        params.addValue("size", size).addValue("offset", (long) page * size);

        List<UsuarioAdminFila> contenido = jdbc.query("""
                SELECT u.id_usuario, u.nombre, u.apellidos, u.correo, u.telefono, u.rol::text AS rol,
                       u.cuenta_activa, u.telefono_verificado, u.fecha_registro, u.motivo_baja, u.fecha_baja,
                       r.id_negocio, n.nombre_comercial, (n.id_dueno = u.id_usuario) AS es_dueno,
                       r.estado_operativo, c.suspendido_hasta,
                       CASE u.rol
                           WHEN 'cliente'    THEN (SELECT COUNT(*) FROM pedidos p WHERE p.id_cliente = u.id_usuario)
                           WHEN 'repartidor' THEN (SELECT COUNT(*) FROM pedidos p WHERE p.id_repartidor = u.id_usuario)
                           ELSE 0 END AS pedidos
                FROM usuarios u
                LEFT JOIN repartidores r ON r.id_usuario = u.id_usuario
                LEFT JOIN negocios n     ON n.id_negocio = r.id_negocio
                LEFT JOIN clientes c     ON c.id_usuario = u.id_usuario""" + where
                + " ORDER BY u.fecha_registro DESC, u.id_usuario DESC LIMIT :size OFFSET :offset",
                params, (rs, i) -> new UsuarioAdminFila(
                        rs.getInt("id_usuario"), rs.getString("nombre"), rs.getString("apellidos"),
                        rs.getString("correo"), rs.getString("telefono"), RolUsuario.valueOf(rs.getString("rol")),
                        rs.getBoolean("cuenta_activa"), rs.getBoolean("telefono_verificado"),
                        rs.getObject("fecha_registro", OffsetDateTime.class), rs.getString("motivo_baja"),
                        rs.getObject("fecha_baja", OffsetDateTime.class),
                        (Integer) rs.getObject("id_negocio"), rs.getString("nombre_comercial"),
                        rs.getBoolean("es_dueno"), (Boolean) rs.getObject("estado_operativo"),
                        rs.getObject("suspendido_hasta", OffsetDateTime.class), rs.getLong("pedidos")));

        int totalPaginas = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PagedResponse<>(contenido, page, size, total, totalPaginas, page >= totalPaginas - 1);
    }

    /** Conteos por rol/estado para las tarjetas del listado de usuarios. */
    @Transactional(readOnly = true)
    public Map<String, Object> resumenUsuarios() {
        return jdbc.queryForMap("""
                SELECT COUNT(*)                                                   AS total,
                       COUNT(*) FILTER (WHERE rol = 'cliente' AND cuenta_activa)    AS clientes,
                       COUNT(*) FILTER (WHERE rol = 'repartidor' AND cuenta_activa) AS repartidores,
                       COUNT(*) FILTER (WHERE rol = 'admin' AND cuenta_activa)      AS admins,
                       COUNT(*) FILTER (WHERE NOT cuenta_activa)                    AS inactivos,
                       COUNT(*) FILTER (WHERE fecha_registro >= now() - interval '7 days') AS nuevos7d
                FROM usuarios""", new MapSqlParameterSource());
    }

    /** Todos los negocios (activos e inactivos) con su dueño y número de repartidores. */
    @Transactional(readOnly = true)
    public List<NegocioAdminFila> negocios() {
        return jdbc.query("""
                SELECT n.id_negocio, n.nombre_comercial, n.activo, n.id_dueno, n.colonia,
                       u.nombre || ' ' || u.apellidos AS dueno,
                       (SELECT COUNT(*) FROM repartidores r WHERE r.id_negocio = n.id_negocio) AS repartidores
                FROM negocios n LEFT JOIN usuarios u ON u.id_usuario = n.id_dueno
                ORDER BY n.activo DESC, n.nombre_comercial""",
                (rs, i) -> new NegocioAdminFila(rs.getInt("id_negocio"), rs.getString("nombre_comercial"),
                        rs.getBoolean("activo"), (Integer) rs.getObject("id_dueno"), rs.getString("dueno"),
                        rs.getString("colonia"), rs.getLong("repartidores")));
    }

    /** Solicitudes de cambio de perfil; {@code estado} nulo devuelve todas. Más recientes primero. */
    @Transactional(readOnly = true)
    public List<SolicitudAdminFila> solicitudes(EstadoSolicitud estado) {
        var params = new MapSqlParameterSource();
        String where = "";
        if (estado != null) {
            where = " WHERE s.estado = CAST(:estado AS estado_solicitud)";
            params.addValue("estado", estado.name());
        }
        return jdbc.query("""
                SELECT s.id_solicitud, s.codigo_cambio::text AS codigo, s.id_negocio, n.nombre_comercial,
                       n.id_dueno, ud.nombre || ' ' || ud.apellidos AS solicitante,
                       s.id_vehiculo, s.id_producto_negocio, s.valor_anterior, s.valor_nuevo,
                       s.estado::text AS estado, s.comentario_admin,
                       ua.nombre || ' ' || ua.apellidos AS revisor,
                       s.fecha_solicitud, s.fecha_resolucion
                FROM solicitudes_cambio_perfil s
                JOIN negocios n       ON n.id_negocio = s.id_negocio
                LEFT JOIN usuarios ud ON ud.id_usuario = n.id_dueno
                LEFT JOIN usuarios ua ON ua.id_usuario = s.id_admin_revisor""" + where
                + " ORDER BY s.fecha_solicitud DESC LIMIT 500",
                params, (rs, i) -> new SolicitudAdminFila(
                        rs.getInt("id_solicitud"), CodigoCambioPerfil.valueOf(rs.getString("codigo")),
                        rs.getInt("id_negocio"), rs.getString("nombre_comercial"),
                        (Integer) rs.getObject("id_dueno"), rs.getString("solicitante"),
                        (Integer) rs.getObject("id_vehiculo"), (Integer) rs.getObject("id_producto_negocio"),
                        rs.getString("valor_anterior"), rs.getString("valor_nuevo"),
                        EstadoSolicitud.valueOf(rs.getString("estado")), rs.getString("comentario_admin"),
                        rs.getString("revisor"),
                        rs.getObject("fecha_solicitud", OffsetDateTime.class),
                        rs.getObject("fecha_resolucion", OffsetDateTime.class)));
    }
}
