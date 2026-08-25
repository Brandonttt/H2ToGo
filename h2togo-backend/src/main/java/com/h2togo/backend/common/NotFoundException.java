package com.h2togo.backend.common;

/** Recurso inexistente → 404 (§3.3). */
public class NotFoundException extends RuntimeException {

    private final String codigo;

    public NotFoundException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public NotFoundException(String mensaje) {
        this("NO_ENCONTRADO", mensaje);
    }

    public String getCodigo() {
        return codigo;
    }
}
