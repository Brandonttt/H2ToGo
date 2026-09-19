package com.h2togo.backend.negocios.dto;

/** Negocio cercano a una ubicación, con la distancia en metros (KNN, apoya CU-004). */
public record NegocioCercanoResponse(
        Integer id,
        String nombreComercial,
        double lat,
        double lon,
        double distanciaM,
        String direccion,
        int repartidores
) {
    public NegocioCercanoResponse(Integer id, String nombreComercial, double lat, double lon, double distanciaM) {
        this(id, nombreComercial, lat, lon, distanciaM, null, 0);
    }
}
