package com.h2togo.backend.catalogo.dto;

import java.math.BigDecimal;

/**
 * Producto del catálogo de un negocio. {@code precio} = agua con envase propio (RN-025);
 * {@code precioEnvase} = adicional del envase (RN-031). {@code stockDisponible} = suma de
 * {@code (cantidad_actual - cantidad_apartada)} de lotes activos de la base para la marca.
 */
public record ProductoResponse(
        Integer idProductoNegocio,
        Integer idMarca,
        String marca,
        BigDecimal precio,
        BigDecimal precioEnvase,
        Integer capacidadMaxima,
        boolean activo,
        long stockDisponible
) {
}
