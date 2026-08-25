package com.h2togo.backend.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * Escribe un cuerpo {@code ApiError} (§3.3) en respuestas generadas por los filtros
 * de seguridad (401/403), donde el {@code GlobalExceptionHandler} de MVC no aplica.
 * Serializa a mano para no depender de una versión concreta de Jackson.
 */
final class JsonErrorWriter {

    private JsonErrorWriter() {
    }

    static void write(HttpServletResponse response, int status, String codigo, String mensaje)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String body = "{"
                + "\"timestamp\":\"" + OffsetDateTime.now() + "\","
                + "\"status\":" + status + ","
                + "\"codigo\":\"" + esc(codigo) + "\","
                + "\"mensaje\":\"" + esc(mensaje) + "\","
                + "\"detalles\":[]"
                + "}";
        response.getWriter().write(body);
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
