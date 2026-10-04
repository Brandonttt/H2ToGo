package com.h2togo.backend.admin.dto;

/** Negocio (purificadora) para selectores del panel: alta de repartidor, filtros. */
public record NegocioAdminFila(
        Integer id,
        String nombreComercial,
        boolean activo,
        Integer idDueno,
        String dueno,
        String colonia,
        long repartidores
) {
}
