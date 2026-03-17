package com.h2togo.routing.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa un segmento de calle (arista/edge) entre dos nodos del grafo.
 *
 * Corresponde a un tramo de una "way" de OSM.
 * El peso (pesoKm) es la distancia Haversine real entre los dos nodos extremos,
 * calculada al momento de construir el grafo desde OsmGraphLoader.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Arista {

    /** Nodo de origen del segmento (desde) */
    private Nodo origen;

    /** Nodo de destino del segmento (hasta) */
    private Nodo destino;

    /**
     * Peso de la arista en kilómetros.
     * Se calcula con la fórmula de Haversine al construir el grafo,
     * por lo que representa la distancia física real del segmento de calle.
     */
    private double pesoKm;
}
