package com.h2togo.backend.negocios;

import com.h2togo.backend.catalogo.dto.PrecioRequest;
import com.h2togo.backend.catalogo.dto.ProductoResponse;
import com.h2togo.backend.negocios.dto.HorarioRequest;
import com.h2togo.backend.negocios.dto.HorarioResponse;
import com.h2togo.backend.negocios.dto.PerfilNegocioResponse;
import com.h2togo.backend.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestión del negocio por su repartidor (CU-023). El actor se resuelve del token; las
 * ediciones (horario, precio) exigen ser el dueño (RN-021) — se valida en el servicio.
 * El rol REPARTIDOR lo exige {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/negocios/me")
public class NegocioDuenoController {

    private final NegocioService negocioService;

    public NegocioDuenoController(NegocioService negocioService) {
        this.negocioService = negocioService;
    }

    @GetMapping
    public PerfilNegocioResponse miNegocio() {
        return negocioService.miNegocio(SecurityUtils.idActual());
    }

    @PutMapping("/horarios")
    public List<HorarioResponse> actualizarHorarios(@RequestBody List<@Valid HorarioRequest> dias) {
        return negocioService.actualizarHorarios(SecurityUtils.idActual(), dias);
    }

    @PutMapping("/productos/{id}/precio")
    public ProductoResponse actualizarPrecio(@PathVariable int id, @Valid @RequestBody PrecioRequest request) {
        return negocioService.actualizarPrecio(SecurityUtils.idActual(), id, request);
    }
}
