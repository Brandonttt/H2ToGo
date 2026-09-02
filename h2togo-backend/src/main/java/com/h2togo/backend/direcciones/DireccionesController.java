package com.h2togo.backend.direcciones;

import com.h2togo.backend.direcciones.dto.DireccionRequest;
import com.h2togo.backend.direcciones.dto.DireccionResponse;
import com.h2togo.backend.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Libreta de direcciones del cliente (soporte CU-004). Todo bajo {@code /clientes/me}:
 * el cliente se resuelve del token, nunca del body (RNF-008). El rol CLIENTE lo exige
 * {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/clientes/me/direcciones")
public class DireccionesController {

    private final DireccionService direccionService;

    public DireccionesController(DireccionService direccionService) {
        this.direccionService = direccionService;
    }

    @GetMapping
    public List<DireccionResponse> listar() {
        return direccionService.listar(SecurityUtils.idActual());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DireccionResponse crear(@Valid @RequestBody DireccionRequest request) {
        return direccionService.crear(SecurityUtils.idActual(), request);
    }

    @PutMapping("/{id}")
    public DireccionResponse actualizar(@PathVariable int id, @Valid @RequestBody DireccionRequest request) {
        return direccionService.actualizar(SecurityUtils.idActual(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable int id) {
        direccionService.eliminar(SecurityUtils.idActual(), id);
    }
}
