package com.h2togo.backend.common;

/**
 * Violación de una regla de negocio (RN-xxx) → 422 (§3.3). El {@code codigo}
 * identifica la regla para la app móvil (p. ej. {@code RN-030_CADUCIDAD_INVALIDA}).
 */
public class BusinessRuleException extends RuntimeException {

    private final String codigo;

    public BusinessRuleException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
