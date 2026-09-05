package com.h2togo.backend.pedidos.dto;

import com.h2togo.backend.common.enums.TipoSolicitudPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Alta de pedido (CU-004). {@code idNegocio} es obligatorio en modalidad directa;
 * {@code precioMaximoGarrafon} en abierta (se validan en el servicio). Si {@code programado}
 * viene con fecha futura, el pedido queda en {@code pendiente_programado} (RN-027).
 */
public record PedidoCreateRequest(
        @NotNull TipoSolicitudPedido tipoSolicitud,
        Integer idNegocio,
        @Positive BigDecimal precioMaximoGarrafon,
        @NotNull Integer idDireccionEntrega,
        String indicaciones,
        @Valid Programado programado,
        @NotEmpty List<@Valid DetallePedidoRequest> detalles
) {

    /** Programación opcional del pedido a una fecha/hora futura. */
    public record Programado(@NotNull OffsetDateTime fechaProgramada) {
    }
}
