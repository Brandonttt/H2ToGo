package com.h2togo.routing.service;

import com.h2togo.routing.domain.Arista;
import com.h2togo.routing.domain.Grafo;
import com.h2togo.routing.domain.Nodo;
import com.h2togo.routing.dto.RouteRequest;
import com.h2togo.routing.dto.RouteResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * ===========================================================================
 * IMPLEMENTACIÓN DEL ALGORITMO A* (A-Estrella)
 * ===========================================================================
 *
 * Implementa la interfaz CalculoRutaService (patrón Strategy).
 *
 * Fundamento del algoritmo:
 *   f(n) = g(n) + h(n)
 *   donde:
 *     g(n) = costo acumulado real desde el origen hasta n (en km)
 *     h(n) = heurística: distancia Haversine en línea recta desde n hasta el destino
 *     f(n) = estimación del costo total de la ruta que pasa por n
 *
 *   A* expande primero el nodo con menor f(n), garantizando la ruta más corta
 *   siempre que h(n) sea admisible (≤ costo real), lo que Haversine garantiza.
 *
 * Estructuras de datos usadas:
 *   - PriorityQueue<NodoConPrioridad>: cola de prioridad (min-heap) por f(n)
 *   - Map<osmId, gScore>: costo g acumulado para cada nodo visitado
 *   - Map<osmId, Nodo> cameFrom: registro del camino para reconstrucción
 *   - Set<osmId> cerrado: nodos ya expandidos (no se reprocesen)
 */
@Slf4j
@Service
public class AEstrella implements CalculoRutaService {

    // -------------------------------------------------------------------------
    // Clase auxiliar interna: nodo con su prioridad f(n) para la PriorityQueue
    // -------------------------------------------------------------------------
    private record NodoConPrioridad(Nodo nodo, double fScore)
            implements Comparable<NodoConPrioridad> {
        @Override
        public int compareTo(NodoConPrioridad otro) {
            return Double.compare(this.fScore, otro.fScore);
        }
    }

    // -------------------------------------------------------------------------
    // Algoritmo principal
    // -------------------------------------------------------------------------

    @Override
    public RouteResponse calcularRuta(Grafo grafo, RouteRequest request) {
        // --- 1. Mapear coordenadas del usuario al nodo OSM más cercano ---
        Nodo origen  = grafo.nodoMasCercano(request.getOrigenLat(),  request.getOrigenLon());
        Nodo destino = grafo.nodoMasCercano(request.getDestinoLat(), request.getDestinoLon());

        if (origen == null || destino == null) {
            log.warn("No se encontraron nodos cercanos a las coordenadas dadas.");
            return new RouteResponse(false, 0.0, List.of());
        }

        log.info("A* iniciando: origen={}, destino={}", origen, destino);

        // --- 2. Inicialización ---
        // Cola de prioridad: el nodo con menor f(n) sale primero
        PriorityQueue<NodoConPrioridad> abierto = new PriorityQueue<>();

        // gScore[id] = menor costo conocido desde origen hasta ese nodo
        Map<Long, Double> gScore = new HashMap<>();

        // cameFrom[id] = nodo predecesor en la ruta más barata encontrada hasta ahora
        Map<Long, Nodo> cameFrom = new HashMap<>();

        // Conjunto de nodos ya expandidos (evita reprocesamiento)
        Set<Long> cerrado = new HashSet<>();

        // Inicializar origen
        gScore.put(origen.getOsmId(), 0.0);
        double hOrigen = haversineKm(origen.getLatitud(), origen.getLongitud(),
                                     destino.getLatitud(), destino.getLongitud());
        abierto.add(new NodoConPrioridad(origen, hOrigen));

        // --- 3. Bucle principal de A* ---
        while (!abierto.isEmpty()) {
            Nodo actual = abierto.poll().nodo();

            // ¿Llegamos al destino?
            if (actual.getOsmId() == destino.getOsmId()) {
                log.info("A* encontró ruta. g(destino)={} km", gScore.get(destino.getOsmId()));
                return construirRespuesta(cameFrom, destino, gScore.get(destino.getOsmId()));
            }

            // Si ya fue expandido con un costo menor, ignorar esta entrada de la cola
            if (cerrado.contains(actual.getOsmId())) continue;
            cerrado.add(actual.getOsmId());

            // --- 4. Explorar vecinos ---
            for (Arista arista : grafo.obtenerVecinos(actual.getOsmId())) {
                Nodo vecino = arista.getDestino();

                if (cerrado.contains(vecino.getOsmId())) continue;

                // Costo tentativo: costo hasta 'actual' + peso de la arista
                double gTentativo = gScore.getOrDefault(actual.getOsmId(), Double.MAX_VALUE)
                                  + arista.getPesoKm();

                // Solo actualizamos si encontramos un camino mejor
                if (gTentativo < gScore.getOrDefault(vecino.getOsmId(), Double.MAX_VALUE)) {
                    cameFrom.put(vecino.getOsmId(), actual);
                    gScore.put(vecino.getOsmId(), gTentativo);

                    double h = haversineKm(vecino.getLatitud(), vecino.getLongitud(),
                                           destino.getLatitud(), destino.getLongitud());
                    double f = gTentativo + h;
                    abierto.add(new NodoConPrioridad(vecino, f));
                }
            }
        }

        // Si la cola se vacía sin llegar al destino, no hay ruta
        log.warn("A* no encontró ruta entre {} y {}", origen, destino);
        return new RouteResponse(false, 0.0, List.of());
    }

    // -------------------------------------------------------------------------
    // Reconstrucción del camino
    // -------------------------------------------------------------------------

    /**
     * Recorre el mapa cameFrom hacia atrás desde el destino hasta el origen
     * para obtener la lista ordenada de coordenadas de la ruta.
     */
    private RouteResponse construirRespuesta(Map<Long, Nodo> cameFrom,
                                             Nodo destino,
                                             double distanciaTotal) {
        LinkedList<RouteResponse.Coordenada> path = new LinkedList<>();
        Nodo actual = destino;

        while (actual != null) {
            path.addFirst(CalculoRutaService.toCoord(actual));
            actual = cameFrom.get(actual.getOsmId());
        }

        return new RouteResponse(true, Math.round(distanciaTotal * 1000.0) / 1000.0, path);
    }

    // -------------------------------------------------------------------------
    // Haversine — expuesta como método público estático para OsmGraphLoader
    // -------------------------------------------------------------------------

    /**
     * Distancia geodésica entre dos puntos geográficos en kilómetros.
     * Esta es la función h(n) del algoritmo A*.
     *
     * @param lat1 Latitud del punto 1 (grados decimales)
     * @param lon1 Longitud del punto 1 (grados decimales)
     * @param lat2 Latitud del punto 2 (grados decimales)
     * @param lon2 Longitud del punto 2 (grados decimales)
     * @return Distancia en kilómetros
     */
    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        // Reutilizamos la implementación del método default de la interfaz
        return CalculoRutaService.haversineKm(lat1, lon1, lat2, lon2);
    }
}
