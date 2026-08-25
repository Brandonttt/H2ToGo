package com.h2togo.backend.common;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Formato único de error de la API (§3.3). El campo {@code codigo} es legible por
 * la app móvil (p. ej. {@code RN-030_CADUCIDAD_INVALIDA}, {@code PEDIDO_YA_ASIGNADO}).
 */
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String codigo,
        String mensaje,
        List<String> detalles
) {
    public static ApiError of(int status, String codigo, String mensaje, List<String> detalles) {
        return new ApiError(OffsetDateTime.now(), status, codigo, mensaje,
                detalles == null ? List.of() : detalles);
    }

    public static ApiError of(int status, String codigo, String mensaje) {
        return of(status, codigo, mensaje, List.of());
    }
}
