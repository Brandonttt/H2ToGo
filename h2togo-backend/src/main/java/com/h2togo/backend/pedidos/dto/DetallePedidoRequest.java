package com.h2togo.backend.pedidos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Una línea del pedido: marca, cantidad y si el cliente aporta envase propio (RN-031). */
public record DetallePedidoRequest(
        @NotNull Integer idMarca,
        @NotNull @Positive Integer cantidad,
        // Boxed y requerido: evita el fallo de booleanos primitivos omitidos (§10 #21).
        @NotNull Boolean tieneEnvase
) {
}
