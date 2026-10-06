package com.h2togo.backend.negocios.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Negocio cercano a una ubicación, con la distancia en metros (KNN, apoya CU-004). Incluye lo
 * necesario para la lista de la app: si está abierto ahora (RN-004), el precio más bajo de su
 * catálogo y las marcas que vende. {@code precioDesde} es null si no tiene productos activos.
 */
public record NegocioCercanoResponse(
        Integer id,
        String nombreComercial,
        double lat,
        double lon,
        double distanciaM,
        String direccion,
        int repartidores,
        boolean abiertoAhora,
        BigDecimal precioDesde,
        List<String> marcas
) {
}
