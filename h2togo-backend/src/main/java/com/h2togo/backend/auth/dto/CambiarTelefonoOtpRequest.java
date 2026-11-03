package com.h2togo.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Cambio del número telefónico de una cuenta aún no verificada (CU-001, "Cambiar número").
 * Exige correo y contraseña para autenticar al titular antes de modificar el teléfono y reenviar el OTP.
 */
public record CambiarTelefonoOtpRequest(
        @NotBlank @Email String correo,
        @NotBlank String password,
        @NotBlank @Pattern(regexp = "\\d{10}", message = "El teléfono debe tener 10 dígitos.") String nuevoTelefono
) {
}
