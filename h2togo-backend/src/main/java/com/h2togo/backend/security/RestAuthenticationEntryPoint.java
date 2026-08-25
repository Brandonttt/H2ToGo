package com.h2togo.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/** Responde 401 con {@code ApiError} cuando falta autenticación válida (token ausente/expirado). */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        JsonErrorWriter.write(response, 401, "NO_AUTENTICADO",
                "Se requiere autenticación para acceder a este recurso.");
    }
}
