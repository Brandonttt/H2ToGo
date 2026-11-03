package com.h2togo.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Solicitud para retomar la verificación telefónica desde el login cuando una cuenta quedó
 * con {@code telefono_verificado = false}. Valida credenciales, genera un nuevo OTP y devuelve el teléfono.
 */
public record IniciarVerificacionRequest(
        @NotBlank @Email String correo,
        @NotBlank String password
) {
}
