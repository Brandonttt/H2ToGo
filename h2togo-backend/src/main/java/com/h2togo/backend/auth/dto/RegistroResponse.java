package com.h2togo.backend.auth.dto;

/**
 * Resultado del registro (CU-001). No expone datos sensibles; confirma que se envió
 * el SMS de verificación y que la cuenta queda inactiva hasta verificar el teléfono.
 */
public record RegistroResponse(
        Integer idUsuario,
        String correo,
        String telefono,
        boolean telefonoVerificado,
        Integer idNegocio,
        String mensaje
) {
}
