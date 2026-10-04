package com.h2togo.backend.admin;

import com.h2togo.backend.admin.dto.PedidoAdminFila;
import com.h2togo.backend.common.PagedResponse;
import com.h2togo.backend.common.enums.EstadoPedido;
import com.h2togo.backend.common.enums.TipoSolicitudPedido;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Historial global de pedidos con filtros combinables (CU-016, RN-016). Solo ADMIN. */
@Service
public class AdminPedidosService {

    private static final int TAM_MAX = 100;

    private static final String FROM = " " + """
            FROM pedidos p
            JOIN usuarios uc         ON uc.id_usuario = p.id_cliente
            LEFT JOIN usuarios ur    ON ur.id_usuario = p.id_repartidor
            LEFT JOIN repartidores r ON r.id_usuario = p.id_repartidor
            LEFT JOIN negocios n     ON n.id_negocio = COALESCE(p.id_negocio_solicitado, r.id_negocio)""";

    private final NamedParameterJdbcTemplate jdbc;

    public AdminPedidosService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * {@code cliente}/{@code repartidor} son texto libre: coinciden con el id exacto o con parte
     * del nombre completo, para que el panel pueda buscar sin conocer los ids.
     */
    @Transactional(readOnly = true)
    public PagedResponse<PedidoAdminFila> historial(OffsetDateTime desde, OffsetDateTime hasta,
            EstadoPedido estado, Integer idCliente, Integer idRepartidor, Integer idNegocio,
            String cliente, String repartidor, Pageable pageable) {

        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        var params = new MapSqlParameterSource();
        if (desde != null) {
            where.append(" AND p.fecha_creacion >= :desde");
            params.addValue("desde", desde);
        }
        if (hasta != null) {
            where.append(" AND p.fecha_creacion <= :hasta");
            params.addValue("hasta", hasta);
        }
        if (estado != null) {
            where.append(" AND p.estado_actual = CAST(:estado AS estado_pedido)");
            params.addValue("estado", estado.name());
        }
        if (idCliente != null) {
            where.append(" AND p.id_cliente = :cli");
            params.addValue("cli", idCliente);
        }
        if (idRepartidor != null) {
            where.append(" AND p.id_repartidor = :rep");
            params.addValue("rep", idRepartidor);
        }
        if (idNegocio != null) {
            where.append(" AND p.id_negocio_solicitado = :neg");
            params.addValue("neg", idNegocio);
        }
        if (cliente != null && !cliente.isBlank()) {
            where.append(" AND (CAST(uc.id_usuario AS text) = :cliTxt OR uc.nombre || ' ' || uc.apellidos ILIKE :cliLike)");
            params.addValue("cliTxt", cliente.trim()).addValue("cliLike", "%" + cliente.trim() + "%");
        }
        if (repartidor != null && !repartidor.isBlank()) {
            where.append(" AND (CAST(ur.id_usuario AS text) = :repTxt OR ur.nombre || ' ' || ur.apellidos ILIKE :repLike)");
            params.addValue("repTxt", repartidor.trim()).addValue("repLike", "%" + repartidor.trim() + "%");
        }

        int total = jdbc.queryForObject("SELECT COUNT(*)" + FROM + where, params, Integer.class);
        int size = Math.min(pageable.getPageSize(), TAM_MAX);
        int page = pageable.getPageNumber();
        params.addValue("size", size).addValue("offset", (long) page * size);

        List<PedidoAdminFila> contenido = jdbc.query("""
                SELECT p.id_pedido, p.estado_actual::text AS estado, p.tipo_solicitud::text AS tipo,
                       p.total_pagar, p.garrafones_totales, p.es_programado, p.fecha_creacion, p.fecha_entrega,
                       p.id_cliente, uc.nombre || ' ' || uc.apellidos AS cliente,
                       p.id_repartidor, ur.nombre || ' ' || ur.apellidos AS repartidor,
                       n.id_negocio, n.nombre_comercial""" + FROM + where
                + " ORDER BY p.fecha_creacion DESC LIMIT :size OFFSET :offset",
                params, (rs, i) -> new PedidoAdminFila(
                        rs.getInt("id_pedido"),
                        EstadoPedido.valueOf(rs.getString("estado")),
                        TipoSolicitudPedido.valueOf(rs.getString("tipo")),
                        rs.getBigDecimal("total_pagar"), rs.getInt("garrafones_totales"),
                        rs.getBoolean("es_programado"),
                        rs.getObject("fecha_creacion", OffsetDateTime.class),
                        rs.getObject("fecha_entrega", OffsetDateTime.class),
                        rs.getInt("id_cliente"), rs.getString("cliente"),
                        (Integer) rs.getObject("id_repartidor"), rs.getString("repartidor"),
                        (Integer) rs.getObject("id_negocio"), rs.getString("nombre_comercial")));

        int totalPaginas = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        boolean ultima = page >= totalPaginas - 1;
        return new PagedResponse<>(contenido, page, size, total, totalPaginas, ultima);
    }
}
