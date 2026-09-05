package com.h2togo.backend.inventario.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Una línea de carga: cuántos garrafones de una marca mover de la base al vehículo. */
public record CargaItem(
        @NotNull Integer idMarca,
        @NotNull @Positive Integer cantidad
) {
}
