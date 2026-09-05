package com.h2togo.backend.pedidos.dto;

import com.h2togo.backend.common.enums.EstadoPedido;
import com.h2togo.backend.common.enums.TipoSolicitudPedido;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Resumen de pedido para listados paginados (CU-007/013). */
public record PedidoResumen(
        Integer id,
        EstadoPedido estado,
        TipoSolicitudPedido tipoSolicitud,
        BigDecimal totalPagar,
        Integer garrafonesTotales,
        boolean esProgramado,
        OffsetDateTime fechaCreacion
) {
}
