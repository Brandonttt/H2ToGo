package com.h2togo.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Reenvío del código de verificación por SMS (CU-001, flujo alterno / excepción E2). */
public record ReenviarOtpRequest(
        @NotBlank @Email String correo
) {
}
