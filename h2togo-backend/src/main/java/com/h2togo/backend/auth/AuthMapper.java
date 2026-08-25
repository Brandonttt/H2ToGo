package com.h2togo.backend.auth;

import com.h2togo.backend.auth.dto.PerfilResponse;
import com.h2togo.backend.usuarios.Usuario;

/** Mapeo estático a DTOs del módulo auth (§3.2, sin MapStruct). */
public final class AuthMapper {

    private AuthMapper() {
    }

    public static PerfilResponse toPerfil(Usuario u) {
        return new PerfilResponse(
                u.getId(), u.getNombre(), u.getApellidos(), u.getCorreo(),
                u.getTelefono(), u.getRol(), u.getUrlFotoPerfil());
    }
}
