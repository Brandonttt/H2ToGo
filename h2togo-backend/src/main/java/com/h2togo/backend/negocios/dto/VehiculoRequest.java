package com.h2togo.backend.negocios.dto;

import com.h2togo.backend.common.enums.TipoVehiculo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record VehiculoRequest(
        TipoVehiculo tipoVehiculo,
        @Size(max = 50) String marca,
        @Size(max = 50) String modelo,
        @Size(max = 30) String color,
        @Size(max = 20) String placas,
        @Min(1) Integer capacidadGarrafones
) {
}
