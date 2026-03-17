package com.h2togo.routing.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Estructura de datos del grafo de calles.
 *
 * Internamente usa una lista de adyacencia:
 *   Map<osmId_del_nodo, List<Arista_que_salen_de_ese_nodo>>
 *
 * El diseño es intencional:
 *   - Acceso O(1) a los vecinos de un nodo durante A*.
 *   - Separación limpia entre la estructura (Grafo) y los algoritmos (AEstrella).
 */
public class Grafo {

    /** Todos los nodos indexados por su osmId para búsqueda en O(1) */
    private final Map<Long, Nodo> nodos = new HashMap<>();

    /** Lista de adyacencia: osmId → aristas salientes */
    private final Map<Long, List<Arista>> listaAdyacencia = new HashMap<>();

    // -------------------------------------------------------------------------
    // Construcción del grafo
    // -------------------------------------------------------------------------

    /** Agrega un nodo al grafo. Si ya existe, lo reemplaza. */
    public void agregarNodo(Nodo nodo) {
        nodos.put(nodo.getOsmId(), nodo);
        listaAdyacencia.putIfAbsent(nodo.getOsmId(), new ArrayList<>());
    }

    /**
     * Agrega una arista dirigida origen → destino.
     * Para calles bidireccionales llama dos veces (origen→destino y destino→origen).
     */
    public void agregarArista(Arista arista) {
        long origenId = arista.getOrigen().getOsmId();
        listaAdyacencia
            .computeIfAbsent(origenId, k -> new ArrayList<>())
            .add(arista);
    }

    // -------------------------------------------------------------------------
    // Consultas
    // -------------------------------------------------------------------------

    /**
     * Devuelve todas las aristas que salen del nodo con el osmId dado.
     * Retorna lista vacía si el nodo no existe o no tiene vecinos.
     */
    public List<Arista> obtenerVecinos(long osmId) {
        return listaAdyacencia.getOrDefault(osmId, List.of());
    }

    /** Recupera un nodo por su osmId, o null si no existe. */
    public Nodo obtenerNodo(long osmId) {
        return nodos.get(osmId);
    }

    /** Devuelve la vista de todos los nodos del grafo. */
    public Map<Long, Nodo> obtenerTodosLosNodos() {
        return nodos;
    }

    /** Cantidad de nodos en el grafo (útil para logs y métricas). */
    public int totalNodos() {
        return nodos.size();
    }

    /** Cantidad de aristas totales del grafo. */
    public int totalAristas() {
        return listaAdyacencia.values().stream().mapToInt(List::size).sum();
    }

    /**
     * Encuentra el nodo del grafo más cercano a las coordenadas dadas.
     * Se usa cuando el usuario hace clic en el mapa y las coordenadas no caen
     * exactamente sobre un nodo OSM.
     *
     * Complejidad: O(n) — aceptable para grafos de tamaño de ciudad.
     * Para grafos grandes (> 500k nodos) se puede mejorar con un KD-Tree.
     */
    public Nodo nodoMasCercano(double latitud, double longitud) {
        Nodo masCercano = null;
        double menorDistancia = Double.MAX_VALUE;

        for (Nodo nodo : nodos.values()) {
            double dist = distanciaEuclidiana(latitud, longitud,
                                              nodo.getLatitud(), nodo.getLongitud());
            if (dist < menorDistancia) {
                menorDistancia = dist;
                masCercano = nodo;
            }
        }
        return masCercano;
    }

    /** Distancia euclidiana simple en espacio latitud/longitud (solo para comparar, no para rutas). */
    private double distanciaEuclidiana(double lat1, double lon1, double lat2, double lon2) {
        double dLat = lat1 - lat2;
        double dLon = lon1 - lon2;
        return Math.sqrt(dLat * dLat + dLon * dLon);
    }
}
