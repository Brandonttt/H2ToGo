package com.h2togo.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Código recibido por SMS y la nueva contraseña (mismas reglas que en el registro). */
public record RecuperacionConfirmacionRequest(
        @NotBlank @Email String correo,
        @NotBlank String codigo,
        @NotBlank @Size(min = 8, max = 100) String nuevaContrasena
) {
}
