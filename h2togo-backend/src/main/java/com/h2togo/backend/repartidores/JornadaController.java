package com.h2togo.backend.repartidores;

import com.h2togo.backend.inventario.InventarioService;
import com.h2togo.backend.repartidores.dto.IniciarJornadaRequest;
import com.h2togo.backend.repartidores.dto.JornadaResponse;
import com.h2togo.backend.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Jornada del repartidor (CU-008): seleccionar vehículo y carga inicial → disponible.
 * Rol REPARTIDOR (SecurityConfig); el actor se resuelve del token.
 */
@RestController
@RequestMapping("/api/v1/repartidores/me")
public class JornadaController {

    private final InventarioService inventarioService;

    public JornadaController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @PostMapping("/jornada")
    public JornadaResponse iniciarJornada(@Valid @RequestBody IniciarJornadaRequest request) {
        return inventarioService.iniciarJornada(SecurityUtils.idActual(), request);
    }
}
