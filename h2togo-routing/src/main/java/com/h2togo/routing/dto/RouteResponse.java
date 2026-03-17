package com.h2togo.routing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de salida del endpoint POST /api/route.
 *
 * Respuesta exitosa:
 * {
 *   "encontrada": true,
 *   "distanciaTotalKm": 2.34,
 *   "coordenadas": [
 *     { "lat": 19.432608, "lon": -99.133209 },
 *     ...
 *   ]
 * }
 *
 * Respuesta sin ruta:
 * {
 *   "encontrada": false,
 *   "distanciaTotalKm": 0.0,
 *   "coordenadas": []
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponse {

    private boolean encontrada;
    private double distanciaTotalKm;
    private List<Coordenada> coordenadas;

    // -------------------------------------------------------------------------
    // Clase interna: par latitud/longitud para la polyline
    // -------------------------------------------------------------------------
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Coordenada {
        private double lat;
        private double lon;
    }
}
