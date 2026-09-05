package com.h2togo.backend.pedidos;

import com.h2togo.backend.common.enums.RolUsuario;
import com.h2togo.backend.pedidos.dto.PedidoResponse;
import com.h2togo.backend.pedidos.dto.ResultadoEntregaRequest;
import com.h2togo.backend.pedidos.dto.UbicacionRequest;
import com.h2togo.backend.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Acciones del repartidor sobre un pedido (CU-010/012): aceptar (concurrencia RF-019),
 * ponerse en camino y registrar el resultado. Exige rol REPARTIDOR (se resuelve del token).
 */
@RestController
@RequestMapping("/api/v1/pedidos")
public class AsignacionController {

    private final AsignacionService asignacionService;
    private final EntregaService entregaService;

    public AsignacionController(AsignacionService asignacionService, EntregaService entregaService) {
        this.asignacionService = asignacionService;
        this.entregaService = entregaService;
    }

    @PostMapping("/{id}/aceptacion")
    public PedidoResponse aceptar(@PathVariable int id) {
        return asignacionService.aceptar(SecurityUtils.exigirRol(RolUsuario.repartidor), id);
    }

    @PostMapping("/{id}/en-camino")
    public PedidoResponse enCamino(@PathVariable int id, @Valid @RequestBody UbicacionRequest request) {
        return asignacionService.enCamino(SecurityUtils.exigirRol(RolUsuario.repartidor), id,
                request.lat(), request.lon());
    }

    @PostMapping("/{id}/resultado")
    public PedidoResponse resultado(@PathVariable int id, @Valid @RequestBody ResultadoEntregaRequest request) {
        return entregaService.resultado(SecurityUtils.exigirRol(RolUsuario.repartidor), id, request);
    }
}
