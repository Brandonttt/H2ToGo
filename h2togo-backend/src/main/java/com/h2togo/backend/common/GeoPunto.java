package com.h2togo.backend.common;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Punto geográfico lat/lon que viaja en los DTOs (§1: las columnas {@code geography}
 * NO se mapean como atributos JPA; lat/lon se pasan a las queries nativas de PostGIS).
 * SRID 4326 (WGS84).
 */
public record GeoPunto(
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double lat,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double lon
) {
}
