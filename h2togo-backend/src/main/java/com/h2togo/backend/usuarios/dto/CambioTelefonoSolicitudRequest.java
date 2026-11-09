package com.h2togo.backend.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Paso 1 del cambio de teléfono con sesión iniciada: el número nuevo al que se envía el código. */
public record CambioTelefonoSolicitudRequest(
        @NotBlank @Pattern(regexp = "\\d{10}", message = "El teléfono debe tener 10 dígitos.") String telefono
) {
}
