package com.h2togo.backend.planeacion.dto;

import com.h2togo.backend.routing.dto.RouteResponse.Coordenada;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Ruta del día optimizada con OR-Tools: paradas en el orden de visita, las que no se pudieron
 * incluir y la polilínea completa por calles (A* entre paradas consecutivas).
 */
public record RutaOptimaResponse(
        OffsetDateTime calculadaEn,
        String algoritmo,
        double origenLat,
        double origenLon,
        double distanciaTotalKm,
        long duracionMin,
        int capacidadVehiculo,
        int garrafonesEnRuta,
        List<Parada> paradas,
        List<Omitido> omitidos,
        List<Coordenada> coordenadas
) {

    /**
     * Una parada. {@code llegadaEstimada} supone velocidad urbana promedio; si se llega antes de la
     * ventana de un programado, incluye la espera. {@code fueraDeHorario}: se llegaría después del
     * margen de la hora programada.
     */
    public record Parada(
            int orden,
            Integer idPedido,
            String estado,
            String nombreCliente,
            String telefonoCliente,
            String direccion,
            double lat,
            double lon,
            int garrafones,
            OffsetDateTime fechaProgramada,
            OffsetDateTime llegadaEstimada,
            double distanciaDesdeAnteriorKm,
            boolean fueraDeHorario
    ) {
    }

    /** Pedido que quedó fuera de la ruta y por qué (no cabe en el vehículo o no hay camino). */
    public record Omitido(Integer idPedido, String nombreCliente, String direccion, String motivo) {
    }
}
