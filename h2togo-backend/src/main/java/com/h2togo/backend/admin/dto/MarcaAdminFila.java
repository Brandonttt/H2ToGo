package com.h2togo.backend.admin.dto;

/**
 * Fila de marca en el catálogo del panel de administración.
 */
public record MarcaAdminFila(
        Integer id,
        String nombre,
        boolean activo
) {
}
