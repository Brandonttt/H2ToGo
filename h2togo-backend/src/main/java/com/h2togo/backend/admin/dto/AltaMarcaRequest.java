package com.h2togo.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Petición de alta de nueva marca en el catálogo global por el administrador.
 */
public record AltaMarcaRequest(
        @NotBlank(message = "El nombre de la marca es obligatorio")
        @Size(max = 100, message = "El nombre de la marca no puede superar los 100 caracteres")
        String nombre
) {
}
