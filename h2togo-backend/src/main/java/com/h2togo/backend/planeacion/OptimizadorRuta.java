package com.h2togo.backend.planeacion;

import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.Assignment;
import com.google.ortools.constraintsolver.FirstSolutionStrategy;
import com.google.ortools.constraintsolver.LocalSearchMetaheuristic;
import com.google.ortools.constraintsolver.RoutingDimension;
import com.google.ortools.constraintsolver.RoutingIndexManager;
import com.google.ortools.constraintsolver.RoutingModel;
import com.google.ortools.constraintsolver.RoutingSearchParameters;
import com.google.ortools.constraintsolver.main;
import com.google.protobuf.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Orden óptimo de las paradas de un repartidor con Google OR-Tools: problema del viajero (TSP)
 * con ventanas de horario y capacidad del vehículo.
 *
 * <p>Modelo:
 * <ul>
 *   <li>Nodo 0 = posición actual del repartidor (salida). Nodos 1..n = paradas.</li>
 *   <li>Ruta abierta: termina en la última entrega. Se modela con un nodo final ficticio al que
 *       se llega desde cualquier parada con costo 0.</li>
 *   <li>Costo a minimizar: metros recorridos por calles.</li>
 *   <li>Dimensión "Tiempo" (minutos desde ahora): manejo a velocidad urbana + tiempo de servicio
 *       por parada. Se puede esperar (holgura) si se llega antes de la ventana; llegar tarde se
 *       permite pero se penaliza por minuto, para que una ventana ya vencida no deje la ruta sin
 *       solución.</li>
 *   <li>Dimensión "Carga": la suma de garrafones no puede pasar la capacidad del vehículo.</li>
 *   <li>Cada parada es opcional con una penalización enorme (disyunción): el solver solo deja
 *       fuera una parada cuando no cabe o no hay camino por calles, y la reporta como omitida.</li>
 * </ul>
 *
 * <p>Búsqueda: solución inicial con PATH_CHEAPEST_ARC (siempre la arista más barata) y mejora con
 * Guided Local Search hasta el límite de tiempo. El TSP es NP-difícil: con pocas paradas suele
 * dar el óptimo, pero no lo garantiza.
 */
@Component
public class OptimizadorRuta {

    /** Penalización (en metros equivalentes) por dejar fuera una parada: 1,000 km. */
    static final long PENALIZACION_OMITIR = 1_000_000;
    /** Costo de un tramo sin camino por calles: mayor que omitir la parada. */
    static final long METROS_SIN_RUTA = 10_000_000;
    /** Penalización por cada minuto que se llega después de la ventana (2 km por minuto). */
    static final long PENALIZACION_MINUTO_TARDE = 2_000;
    /** Horizonte de planeación: 48 horas. */
    static final long HORIZONTE_MIN = 48 * 60;

    private static volatile boolean nativasCargadas;

    /**
     * Carga los binarios nativos de OR-Tools la primera vez que se optimiza, no al arrancar: si
     * faltaran para la plataforma, solo falla la ruta óptima y el resto del backend sigue arriba.
     */
    private static void cargarNativas() {
        if (!nativasCargadas) {
            synchronized (OptimizadorRuta.class) {
                if (!nativasCargadas) {
                    Loader.loadNativeLibraries();
                    nativasCargadas = true;
                }
            }
        }
    }

    /**
     * @param metros       metros[i][j] por calles; índice 0 = salida del repartidor
     * @param demandas     garrafones por nodo (demandas[0] = 0)
     * @param ventanas     [inicio, fin] en minutos desde ahora por nodo, o null si no hay horario
     * @param capacidad    garrafones que caben en el vehículo
     * @param servicioMin  minutos que tarda cada entrega
     * @param velocidadKmh velocidad promedio de manejo en la ciudad
     * @param limiteMs     tiempo máximo de búsqueda
     */
    public record Problema(long[][] metros, int[] demandas, long[][] ventanas, int capacidad,
            int servicioMin, double velocidadKmh, int limiteMs) {
    }

    /** Una parada en el orden de visita; {@code nodo} es el índice en el problema (1..n). */
    public record Visita(int nodo, long llegadaMin, long metrosDesdeAnterior, boolean tarde) {
    }

    public record Solucion(List<Visita> visitas, List<Integer> omitidos, long metrosTotales) {
    }

