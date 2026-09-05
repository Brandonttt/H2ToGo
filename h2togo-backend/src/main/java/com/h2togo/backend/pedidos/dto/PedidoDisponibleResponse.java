package com.h2togo.backend.pedidos.dto;

import com.h2togo.backend.common.enums.TipoSolicitudPedido;
import java.math.BigDecimal;
import java.util.List;

/** Pedido disponible para el repartidor (CU-010), con distancia y desglose. */
public record PedidoDisponibleResponse(
        Integer idPedido,
        Double distanciaM,
        Integer garrafonesTotales,
        String colonia,
        TipoSolicitudPedido tipoSolicitud,
        BigDecimal totalEstimado,
        List<DetalleResponse> detalles
) {
}
