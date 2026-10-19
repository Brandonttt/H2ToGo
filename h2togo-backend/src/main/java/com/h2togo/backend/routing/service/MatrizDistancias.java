package com.h2togo.backend.routing.service;

import com.h2togo.backend.routing.domain.Arista;
import com.h2togo.backend.routing.domain.Grafo;
import com.h2togo.backend.routing.domain.Nodo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Matriz de distancias por calles entre varios puntos (entrada del optimizador de rutas).
 *
 * <p>Para n puntos haría falta n·(n−1) búsquedas A*; en su lugar se corre un Dijkstra desde cada
 * punto que se detiene en cuanto alcanza a todos los demás: n búsquedas en total. Dijkstra es A*
 * con h(n) = 0, que es lo correcto cuando no hay un único destino al que apuntar.
 *
 * <p>Respeta los sentidos de circulación del grafo, así que la matriz no es simétrica: ir de A
 * a B puede costar distinto que de B a A.
 */
@Component
public class MatrizDistancias {

    /** Distancia que se reporta cuando no hay camino por calles entre dos puntos. */
    public static final double SIN_RUTA_KM = Double.POSITIVE_INFINITY;

    private record Pendiente(long osmId, double km) implements Comparable<Pendiente> {
        @Override
        public int compareTo(Pendiente otro) {
            return Double.compare(km, otro.km);
        }
    }

    /**
     * @param puntos pares {lat, lon}
     * @return km[i][j] = distancia por calles de i a j ({@link #SIN_RUTA_KM} si no hay camino)
     */
    public double[][] kilometros(Grafo grafo, List<double[]> puntos) {
        int n = puntos.size();
        Nodo[] nodos = new Nodo[n];
        for (int i = 0; i < n; i++) {
            nodos[i] = grafo.nodoMasCercano(puntos.get(i)[0], puntos.get(i)[1]);
        }
        double[][] km = new double[n][n];
        for (int i = 0; i < n; i++) {
            Map<Long, Double> distancias = dijkstra(grafo, nodos[i], objetivos(nodos, i));
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    continue;
                }
                km[i][j] = nodos[i] == null || nodos[j] == null
                        ? SIN_RUTA_KM
                        : distancias.getOrDefault(nodos[j].getOsmId(), SIN_RUTA_KM);
            }
        }
        return km;
    }

    private static Set<Long> objetivos(Nodo[] nodos, int origen) {
        Set<Long> ids = new HashSet<>();
        for (int j = 0; j < nodos.length; j++) {
            if (j != origen && nodos[j] != null) {
                ids.add(nodos[j].getOsmId());
            }
        }
        return ids;
    }

    /** Dijkstra desde {@code origen} hasta fijar la distancia de todos los {@code objetivos}. */
    private static Map<Long, Double> dijkstra(Grafo grafo, Nodo origen, Set<Long> objetivos) {
        Map<Long, Double> fijas = new HashMap<>();
        if (origen == null) {
            return fijas;
        }
        Map<Long, Double> tentativas = new HashMap<>();
        PriorityQueue<Pendiente> abierto = new PriorityQueue<>();
        Set<Long> faltan = new HashSet<>(objetivos);
        tentativas.put(origen.getOsmId(), 0.0);
        abierto.add(new Pendiente(origen.getOsmId(), 0.0));

        while (!abierto.isEmpty() && !faltan.isEmpty()) {
            Pendiente actual = abierto.poll();
            if (fijas.containsKey(actual.osmId())) {
                continue; // entrada vieja de la cola
            }
            fijas.put(actual.osmId(), actual.km());
            faltan.remove(actual.osmId());
            for (Arista arista : grafo.obtenerVecinos(actual.osmId())) {
                long vecino = arista.getDestino().getOsmId();
                if (fijas.containsKey(vecino)) {
                    continue;
                }
                double km = actual.km() + arista.getPesoKm();
                if (km < tentativas.getOrDefault(vecino, Double.POSITIVE_INFINITY)) {
                    tentativas.put(vecino, km);
                    abierto.add(new Pendiente(vecino, km));
                }
            }
        }
        return fijas;
    }

    /** Une los tramos de una ruta de varias paradas en una sola polilínea, sin repetir puntos. */
    public static <T> List<T> unir(List<List<T>> tramos) {
        List<T> salida = new ArrayList<>();
        for (List<T> tramo : tramos) {
            for (T punto : tramo) {
                if (salida.isEmpty() || !salida.get(salida.size() - 1).equals(punto)) {
                    salida.add(punto);
                }
            }
        }
        return salida;
    }
}
