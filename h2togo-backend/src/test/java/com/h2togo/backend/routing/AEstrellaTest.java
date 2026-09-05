package com.h2togo.backend.routing;

import static org.assertj.core.api.Assertions.assertThat;

import com.h2togo.backend.routing.domain.Arista;
import com.h2togo.backend.routing.domain.Grafo;
import com.h2togo.backend.routing.domain.Nodo;
import com.h2togo.backend.routing.dto.RouteRequest;
import com.h2togo.backend.routing.dto.RouteResponse;
import com.h2togo.backend.routing.service.AEstrella;
import org.junit.jupiter.api.Test;

/** Prueba unitaria del A* portado (F9), sobre un grafo mínimo en memoria (sin el JSON OSM). */
class AEstrellaTest {

    private Grafo grafoLineal() {
        Grafo g = new Grafo();
        Nodo a = new Nodo(1, 19.3700, -99.1800);
        Nodo b = new Nodo(2, 19.3710, -99.1800);
        Nodo c = new Nodo(3, 19.3720, -99.1800);
        g.agregarNodo(a);
        g.agregarNodo(b);
        g.agregarNodo(c);
        g.agregarArista(new Arista(a, b, AEstrella.haversineKm(a.getLatitud(), a.getLongitud(), b.getLatitud(), b.getLongitud())));
        g.agregarArista(new Arista(b, c, AEstrella.haversineKm(b.getLatitud(), b.getLongitud(), c.getLatitud(), c.getLongitud())));
        return g;
    }

    @Test
    void encuentraRutaEntreOrigenYDestino() {
        RouteRequest req = new RouteRequest();
        req.setOrigenLat(19.3700);
        req.setOrigenLon(-99.1800);
        req.setDestinoLat(19.3720);
        req.setDestinoLon(-99.1800);

        RouteResponse ruta = new AEstrella().calcularRuta(grafoLineal(), req);

        assertThat(ruta.isEncontrada()).isTrue();
        assertThat(ruta.getCoordenadas()).hasSize(3);
        assertThat(ruta.getDistanciaTotalKm()).isGreaterThan(0.0);
    }

    @Test
    void sinAristasNoHayRuta() {
        Grafo g = new Grafo();
        g.agregarNodo(new Nodo(1, 19.3700, -99.1800));
        g.agregarNodo(new Nodo(2, 19.4000, -99.1500)); // aislado, sin aristas
        RouteRequest req = new RouteRequest();
        req.setOrigenLat(19.3700);
        req.setOrigenLon(-99.1800);
        req.setDestinoLat(19.4000);
        req.setDestinoLon(-99.1500);

        RouteResponse ruta = new AEstrella().calcularRuta(g, req);

        assertThat(ruta.isEncontrada()).isFalse();
    }
}
