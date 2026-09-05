package com.h2togo.backend.pedidos.dto;

import java.math.BigDecimal;

/** Línea de un pedido. Los precios son snapshot al momento del pedido (RN-025/031). */
public record DetalleResponse(
        Integer id,
        Integer idMarca,
        Integer cantidadSolicitada,
        Integer cantidadEntregada,
        boolean tieneEnvase,
        BigDecimal precioUnitario,
        BigDecimal precioEnvaseUnitario,
        boolean agregadaEnSitio
) {
}
