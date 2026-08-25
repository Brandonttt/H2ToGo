package com.h2togo.backend.common;

/** Conflicto de estado (duplicados, sesión activa, pedido ya asignado) → 409 (§3.3). */
public class ConflictException extends RuntimeException {

    private final String codigo;

    public ConflictException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
