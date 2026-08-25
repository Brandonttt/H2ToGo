package com.h2togo.backend.common.enums;

/**
 * Rol del usuario (tipo ENUM nativo {@code rol_usuario} de v6).
 * <p>Los nombres de las constantes coinciden EXACTAMENTE con las etiquetas del
 * enum de PostgreSQL: Hibernate {@code @JdbcTypeCode(NAMED_ENUM)} mapea por
 * {@code name()} sin conversión de caso, y el esquema v6 las define en minúsculas.
 */
public enum RolUsuario {
    cliente,
    repartidor,
    admin
}
