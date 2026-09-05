package com.h2togo.backend.inventario.dto;

import java.time.LocalDate;

/** Existencia de un lote (en base o en vehículo), con su caducidad. */
public record InventarioItem(
        Integer idLote,
        Integer idMarca,
        String marca,
        LocalDate fechaCaducidad,
        Integer cantidadActual,
        Integer cantidadApartada
) {
}
