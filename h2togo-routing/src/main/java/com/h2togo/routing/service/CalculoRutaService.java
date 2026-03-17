package com.h2togo.routing.service;

import com.h2togo.routing.domain.Grafo;
import com.h2togo.routing.domain.Nodo;
import com.h2togo.routing.dto.RouteRequest;
import com.h2togo.routing.dto.RouteResponse;

/**
 * ===========================================================================
 * PATRÓN STRATEGY — Interfaz de Cálculo de Rutas
 * ===========================================================================
 *
 * Define el contrato que CUALQUIER algoritmo de ruteo debe cumplir.
 * Actualmente implementado por: AEstrella
 *
 * Para agregar un nuevo algoritmo (Dijkstra, Bellman-Ford, etc.) en el futuro,
 * solo se necesita crear una nueva clase que implemente esta interfaz y
 * anotarla con @Service + @Qualifier("nombreAlgoritmo").
 *
 * El controlador depende de esta interfaz, NO de una implementación concreta
 * → Esto cumple el principio de Inversión de Dependencias (DIP en SOLID).
 */
public interface CalculoRutaService {

    /**
     * Calcula la ruta óptima entre origen y destino sobre el grafo dado.
     *
     * @param grafo   Grafo de calles previamente construido desde OSM
     * @param request Coordenadas de origen y destino
     * @return        Ruta con la lista de coordenadas y la distancia total
     */
    RouteResponse calcularRuta(Grafo grafo, RouteRequest request);

    /**
     * Fórmula de Haversine — distancia geodésica entre dos puntos en km.
     *
     * Es la función heurística h(n) de A*:
     *   - Es admisible: nunca sobreestima la distancia real (las calles
     *     nunca son más cortas que la línea recta).
     *   - Es consistente: cumple la desigualdad triangular.
     *   → Garantiza que A* encuentre la ruta óptima.
     *
     * Alternativas consideradas:
     *   - Distancia Euclidiana plana: menos precisa en zonas grandes (la Tierra es curva).
     *   - Manhattan: solo válida en grillas perfectas, no en OSM.
     *   - Haversine ✓: precisa, estándar en GIS, costo computacional bajo (O(1)).
     */
    static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Radio medio de la Tierra en km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Convierte un nodo OSM a la clase interna Coordenada del DTO de respuesta.
     * Helper compartido entre implementaciones.
     */
    static RouteResponse.Coordenada toCoord(Nodo n) {
        return new RouteResponse.Coordenada(n.getLatitud(), n.getLongitud());
    }
}
