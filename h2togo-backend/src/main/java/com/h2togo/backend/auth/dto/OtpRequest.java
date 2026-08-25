package com.h2togo.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Verificación del teléfono con el código OTP recibido por SMS (CU-001, RN-002). */
public record OtpRequest(
        @NotBlank @Email String correo,
        @NotBlank String codigo
) {
}
