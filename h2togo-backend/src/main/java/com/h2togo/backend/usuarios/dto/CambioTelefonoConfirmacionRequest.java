package com.h2togo.backend.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Paso 2 del cambio de teléfono con sesión iniciada: el número nuevo y el código recibido por SMS. */
public record CambioTelefonoConfirmacionRequest(
        @NotBlank @Pattern(regexp = "\\d{10}", message = "El teléfono debe tener 10 dígitos.") String telefono,
        @NotBlank String codigo
) {
}
