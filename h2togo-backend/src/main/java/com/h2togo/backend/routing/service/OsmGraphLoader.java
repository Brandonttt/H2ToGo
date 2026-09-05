package com.h2togo.backend.routing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2togo.backend.routing.domain.Arista;
import com.h2togo.backend.routing.domain.Grafo;
import com.h2togo.backend.routing.domain.Nodo;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Lee el JSON de OpenStreetMap (formato Overpass) y construye el {@link Grafo} de calles.
 * <p>Integrado del prototipo {@code h2togo-routing} (F9); el algoritmo y el formato no cambian.
 * La carga es <b>lazy</b> (se construye en el primer {@code getGrafo()}) y en producción se
 * dispara al arranque con {@code RoutingWarmup}; en pruebas queda desactivada para no cargar
 * el grafo de ~5.4 MB salvo cuando se pide una ruta.
 */
@Service
public class OsmGraphLoader {

    private static final Logger log = LoggerFactory.getLogger(OsmGraphLoader.class);

    @Value("classpath:osm_data/sample_map.json")
    private Resource osmResource;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile Grafo grafo;

    /** Devuelve el grafo, construyéndolo una sola vez de forma segura ante concurrencia. */
    public Grafo getGrafo() {
        Grafo actual = grafo;
        if (actual == null) {
            synchronized (this) {
                actual = grafo;
                if (actual == null) {
                    actual = construir();
                    grafo = actual;
                }
            }
        }
        return actual;
    }

    private Grafo construir() {
        long inicio = System.currentTimeMillis();
        Grafo g = new Grafo();
        try (InputStream is = osmResource.getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            JsonNode elements = root.get("elements");
            if (elements == null || !elements.isArray()) {
                log.error("El archivo OSM no contiene el campo 'elements' esperado.");
                return g;
            }

            for (JsonNode element : elements) {
                if ("node".equals(element.path("type").asText())) {
                    g.agregarNodo(new Nodo(element.path("id").asLong(),
                            element.path("lat").asDouble(), element.path("lon").asDouble()));
                }
            }

            int aristas = 0;
            for (JsonNode element : elements) {
                if (!"way".equals(element.path("type").asText())) {
                    continue;
                }
                JsonNode tags = element.path("tags");
                if (!esCalleTransitable(tags)) {
                    continue;
                }
                boolean unaDireccion = "yes".equalsIgnoreCase(tags.path("oneway").asText("no"));
                List<Nodo> nodosDeVia = new ArrayList<>();
                for (JsonNode ref : element.path("nodes")) {
                    Nodo n = g.obtenerNodo(ref.asLong());
                    if (n != null) {
                        nodosDeVia.add(n);
                    }
                }
                for (int i = 0; i < nodosDeVia.size() - 1; i++) {
                    Nodo desde = nodosDeVia.get(i);
                    Nodo hasta = nodosDeVia.get(i + 1);
                    double peso = AEstrella.haversineKm(desde.getLatitud(), desde.getLongitud(),
                            hasta.getLatitud(), hasta.getLongitud());
                    g.agregarArista(new Arista(desde, hasta, peso));
                    aristas++;
                    if (!unaDireccion) {
                        g.agregarArista(new Arista(hasta, desde, peso));
                        aristas++;
                    }
                }
            }
            log.info("Grafo OSM cargado: {} nodos, {} aristas en {} ms.",
                    g.totalNodos(), aristas, System.currentTimeMillis() - inicio);
        } catch (Exception e) {
            log.error("Error al cargar el archivo OSM: {}", e.getMessage(), e);
        }
        return g;
    }

    /** Solo calles transitables por el repartidor (excluye ciclovías, peatonales, etc.). */
    private boolean esCalleTransitable(JsonNode tags) {
        if (tags == null || tags.isMissingNode()) {
            return false;
        }
        return switch (tags.path("highway").asText("")) {
            case "motorway", "trunk", "primary", "secondary", "tertiary",
                 "residential", "service", "unclassified",
                 "motorway_link", "trunk_link", "primary_link",
                 "secondary_link", "tertiary_link" -> true;
            default -> false;
        };
    }
}
