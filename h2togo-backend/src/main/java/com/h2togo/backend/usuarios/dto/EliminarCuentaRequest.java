package com.h2togo.backend.usuarios.dto;

import jakarta.validation.constraints.NotBlank;

/** Confirmación de contraseña para que el titular elimine su propia cuenta. */
public record EliminarCuentaRequest(
        @NotBlank(message = "Ingresa tu contraseña actual para confirmar.") String password
) {}
