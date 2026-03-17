package com.h2togo.routing.dto;

import lombok.Data;

/**
 * DTO de entrada para el endpoint POST /api/route.
 *
 * El cliente envía las coordenadas de origen y destino en formato JSON:
 * {
 *   "origenLat":  19.432608,
 *   "origenLon": -99.133209,
 *   "destinoLat":  19.441234,
 *   "destinoLon": -99.128765
 * }
 */
@Data
public class RouteRequest {
    private double origenLat;
    private double origenLon;
    private double destinoLat;
    private double destinoLon;
}
