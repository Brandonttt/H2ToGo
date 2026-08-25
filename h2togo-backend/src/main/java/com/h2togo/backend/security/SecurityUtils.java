package com.h2togo.backend.security;

import com.h2togo.backend.common.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Acceso al actor autenticado desde el contexto de seguridad. Los servicios lo
 * usan para resolver "el usuario actual" sin recibir su id del cliente (RNF-008).
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UsuarioPrincipal actual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioPrincipal p) {
            return p;
        }
        throw new UnauthorizedException("NO_AUTENTICADO", "No hay un usuario autenticado.");
    }

    public static Integer idActual() {
        return actual().id();
    }
}
