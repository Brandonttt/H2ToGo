package com.h2togo.backend.tracking;

import com.h2togo.backend.security.SecurityUtils;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Consulta de la última ubicación del repartidor de un pedido (CU-006). Cliente dueño o repartidor asignado. */
@RestController
@RequestMapping("/api/v1/pedidos")
public class RastreoPedidoController {

    private final RastreoService rastreoService;
    private final AccesoRastreo acceso;

    public RastreoPedidoController(RastreoService rastreoService, AccesoRastreo acceso) {
        this.rastreoService = rastreoService;
        this.acceso = acceso;
    }

    /** 200 con {@code lat, lon, timestamp}; 204 si el pedido no está en ruta o aún no hay posición. */
    @GetMapping("/{id}/ubicacion-repartidor")
    public ResponseEntity<Map<String, Object>> ubicacionRepartidor(@PathVariable int id) {
        if (!acceso.puedeVer(SecurityUtils.idActual(), id)) {
            throw new AccessDeniedException("No tiene permiso para ver este pedido (RN-016).");
        }
        return rastreoService.ultimaUbicacion(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
