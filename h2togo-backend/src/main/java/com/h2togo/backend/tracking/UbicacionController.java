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
 * Fallback REST para reportar la ubicación del repartidor (CU-006) en redes que bloquean
 * WebSocket. Misma lógica que el canal STOMP: persiste y evalúa proximidad. Rol REPARTIDOR.
 */
@RestController
@RequestMapping("/api/v1/repartidores/me")
public class UbicacionController {

    private final UbicacionService ubicacionService;

    public UbicacionController(UbicacionService ubicacionService) {
        this.ubicacionService = ubicacionService;
    }

    @PutMapping("/ubicacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reportar(@Valid @RequestBody UbicacionRequest request) {
        ubicacionService.reportar(SecurityUtils.idActual(), request.lat(), request.lon());
    }
}
