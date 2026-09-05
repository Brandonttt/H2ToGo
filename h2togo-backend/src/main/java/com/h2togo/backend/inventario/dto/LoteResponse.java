package com.h2togo.backend.inventario.dto;

import java.time.LocalDate;

/** Lote recién registrado en la base (CU-018). */
public record LoteResponse(
        Integer idLote,
        Integer idMarca,
        String marca,
        LocalDate fechaCaducidad,
        Integer cantidadActual
) {
}
