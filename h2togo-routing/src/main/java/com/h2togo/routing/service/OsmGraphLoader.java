package com.h2togo.routing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2togo.routing.domain.Arista;
import com.h2togo.routing.domain.Grafo;
import com.h2togo.routing.domain.Nodo;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee un archivo JSON de OpenStreetMap (formato Overpass API) y construye el Grafo.
 *
 * ===========================================================================
 * FORMATO DE ENTRADA esperado (exportado desde Overpass Turbo / overpass-api.de):
 * ===========================================================================
 * {
 *   "elements": [
 *     { "type": "node", "id": 123, "lat": 19.43, "lon": -99.13 },
 *     { "type": "node", "id": 124, "lat": 19.44, "lon": -99.12 },
 *     {
 *       "type": "way",
 *       "id": 999,
 *       "nodes": [123, 124, ...],
 *       "tags": { "highway": "residential", "oneway": "yes" }
 *     }
 *   ]
 * }
 * ===========================================================================
 *
 * Para obtener este JSON de tu zona, ve a:
 *   https://overpass-turbo.eu/
 * y ejecuta la query incluida en osm_data/overpass_query.txt
 */
@Slf4j
@Service
public class OsmGraphLoader {

    /** Grafo resultante, disponible para todo el contexto Spring */
    @Getter
    private Grafo grafo;

    /** Ruta al archivo JSON de OSM dentro de resources */
    @Value("classpath:osm_data/sample_map.json")
    private Resource osmResource;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Se ejecuta automáticamente al arrancar la aplicación.
     * Carga el archivo JSON y construye el grafo una sola vez en memoria.
     */
    @PostConstruct
    public void cargarGrafo() {
        grafo = new Grafo();
        try (InputStream is = osmResource.getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            JsonNode elements = root.get("elements");

            if (elements == null || !elements.isArray()) {
                log.error("El archivo OSM no contiene el campo 'elements' esperado.");
                return;
            }

            // --- Paso 1: cargar todos los nodos primero ---
            int nodosAgregados = 0;
            for (JsonNode element : elements) {
                if ("node".equals(element.path("type").asText())) {
                    long id      = element.path("id").asLong();
                    double lat   = element.path("lat").asDouble();
                    double lon   = element.path("lon").asDouble();
                    grafo.agregarNodo(new Nodo(id, lat, lon));
                    nodosAgregados++;
                }
            }

            // --- Paso 2: procesar las vías (ways) y crear aristas ---
            int aristasAgregadas = 0;
            for (JsonNode element : elements) {
                if ("way".equals(element.path("type").asText())) {
                    JsonNode nodeRefs = element.path("nodes");
                    JsonNode tags     = element.path("tags");

                    // Solo procesamos vías que son calles transitables
                    if (!esCalleTransitable(tags)) continue;

                    boolean soloUnaDireccion = "yes".equalsIgnoreCase(
                        tags.path("oneway").asText("no")
                    );

                    // Convertir la lista de ids a una lista de nodos reales
                    List<Nodo> nodosDeVia = new ArrayList<>();
                    for (JsonNode ref : nodeRefs) {
                        Nodo n = grafo.obtenerNodo(ref.asLong());
                        if (n != null) nodosDeVia.add(n);
                    }

                    // Crear aristas entre nodos consecutivos de la vía
                    for (int i = 0; i < nodosDeVia.size() - 1; i++) {
                        Nodo desde  = nodosDeVia.get(i);
                        Nodo hasta  = nodosDeVia.get(i + 1);
                        double peso = AEstrella.haversineKm(
                            desde.getLatitud(), desde.getLongitud(),
                            hasta.getLatitud(), hasta.getLongitud()
                        );

                        // Arista en dirección forward siempre
                        grafo.agregarArista(new Arista(desde, hasta, peso));
                        aristasAgregadas++;

                        // Arista inversa solo si la calle es bidireccional
                        if (!soloUnaDireccion) {
                            grafo.agregarArista(new Arista(hasta, desde, peso));
                            aristasAgregadas++;
                        }
                    }
                }
            }

            log.info("Grafo OSM cargado: {} nodos, {} aristas.", nodosAgregados, aristasAgregadas);

        } catch (Exception e) {
            log.error("Error al cargar el archivo OSM: {}", e.getMessage(), e);
        }
    }

    /**
     * Filtra solo los tipos de vía que corresponden a calles por donde
     * puede circular un repartidor de garrafones.
     *
     * Se excluyen ciclovías, peatonales, autopistas de acceso restringido, etc.
     */
    private boolean esCalleTransitable(JsonNode tags) {
        if (tags == null || tags.isMissingNode()) return false;
        String highway = tags.path("highway").asText("");
        return switch (highway) {
            case "motorway", "trunk", "primary", "secondary", "tertiary",
                 "residential", "service", "unclassified",
                 "motorway_link", "trunk_link", "primary_link",
                 "secondary_link", "tertiary_link" -> true;
            default -> false;
        };
    }
}
