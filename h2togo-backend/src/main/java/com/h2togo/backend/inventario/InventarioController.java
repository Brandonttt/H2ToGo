package com.h2togo.backend.inventario;

import com.h2togo.backend.inventario.dto.CargaVehiculoRequest;
import com.h2togo.backend.inventario.dto.InventarioBaseResponse;
import com.h2togo.backend.inventario.dto.InventarioVehiculoResponse;
import com.h2togo.backend.inventario.dto.LoteEntradaRequest;
import com.h2togo.backend.inventario.dto.LoteResponse;
import com.h2togo.backend.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inventario del negocio y del vehículo (CU-018/019). El actor se resuelve del token; el
 * rol REPARTIDOR lo exige {@code SecurityConfig}. Registrar lote exige ser dueño (RN-021),
 * validado en el servicio.
 */
@RestController
@RequestMapping("/api/v1/inventario")
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @PostMapping("/lotes")
    @ResponseStatus(HttpStatus.CREATED)
    public LoteResponse registrarLote(@Valid @RequestBody LoteEntradaRequest request) {
        return inventarioService.registrarLote(SecurityUtils.idActual(), request);
    }

    @GetMapping("/base")
    public InventarioBaseResponse base() {
        return inventarioService.inventarioBase(SecurityUtils.idActual());
    }

    @GetMapping("/vehiculo")
    public InventarioVehiculoResponse vehiculo() {
        return inventarioService.inventarioVehiculo(SecurityUtils.idActual());
    }

    @PostMapping("/carga-vehiculo")
    public InventarioVehiculoResponse cargar(@Valid @RequestBody CargaVehiculoRequest request) {
        return inventarioService.cargarVehiculo(SecurityUtils.idActual(), request.cargas());
    }

    @PostMapping("/devolucion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void devolucion() {
        inventarioService.devolucion(SecurityUtils.idActual());
    }
}
