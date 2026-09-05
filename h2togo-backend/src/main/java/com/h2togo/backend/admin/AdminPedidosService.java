package com.h2togo.backend.admin;

import com.h2togo.backend.common.PagedResponse;
import com.h2togo.backend.common.enums.EstadoPedido;
import com.h2togo.backend.common.enums.TipoSolicitudPedido;
import com.h2togo.backend.pedidos.dto.PedidoResumen;
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

    private final NamedParameterJdbcTemplate jdbc;

    public AdminPedidosService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public PagedResponse<PedidoResumen> historial(OffsetDateTime desde, OffsetDateTime hasta,
            EstadoPedido estado, Integer idCliente, Integer idRepartidor, Integer idNegocio, Pageable pageable) {

        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        var params = new MapSqlParameterSource();
        if (desde != null) {
            where.append(" AND fecha_creacion >= :desde");
            params.addValue("desde", desde);
        }
        if (hasta != null) {
            where.append(" AND fecha_creacion <= :hasta");
            params.addValue("hasta", hasta);
        }
        if (estado != null) {
            where.append(" AND estado_actual = CAST(:estado AS estado_pedido)");
            params.addValue("estado", estado.name());
        }
        if (idCliente != null) {
            where.append(" AND id_cliente = :cli");
            params.addValue("cli", idCliente);
        }
        if (idRepartidor != null) {
            where.append(" AND id_repartidor = :rep");
            params.addValue("rep", idRepartidor);
        }
        if (idNegocio != null) {
            where.append(" AND id_negocio_solicitado = :neg");
            params.addValue("neg", idNegocio);
        }

        int total = jdbc.queryForObject("SELECT COUNT(*) FROM pedidos" + where, params, Integer.class);
        int size = Math.min(pageable.getPageSize(), TAM_MAX);
        int page = pageable.getPageNumber();
        params.addValue("size", size).addValue("offset", (long) page * size);

        List<PedidoResumen> contenido = jdbc.query("""
                SELECT id_pedido, estado_actual::text, tipo_solicitud::text, total_pagar,
                       garrafones_totales, es_programado, fecha_creacion
                FROM pedidos""" + where + " ORDER BY fecha_creacion DESC LIMIT :size OFFSET :offset",
                params, (rs, n) -> new PedidoResumen(
                        rs.getInt("id_pedido"),
                        EstadoPedido.valueOf(rs.getString("estado_actual")),
                        TipoSolicitudPedido.valueOf(rs.getString("tipo_solicitud")),
                        rs.getBigDecimal("total_pagar"), rs.getInt("garrafones_totales"),
                        rs.getBoolean("es_programado"),
                        rs.getObject("fecha_creacion", OffsetDateTime.class)));

        int totalPaginas = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        boolean ultima = page >= totalPaginas - 1;
        return new PagedResponse<>(contenido, page, size, total, totalPaginas, ultima);
    }
}
