package com.h2togo.backend.routing.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Precarga el grafo OSM al arranque para que la primera petición de ruta sea rápida
 * (documenta el tiempo de carga en el log). Se desactiva con
 * {@code h2togo.routing.precargar=false} (perfil de pruebas), donde el grafo se carga
 * de forma lazy solo cuando se solicita una ruta.
 */
@Component
@ConditionalOnProperty(name = "h2togo.routing.precargar", havingValue = "true", matchIfMissing = true)
public class RoutingWarmup {

    private final OsmGraphLoader osmGraphLoader;

    public RoutingWarmup(OsmGraphLoader osmGraphLoader) {
        this.osmGraphLoader = osmGraphLoader;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void precargar() {
        osmGraphLoader.getGrafo();
    }
}
