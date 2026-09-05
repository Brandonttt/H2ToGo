package com.h2togo.backend.pedidos.dto;

import com.h2togo.backend.common.enums.EstadoPedido;
import java.time.OffsetDateTime;

/** Un cambio de estado del pedido (bitácora inmutable, RN-013). */
public record HistorialResponse(
        EstadoPedido estado,
        OffsetDateTime fechaCambio,
        String notas
) {
}
