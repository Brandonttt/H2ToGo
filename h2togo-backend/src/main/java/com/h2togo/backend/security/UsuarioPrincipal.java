package com.h2togo.backend.security;

import com.h2togo.backend.common.enums.RolUsuario;

/**
 * Identidad del usuario autenticado que se monta en el contexto de seguridad a
 * partir del token (§F2). Nunca se recibe el id del propio usuario en el body:
 * el actor se resuelve del token (RNF-008, §2).
 */
public record UsuarioPrincipal(Integer id, RolUsuario rol) {

    /** Autoridad de Spring Security: {@code ROLE_CLIENTE}, {@code ROLE_REPARTIDOR}, {@code ROLE_ADMIN}. */
    public String authority() {
        return "ROLE_" + rol.name().toUpperCase();
    }
}
