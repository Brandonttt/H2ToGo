package com.h2togo.backend.repartidores.dto;

import com.h2togo.backend.inventario.dto.InventarioVehiculoResponse;

/** Estado de la jornada tras iniciarla (CU-008). */
public record JornadaResponse(
        Integer idVehiculo,
        boolean estadoOperativo,
        InventarioVehiculoResponse inventarioVehiculo
) {
}
