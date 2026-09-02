package com.h2togo.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Inicio de sesión (CU-002). {@code tokenFcm} es el token del dispositivo para push.
 * {@code forzar=true} cierra la sesión activa en otro dispositivo (CU-002 S1, RN-018).
 */
public record LoginRequest(
        @NotBlank @Email String correo,
        @NotBlank String password,
        String tokenFcm,
        // Boxed y opcional: el cliente puede omitirlo (se interpreta como false).
        // Un boolean primitivo rompería con FAIL_ON_NULL_FOR_PRIMITIVES si se omite.
        Boolean forzar
) {

    /** {@code true} solo si el cliente lo pidió explícitamente. */
    public boolean forzarSesion() {
        return Boolean.TRUE.equals(forzar);
    }
}
