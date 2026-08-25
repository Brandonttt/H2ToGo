package com.h2togo.backend.common;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Respuesta de listados paginados (§3.6). Envuelve {@link Page} de Spring Data
 * sin exponer su estructura interna al cliente.
 */
public record PagedResponse<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas,
        boolean ultima
) {
    public static <T> PagedResponse<T> of(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
