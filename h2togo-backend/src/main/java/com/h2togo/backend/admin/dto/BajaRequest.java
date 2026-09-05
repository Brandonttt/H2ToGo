package com.h2togo.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Baja lógica de un usuario (CU-015). {@code idNuevoDueno} transfiere la propiedad del
 * negocio si el usuario es su dueño (RN-024); si no se indica, el negocio queda inactivo.
 */
public record BajaRequest(
        @NotBlank String motivo,
        Integer idNuevoDueno
) {
}