    public Solucion resolver(Problema p) {
        int n = p.metros().length;              // salida + paradas
        int fin = n;                            // nodo ficticio de llegada
        if (n <= 1) {
            return new Solucion(List.of(), List.of(), 0);
        }
        cargarNativas();
        RoutingIndexManager manager = new RoutingIndexManager(n + 1, 1, new int[] {0}, new int[] {fin});
        RoutingModel routing = new RoutingModel(manager);

        int costoCb = routing.registerTransitCallback((long desde, long hacia) -> {
            int i = manager.indexToNode(desde);
            int j = manager.indexToNode(hacia);
            return tramo(p, i, j, fin);
        });
        routing.setArcCostEvaluatorOfAllVehicles(costoCb);

        double metrosPorMin = p.velocidadKmh() * 1000.0 / 60.0;
        int tiempoCb = routing.registerTransitCallback((long desde, long hacia) -> {
            int i = manager.indexToNode(desde);
            int j = manager.indexToNode(hacia);
            if (j == fin) {
                return 0;
            }
            long manejo = Math.round(Math.min(tramo(p, i, j, fin), HORIZONTE_MIN * metrosPorMin) / metrosPorMin);
            return manejo + (i == 0 ? 0 : p.servicioMin());
        });
        routing.addDimension(tiempoCb, HORIZONTE_MIN, HORIZONTE_MIN, true, "Tiempo");
        RoutingDimension tiempo = routing.getMutableDimension("Tiempo");

        int cargaCb = routing.registerUnaryTransitCallback((long desde) -> {
            int i = manager.indexToNode(desde);
            return i < n ? p.demandas()[i] : 0;
        });
        routing.addDimensionWithVehicleCapacity(cargaCb, 0, new long[] {p.capacidad()}, true, "Carga");

        for (int nodo = 1; nodo < n; nodo++) {
            long indice = manager.nodeToIndex(nodo);
            long[] ventana = p.ventanas()[nodo];
            if (ventana != null) {
                // Llegar antes obliga a esperar; llegar después cuesta por minuto de retraso.
                tiempo.cumulVar(indice).setMin(Math.max(0, ventana[0]));
                tiempo.setCumulVarSoftUpperBound(indice, Math.max(0, ventana[1]), PENALIZACION_MINUTO_TARDE);
            }
            routing.addDisjunction(new long[] {indice}, PENALIZACION_OMITIR);
        }

        RoutingSearchParameters parametros = main.defaultRoutingSearchParameters().toBuilder()
                .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
                .setLocalSearchMetaheuristic(LocalSearchMetaheuristic.Value.GUIDED_LOCAL_SEARCH)
                .setTimeLimit(Duration.newBuilder()
                        .setSeconds(p.limiteMs() / 1000)
                        .setNanos((p.limiteMs() % 1000) * 1_000_000))
                .build();
        Assignment solucion = routing.solveWithParameters(parametros);
        if (solucion == null) {
            List<Integer> todos = new ArrayList<>();
            for (int nodo = 1; nodo < n; nodo++) {
                todos.add(nodo);
            }
            return new Solucion(List.of(), todos, 0);
        }

        List<Visita> visitas = new ArrayList<>();
        long total = 0;
        int anterior = 0;
        long indice = solucion.value(routing.nextVar(routing.start(0)));
        while (!routing.isEnd(indice)) {
            int nodo = manager.indexToNode(indice);
            long metros = p.metros()[anterior][nodo];
            long llegada = solucion.min(tiempo.cumulVar(indice));
            long[] ventana = p.ventanas()[nodo];
            visitas.add(new Visita(nodo, llegada, metros, ventana != null && llegada > ventana[1]));
            total += metros;
            anterior = nodo;
            indice = solucion.value(routing.nextVar(indice));
        }
        List<Integer> omitidos = new ArrayList<>();
        for (int nodo = 1; nodo < n; nodo++) {
            long i = manager.nodeToIndex(nodo);
            if (solucion.value(routing.nextVar(i)) == i) {
                omitidos.add(nodo);
            }
        }
        return new Solucion(visitas, omitidos, total);
    }

    private static long tramo(Problema p, int i, int j, int fin) {
        if (j == fin || i == fin) {
            return 0;
        }
        long m = p.metros()[i][j];
        return m < 0 ? METROS_SIN_RUTA : m;
    }
}
