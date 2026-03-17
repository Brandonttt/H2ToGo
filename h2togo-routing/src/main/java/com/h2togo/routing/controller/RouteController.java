package com.h2togo.routing.controller;

import com.h2togo.routing.dto.RouteRequest;
import com.h2togo.routing.dto.RouteResponse;
import com.h2togo.routing.service.CalculoRutaService;
import com.h2togo.routing.service.OsmGraphLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST del Motor de Ruteo H2ToGo.
 *
 * Expone el endpoint:
 *   POST /api/route
 *
 * Diseño:
 *   - Depende de CalculoRutaService (interfaz) → no hay acoplamiento a A* directamente.
 *   - Depende de OsmGraphLoader para obtener el grafo precargado en memoria.
 *   - CORS habilitado para facilitar las pruebas con el cliente web local (localhost).
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // En producción, restringir al dominio del frontend
public class RouteController {

    /** Implementación inyectada por Spring (actualmente AEstrella) */
    private final CalculoRutaService calculoRutaService;

    /** Grafo OSM precargado al arrancar la aplicación */
    private final OsmGraphLoader osmGraphLoader;

    /**
     * Calcula la ruta óptima entre dos coordenadas geográficas.
     *
     * Request body (JSON):
     * {
     *   "origenLat":  19.432608,
     *   "origenLon": -99.133209,
     *   "destinoLat": 19.441234,
     *   "destinoLon": -99.128765
     * }
     *
     * Response 200 (ruta encontrada):
     * {
     *   "encontrada": true,
     *   "distanciaTotalKm": 2.341,
     *   "coordenadas": [
     *     { "lat": 19.432608, "lon": -99.133209 },
     *     ...
     *   ]
     * }
     *
     * Response 422 (sin ruta posible):
     * { "encontrada": false, "distanciaTotalKm": 0.0, "coordenadas": [] }
     */
    @PostMapping("/route")
    public ResponseEntity<RouteResponse> calcularRuta(@RequestBody RouteRequest request) {
        log.info("POST /api/route | origen=({},{}) destino=({},{})",
                 request.getOrigenLat(), request.getOrigenLon(),
                 request.getDestinoLat(), request.getDestinoLon());

        RouteResponse response = calculoRutaService.calcularRuta(
            osmGraphLoader.getGrafo(),
            request
        );

        if (response.isEncontrada()) {
            return ResponseEntity.ok(response);
        } else {
            // 422 Unprocessable Entity: la petición es válida pero no hay ruta
            return ResponseEntity.unprocessableEntity().body(response);
        }
    }
}
