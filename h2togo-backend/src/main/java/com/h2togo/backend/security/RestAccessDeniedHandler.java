package com.h2togo.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Responde 403 con {@code ApiError} cuando el rol no autoriza el recurso (RNF-008). */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        JsonErrorWriter.write(response, 403, "ACCESO_DENEGADO",
                "No tiene permiso para esta operación.");
    }
}
