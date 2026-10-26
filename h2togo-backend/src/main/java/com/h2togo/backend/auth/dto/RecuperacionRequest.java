package com.h2togo.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** "¿Olvidaste tu contraseña?": pide un código por SMS al teléfono de la cuenta. */
public record RecuperacionRequest(
        @NotBlank @Email String correo
) {
}
