package com.h2togo.backend.planeacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.h2togo.backend.planeacion.OptimizadorRuta.Problema;
import com.h2togo.backend.planeacion.OptimizadorRuta.Solucion;
import com.h2togo.backend.planeacion.OptimizadorRuta.Visita;
import org.junit.jupiter.api.Test;

/**
 * Paradas sobre una calle recta: el repartidor sale del metro 0 y las casas están en los metros
 * indicados. Con distancias |a − b| la respuesta correcta se puede calcular a mano.
 */
class OptimizadorRutaTest {

    private final OptimizadorRuta optimizador = new OptimizadorRuta();

    /** Salida en el metro 0 seguida de las paradas en {@code posiciones}. */
    private static long[][] enLinea(long... posiciones) {
        long[] todos = new long[posiciones.length + 1];
        System.arraycopy(posiciones, 0, todos, 1, posiciones.length);
        long[][] m = new long[todos.length][todos.length];
        for (int i = 0; i < todos.length; i++) {
            for (int j = 0; j < todos.length; j++) {
                m[i][j] = Math.abs(todos[i] - todos[j]);
            }
        }
        return m;
    }

    private static Problema problema(long[][] metros, int[] demandas, long[][] ventanas, int capacidad) {
        return new Problema(metros, demandas, ventanas, capacidad, 5, 20.0, 500);
    }

    private static int[] orden(Solucion s) {
        return s.visitas().stream().mapToInt(Visita::nodo).toArray();
    }

    @Test
    void visitaPrimeroLoMasCercanoYTerminaEnLaUltimaEntrega() {
        // Paradas: 1 → metro 3000, 2 → metro 1000, 3 → metro 2000.
        Solucion s = optimizador.resolver(problema(enLinea(3000, 1000, 2000),
                new int[] {0, 1, 1, 1}, new long[4][], 30));

        assertThat(orden(s)).containsExactly(2, 3, 1);
        assertThat(s.metrosTotales()).isEqualTo(3000); // ruta abierta: no cuenta el regreso
        assertThat(s.omitidos()).isEmpty();
    }

    @Test
    void laVentanaDeHorarioCambiaElOrden() {
        // La parada del metro 3000 debe recibirse en los primeros 10 minutos (a 20 km/h son 9).
        long[][] ventanas = new long[4][];
        ventanas[1] = new long[] {0, 10};
        Solucion s = optimizador.resolver(problema(enLinea(3000, 1000, 2000),
                new int[] {0, 1, 1, 1}, ventanas, 30));

        assertThat(orden(s)[0]).isEqualTo(1);
        assertThat(s.visitas().get(0).tarde()).isFalse();
        assertThat(s.visitas().get(0).llegadaMin()).isLessThanOrEqualTo(10);
    }

    @Test
    void siSeLlegaAntesDeLaVentanaSeEspera() {
        long[][] ventanas = new long[2][];
        ventanas[1] = new long[] {60, 90}; // pedido programado para dentro de una hora
        Solucion s = optimizador.resolver(problema(enLinea(1000), new int[] {0, 1}, ventanas, 30));

        assertThat(s.visitas().get(0).llegadaMin()).isEqualTo(60);
    }

    @Test
    void omiteLaParadaQueNoCabeEnElVehiculo() {
        // 4 + 4 garrafones en un vehículo de 6: solo cabe uno de los dos pedidos.
        Solucion s = optimizador.resolver(problema(enLinea(1000, 2000),
                new int[] {0, 4, 4}, new long[3][], 6));

        assertThat(s.visitas()).hasSize(1);
        assertThat(s.omitidos()).hasSize(1);
    }

    @Test
    void omiteLaParadaSinCaminoPorCalles() {
        long[][] metros = enLinea(1000, 2000);
        metros[0][2] = -1;
        metros[1][2] = -1;
        Solucion s = optimizador.resolver(problema(metros, new int[] {0, 1, 1}, new long[3][], 30));

        assertThat(orden(s)).containsExactly(1);
        assertThat(s.omitidos()).containsExactly(2);
    }
}
