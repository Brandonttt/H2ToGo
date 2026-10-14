package com.h2togo.backend.pedidos.dto;

import com.h2togo.backend.common.enums.EstadoPedido;
import com.h2togo.backend.common.enums.TipoSolicitudPedido;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Resumen de pedido para listados paginados (CU-007/013): incluye cliente, dirección,
 * purificadora y repartidor para que el historial no muestre textos genéricos.
 */
public record PedidoResumen(
        Integer id,
        EstadoPedido estado,
        TipoSolicitudPedido tipoSolicitud,
        BigDecimal totalPagar,
        Integer garrafonesTotales,
        boolean esProgramado,
        OffsetDateTime fechaCreacion,
        String nombreCliente,
        String direccionTexto,
        String nombreNegocio,
        String nombreRepartidor
) {
    public PedidoResumen(
            Integer id,
            EstadoPedido estado,
            TipoSolicitudPedido tipoSolicitud,
            BigDecimal totalPagar,
            Integer garrafonesTotales,
            boolean esProgramado,
            OffsetDateTime fechaCreacion) {
        this(id, estado, tipoSolicitud, totalPagar, garrafonesTotales, esProgramado, fechaCreacion,
                null, null, null, null);
    }
}
