package com.h2togo.backend.auth.dto;

import com.h2togo.backend.common.enums.RolUsuario;

/** Datos comunes del usuario autenticado (subconjunto seguro, sin credenciales). */
public record PerfilResponse(
        Integer id,
        String nombre,
        String apellidos,
        String correo,
        String telefono,
        RolUsuario rol,
        String urlFotoPerfil
) {
}
