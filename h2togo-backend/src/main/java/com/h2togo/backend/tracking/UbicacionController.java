package com.h2togo.backend.tracking;

import com.h2togo.backend.pedidos.dto.UbicacionRequest;
import com.h2togo.backend.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reporte REST de la ubicación del repartidor (CU-006). Misma lógica que el canal STOMP:
 * persiste, evalúa proximidad y la reenvía al cliente de cada pedido activo. Es la vía que usa
 * la app, porque sobrevive mejor a redes móviles inestables que un WebSocket. Rol REPARTIDOR.
 */
@RestController
@RequestMapping("/api/v1/repartidores/me")
public class UbicacionController {

    private final RastreoService rastreoService;

    public UbicacionController(RastreoService rastreoService) {
        this.rastreoService = rastreoService;
    }

    @PutMapping("/ubicacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reportar(@Valid @RequestBody UbicacionRequest request) {
        rastreoService.publicar(SecurityUtils.idActual(), request.lat(), request.lon());
    }
}
