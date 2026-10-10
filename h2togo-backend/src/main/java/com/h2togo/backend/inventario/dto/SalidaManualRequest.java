package com.h2togo.backend.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Registro de salida manual de la base (mermas, roturas, ajustes). */
public record SalidaManualRequest(
        @NotNull(message = "La marca es obligatoria")
        Integer idMarca,

        @Positive(message = "La cantidad debe ser mayor a 0")
        int cantidad,

        @NotBlank(message = "El motivo es obligatorio")
        String motivo
) {
}
