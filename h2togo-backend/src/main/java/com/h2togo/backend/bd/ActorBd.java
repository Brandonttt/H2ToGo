package com.h2togo.backend.bd;

import com.h2togo.backend.security.UsuarioPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Rol de PostgreSQL con el que trabaja una conexión ([S1] del esquema v7). Se decide por quién
 * hace la petición: un usuario con sesión usa el rol de su actor; una petición sin sesión (login,
 * registro, catálogo), {@code h2togo_publico}; y lo que corre fuera de una petición (tareas
 * programadas, RabbitMQ, WebSocket) o dentro de {@link TareaSistema}, {@code h2togo_sistema}.
 */
record ActorBd(String rol, Integer idUsuario) {

    static final ActorBd PUBLICO = new ActorBd("h2togo_publico", null);
    static final ActorBd SISTEMA = new ActorBd("h2togo_sistema", null);

    static ActorBd actual() {
        if (TareaSistema.activa()) {
            return SISTEMA;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioPrincipal p) {
            return new ActorBd("h2togo_" + p.rol().name(), p.id());
        }
        return PeticionHttp.enCurso() ? PUBLICO : SISTEMA;
    }
}
