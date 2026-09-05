package com.h2togo.backend.inventario.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Registro de una entrada de proveedor a la base (CU-018). RN-030 valida la caducidad en el servicio. */
public record LoteEntradaRequest(
        @NotNull Integer idMarca,
        @NotNull @Positive Integer cantidad,
        @NotNull LocalDate fechaCaducidad,
        BigDecimal costoUnitario,
        String proveedor,
        String notas
) {
}
