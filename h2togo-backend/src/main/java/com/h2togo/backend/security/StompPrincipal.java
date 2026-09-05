package com.h2togo.backend.security;

import com.h2togo.backend.common.enums.RolUsuario;
import java.security.Principal;

/** Identidad del usuario en una sesión STOMP (WebSocket), montada por el interceptor de CONNECT. */
public record StompPrincipal(int idUsuario, RolUsuario rol) implements Principal {

    @Override
    public String getName() {
        return String.valueOf(idUsuario);
    }
}
