package com.h2togo.backend.inventario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Carga de garrafones de la base al vehículo (CU-019); el servicio elige los lotes FIFO. */
public record CargaVehiculoRequest(
        @NotEmpty List<@Valid CargaItem> cargas
) {
}
