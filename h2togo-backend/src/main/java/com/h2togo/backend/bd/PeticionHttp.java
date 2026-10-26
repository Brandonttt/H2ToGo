package com.h2togo.backend.bd;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Marca el hilo mientras atiende una petición HTTP. {@link ActorBd} la usa para distinguir una
 * petición sin sesión ({@code h2togo_publico}) del trabajo interno del backend
 * ({@code h2togo_sistema}). Va antes que Spring Security, así cubre también la búsqueda del token.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PeticionHttp extends OncePerRequestFilter {

    private static final ThreadLocal<Boolean> EN_CURSO = ThreadLocal.withInitial(() -> Boolean.FALSE);

    static boolean enCurso() {
        return EN_CURSO.get();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        boolean previa = EN_CURSO.get();
        EN_CURSO.set(Boolean.TRUE);
        try {
            chain.doFilter(request, response);
        } finally {
            EN_CURSO.set(previa);
        }
    }
}
