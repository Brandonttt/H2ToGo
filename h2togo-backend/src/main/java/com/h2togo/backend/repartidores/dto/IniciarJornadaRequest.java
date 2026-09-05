package com.h2togo.backend.repartidores.dto;

import com.h2togo.backend.inventario.dto.CargaItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Inicio de jornada (CU-008): el repartidor elige su vehículo y la carga inicial desde la
 * base. La carga inicial es opcional (puede iniciar y cargar después con CU-019).
 */
public record IniciarJornadaRequest(
        @NotNull Integer idVehiculo,
        List<@Valid CargaItem> cargaInicial
) {
}
