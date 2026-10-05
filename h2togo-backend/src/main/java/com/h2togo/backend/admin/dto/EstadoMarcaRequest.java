package com.h2togo.backend.admin.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Petición para activar o desactivar una marca global.
 */
public record EstadoMarcaRequest(
        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo
) {
}
