package com.h2togo.backend.security;

import com.h2togo.backend.common.UnauthorizedException;
import com.h2togo.backend.common.enums.RolUsuario;
import org.springframework.security.access.AccessDeniedException;
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

    /** Exige que el actor tenga el rol dado (RNF-008); devuelve su id o lanza 403. */
    public static int exigirRol(RolUsuario rol) {
        UsuarioPrincipal p = actual();
        if (p.rol() != rol) {
            throw new AccessDeniedException("Operación permitida solo para el rol " + rol + ".");
        }
        return p.id();
    }
}
