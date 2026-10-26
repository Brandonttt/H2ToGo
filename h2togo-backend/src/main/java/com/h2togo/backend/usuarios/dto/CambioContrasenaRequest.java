package com.h2togo.backend.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cambio de contraseña con sesión iniciada: pide la actual para confirmar que es el dueño de la cuenta. */
public record CambioContrasenaRequest(
        @NotBlank String actual,
        @NotBlank @Size(min = 8, max = 100) String nueva
) {
}
