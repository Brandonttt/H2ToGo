package com.h2togo.backend.usuarios.dto;

import jakarta.validation.constraints.Size;

/**
 * Edición del perfil propio (PATCH /usuarios/me): nombre, apellidos y foto. Campos
 * nulos = no se modifican. El cliente edita directo; los cambios del negocio del dueño
 * van por solicitudes (F10). Correo/teléfono no se editan aquí (afectan identidad/sesión).
 */
public record PerfilUpdateRequest(
        @Size(min = 1, max = 100) String nombre,
        @Size(min = 1, max = 100) String apellidos,
        @Size(max = 500) String urlFotoPerfil
) {
}
