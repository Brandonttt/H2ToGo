package com.h2togo.backend.routing;

import com.h2togo.backend.common.enums.RolUsuario;
import com.h2togo.backend.routing.dto.RouteResponse;
import com.h2togo.backend.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ruta de entrega (CU-011) para el repartidor asignado y su versión de seguimiento para el cliente; devuelve la polilínea y la distancia
 * total (mismo {@code RouteResponse} del motor A*). 422 si no hay ruta posible.
 */
@RestController
@RequestMapping("/api/v1/pedidos")
public class RutaController {

    private final RutaService rutaService;

    public RutaController(RutaService rutaService) {
        this.rutaService = rutaService;
    }

    @GetMapping("/{id}/ruta")
    public ResponseEntity<RouteResponse> ruta(
            @PathVariable int id,
            @RequestParam(required = false) Double origenLat,
            @RequestParam(required = false) Double origenLon) {
        RouteResponse ruta = rutaService.ruta(
                SecurityUtils.exigirRol(RolUsuario.repartidor), id, origenLat, origenLon);
        return ruta.isEncontrada() ? ResponseEntity.ok(ruta) : ResponseEntity.unprocessableEntity().body(ruta);
    }

    /** Seguimiento del cliente (CU-006): la misma ruta por calles, del repartidor a su domicilio. */
    @GetMapping("/{id}/ruta-seguimiento")
    public ResponseEntity<RouteResponse> rutaSeguimiento(
            @PathVariable int id,
            @RequestParam(required = false) Double origenLat,
            @RequestParam(required = false) Double origenLon) {
        RouteResponse ruta = rutaService.rutaSeguimiento(
                SecurityUtils.exigirRol(RolUsuario.cliente), id, origenLat, origenLon);
        return ruta.isEncontrada() ? ResponseEntity.ok(ruta) : ResponseEntity.unprocessableEntity().body(ruta);
    }
}
