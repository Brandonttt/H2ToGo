package com.h2togo.backend.common;

/**
 * Credenciales inválidas o sesión no autenticada → 401 (§3.3). El mensaje es
 * genérico para no revelar si el correo existe (CU-002 E1, RN-016).
 */
public class UnauthorizedException extends RuntimeException {

    private final String codigo;

    public UnauthorizedException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
