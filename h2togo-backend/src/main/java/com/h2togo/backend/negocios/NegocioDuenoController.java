package com.h2togo.backend.negocios;

import com.h2togo.backend.catalogo.dto.PrecioRequest;
import com.h2togo.backend.catalogo.dto.ProductoResponse;
import com.h2togo.backend.negocios.dto.HorarioRequest;
import com.h2togo.backend.negocios.dto.HorarioResponse;
import com.h2togo.backend.negocios.dto.PerfilNegocioResponse;
import com.h2togo.backend.repartidores.MiembrosNegocioService;
import com.h2togo.backend.repartidores.dto.MiembroNegocioResponse;
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
 * Gestión del negocio por su repartidor (CU-023). El actor se resuelve del token; las
 * ediciones (horario, precio) exigen ser el dueño (RN-021) — se valida en el servicio.
 * El rol REPARTIDOR lo exige {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/negocios/me")
public class NegocioDuenoController {

    private final NegocioService negocioService;
    private final MiembrosNegocioService miembrosService;

    public NegocioDuenoController(NegocioService negocioService, MiembrosNegocioService miembrosService) {
        this.negocioService = negocioService;
        this.miembrosService = miembrosService;
    }

    /** Repartidores de la purificadora, con las solicitudes pendientes primero. Solo el dueño. */
    @GetMapping("/repartidores")
    public List<MiembroNegocioResponse> repartidores() {
        return miembrosService.miembros(SecurityUtils.idActual());
    }

    /** Aprueba a un repartidor que pidió unirse. */
    @PostMapping("/repartidores/{id}/aprobacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void aprobar(@PathVariable int id) {
        miembrosService.aprobar(SecurityUtils.idActual(), id);
    }

    /** Rechaza una solicitud pendiente: la cuenta se da de baja. */
    @DeleteMapping("/repartidores/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rechazar(@PathVariable int id) {
        miembrosService.rechazar(SecurityUtils.idActual(), id);
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
