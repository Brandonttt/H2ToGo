package com.h2togo.backend.routing.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * Nodos de la componente fuertemente conexa más grande: la red de calles donde desde cualquier
     * punto se puede llegar a cualquier otro respetando los sentidos. Los datos de OSM traen
     * fragmentos sueltos (privadas, accesos, tramos cortados en el borde del mapa); si una dirección
     * se "pega" a uno de ellos no hay ruta hacia ni desde ahí. Se calcula una vez (Kosaraju).
     * Vacío si ninguna componente reúne más de la mitad de los nodos (grafos de prueba de un solo
     * sentido): entonces no se filtra.
     */
    private volatile Set<Long> componentePrincipal;

    /**
     * Encuentra el nodo de la red principal más cercano a las coordenadas dadas. Se usa porque las
     * coordenadas del domicilio o del GPS no caen exactamente sobre un nodo OSM.
     *
     * Complejidad: O(n) — aceptable para grafos de tamaño de ciudad.
     * Para grafos grandes (> 500k nodos) se puede mejorar con un KD-Tree.
     */
    public Nodo nodoMasCercano(double latitud, double longitud) {
        Set<Long> red = componentePrincipal();
        Nodo masCercano = null;
        double menorDistancia = Double.MAX_VALUE;

        for (Nodo nodo : nodos.values()) {
            if (!red.isEmpty() && !red.contains(nodo.getOsmId())) {
                continue;
            }
            double dist = distanciaEuclidiana(latitud, longitud,
                                              nodo.getLatitud(), nodo.getLongitud());
            if (dist < menorDistancia) {
                menorDistancia = dist;
                masCercano = nodo;
            }
        }
        return masCercano;
    }

    /** Componente fuertemente conexa más grande (se calcula la primera vez que se pide). */
    public Set<Long> componentePrincipal() {
        Set<Long> actual = componentePrincipal;
        if (actual == null) {
            synchronized (this) {
                actual = componentePrincipal;
                if (actual == null) {
                    actual = calcularComponentePrincipal();
                    componentePrincipal = actual;
                }
            }
        }
        return actual;
    }

    /**
     * Kosaraju iterativo (sin recursión, para no desbordar la pila con miles de nodos):
     * 1) DFS sobre el grafo guardando el orden de terminación;
     * 2) DFS sobre el grafo transpuesto en orden inverso de terminación: cada árbol es una
     *    componente fuertemente conexa. Se queda con la más grande. O(V + E).
     */
    private Set<Long> calcularComponentePrincipal() {
        List<Long> orden = new ArrayList<>(nodos.size());
        Set<Long> visitados = new HashSet<>();
        for (Long inicio : nodos.keySet()) {
            if (visitados.contains(inicio)) {
                continue;
            }
            // Pila de (nodo, índice del siguiente vecino por explorar).
            Deque<long[]> pila = new ArrayDeque<>();
            pila.push(new long[] {inicio, 0});
            visitados.add(inicio);
            while (!pila.isEmpty()) {
                long[] tope = pila.peek();
                List<Arista> vecinos = obtenerVecinos(tope[0]);
                if (tope[1] < vecinos.size()) {
                    long siguiente = vecinos.get((int) tope[1]++).getDestino().getOsmId();
                    if (visitados.add(siguiente)) {
                        pila.push(new long[] {siguiente, 0});
                    }
                } else {
                    orden.add(pila.pop()[0]);
                }
            }
        }

        Map<Long, List<Long>> transpuesto = new HashMap<>();
        for (List<Arista> aristas : listaAdyacencia.values()) {
            for (Arista a : aristas) {
                transpuesto.computeIfAbsent(a.getDestino().getOsmId(), k -> new ArrayList<>())
                        .add(a.getOrigen().getOsmId());
            }
        }

        Set<Long> asignados = new HashSet<>();
        Set<Long> mayor = Set.of();
        for (int i = orden.size() - 1; i >= 0; i--) {
            Long raiz = orden.get(i);
            if (!asignados.add(raiz)) {
                continue;
            }
            Set<Long> componente = new HashSet<>();
            Deque<Long> pila = new ArrayDeque<>();
            pila.push(raiz);
            while (!pila.isEmpty()) {
                Long n = pila.pop();
                componente.add(n);
                for (Long previo : transpuesto.getOrDefault(n, List.of())) {
                    if (asignados.add(previo)) {
                        pila.push(previo);
                    }
                }
            }
            if (componente.size() > mayor.size()) {
                mayor = componente;
            }
        }
        return mayor.size() * 2 > nodos.size() ? mayor : Set.of();
    }

    /** Distancia euclidiana simple en espacio latitud/longitud (solo para comparar, no para rutas). */
    private double distanciaEuclidiana(double lat1, double lon1, double lat2, double lon2) {
        double dLat = lat1 - lat2;
        double dLon = lon1 - lon2;
        return Math.sqrt(dLat * dLat + dLon * dLon);
    }
}
