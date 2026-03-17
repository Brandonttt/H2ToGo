package com.h2togo.routing.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa una intersección o punto geográfico del mapa (nodo OSM).
 *
 * En el formato de OpenStreetMap, cada "node" tiene:
 *   - un id único (osmId)
 *   - latitud y longitud
 *
 * Esta clase es la unidad básica del grafo de calles.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Nodo {

    /** Identificador original de OSM (ej: 123456789) */
    private long osmId;

    /** Latitud geográfica en grados decimales */
    private double latitud;

    /** Longitud geográfica en grados decimales */
    private double longitud;

    /**
     * Dos nodos son iguales si tienen el mismo osmId.
     * Necesario para que PriorityQueue y HashMap funcionen correctamente en A*.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Nodo nodo)) return false;
        return osmId == nodo.osmId;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(osmId);
    }

    @Override
    public String toString() {
        return "Nodo{osmId=" + osmId + ", lat=" + latitud + ", lon=" + longitud + "}";
    }
}
