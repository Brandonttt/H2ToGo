package com.h2togo.backend.negocios.dto;

import com.h2togo.backend.catalogo.dto.ProductoResponse;
import java.util.List;

/**
 * Perfil de una purificadora (CU-022). {@code abiertoAhora} se calcula con el horario y la
 * hora local (RN-004). {@code direccion} puede ser nula si el negocio aún no registró su base.
 * Solo datos públicos (RN-016).
 */
public record PerfilNegocioResponse(
        Integer id,
        String nombreComercial,
        boolean activo,
        DireccionBase direccion,
        boolean abiertoAhora,
        List<HorarioResponse> horarios,
        List<ProductoResponse> productos
) {

    /** Dirección de la base para el mini mapa (lat/lon del punto geográfico). */
    public record DireccionBase(
            String calle,
            String numeroExterior,
            String numeroInterior,
            String colonia,
            String codigoPostal,
            String referencias,
            Double lat,
            Double lon
    ) {
    }
}
