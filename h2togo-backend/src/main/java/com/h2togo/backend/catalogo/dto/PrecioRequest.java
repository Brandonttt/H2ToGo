package com.h2togo.backend.catalogo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/** Cambio de precio de un producto ya configurado (CU-023, RF-028: aplica de inmediato). */
public record PrecioRequest(
        @NotNull @PositiveOrZero BigDecimal precio,
        @NotNull @PositiveOrZero BigDecimal precioEnvase
) {
}
