package com.h2togo.backend.admin.dto;

import com.h2togo.backend.common.enums.EstadoPedido;
import com.h2togo.backend.common.enums.TipoSolicitudPedido;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Fila del historial global de pedidos (CU-016). Mismos campos que {@code PedidoResumen} más
 * los nombres de cliente, repartidor y negocio para que el panel no haga N consultas extra.
 */
public record PedidoAdminFila(
        Integer id,
        EstadoPedido estado,
        TipoSolicitudPedido tipoSolicitud,
        BigDecimal totalPagar,
        Integer garrafonesTotales,
        boolean esProgramado,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaEntrega,
        Integer idCliente,
        String cliente,
        Integer idRepartidor,
        String repartidor,
        Integer idNegocio,
        String negocio
) {
}
