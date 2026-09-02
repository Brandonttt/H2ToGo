package com.h2togo.backend.negocios.dto;

/** Negocio cercano a una ubicación, con la distancia en metros (KNN, apoya CU-004). */
public record NegocioCercanoResponse(
        Integer id,
        String nombreComercial,
        double lat,
        double lon,
        double distanciaM
) {
}
