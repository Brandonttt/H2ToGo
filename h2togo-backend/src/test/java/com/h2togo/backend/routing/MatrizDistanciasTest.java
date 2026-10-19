package com.h2togo.backend.routing;

import static org.assertj.core.api.Assertions.assertThat;

import com.h2togo.backend.routing.domain.Arista;
import com.h2togo.backend.routing.domain.Grafo;
import com.h2togo.backend.routing.domain.Nodo;
import com.h2togo.backend.routing.service.MatrizDistancias;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Grafo de juguete: una calle de doble sentido A–B–C (1 km por tramo), un sentido único C → D y
 * un tramo suelto E–F lejos de la red, como los fragmentos que trae OSM.
 */
class MatrizDistanciasTest {

    private final Nodo a = new Nodo(1, 19.000, -99.000);
    private final Nodo b = new Nodo(2, 19.009, -99.000);
    private final Nodo c = new Nodo(3, 19.018, -99.000);
    private final Nodo d = new Nodo(4, 19.027, -99.000);
    private final Nodo e = new Nodo(5, 19.000, -99.050);
    private final Nodo f = new Nodo(6, 19.001, -99.050);

    private Grafo grafo() {
        Grafo g = new Grafo();
        for (Nodo n : List.of(a, b, c, d, e, f)) {
            g.agregarNodo(n);
        }
        doble(g, a, b);
        doble(g, b, c);
        g.agregarArista(new Arista(c, d, 1.0)); // sentido único
        g.agregarArista(new Arista(d, a, 3.0)); // regreso largo: D también es de la red principal
        doble(g, e, f);
        return g;
    }

    private static void doble(Grafo g, Nodo x, Nodo y) {
        g.agregarArista(new Arista(x, y, 1.0));
        g.agregarArista(new Arista(y, x, 1.0));
    }

    @Test
    void laRedPrincipalExcluyeLosFragmentosSueltos() {
        Grafo g = grafo();
        assertThat(g.componentePrincipal()).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
        // Un punto junto al tramo suelto se pega al nodo más cercano de la red principal.
        assertThat(g.nodoMasCercano(19.000, -99.049).getOsmId()).isEqualTo(1L);
    }

    @Test
    void calculaLaMatrizRespetandoLosSentidos() {
        double[][] km = new MatrizDistancias().kilometros(grafo(), List.of(
                new double[] {19.000, -99.000}, new double[] {19.018, -99.000}, new double[] {19.027, -99.000}));

        assertThat(km[0][1]).isEqualTo(2.0);  // A → C
        assertThat(km[1][2]).isEqualTo(1.0);  // C → D por el sentido único
        assertThat(km[2][1]).isEqualTo(5.0);  // D → C: regresa por A (3 + 1 + 1)
        assertThat(km[0][0]).isZero();
    }
}
