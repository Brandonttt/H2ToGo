package com.h2togo.backend.planeacion;

import com.h2togo.backend.common.enums.RolUsuario;
import com.h2togo.backend.planeacion.dto.AgendaPedidoResponse;
import com.h2togo.backend.planeacion.dto.RutaOptimaResponse;
import com.h2togo.backend.security.SecurityUtils;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Agenda de programados y ruta óptima del día del repartidor. Rol REPARTIDOR. */
@RestController
@RequestMapping("/api/v1")
public class PlaneacionController {

    private final PlaneacionService planeacionService;

    public PlaneacionController(PlaneacionService planeacionService) {
        this.planeacionService = planeacionService;
    }

    /** Programados de hoy que su purificadora puede surtir: libres o reservados por él. */
    @GetMapping("/repartidores/me/agenda")
    public List<AgendaPedidoResponse> agenda() {
        return planeacionService.agenda(SecurityUtils.idActual());
    }

    /** Orden óptimo de todas sus paradas (OR-Tools). 422 SIN_UBICACION si no hay GPS ni base. */
    @GetMapping("/repartidores/me/ruta-optima")
    public RutaOptimaResponse rutaOptima(
            @RequestParam(required = false) Double origenLat,
            @RequestParam(required = false) Double origenLon) {
        return planeacionService.rutaOptima(SecurityUtils.idActual(), origenLat, origenLon);
    }

    /** Reserva un programado de hoy. 409 si otro repartidor lo reservó primero. */
    @PostMapping("/pedidos/{id}/reserva")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reservar(@PathVariable int id) {
        planeacionService.reservar(SecurityUtils.exigirRol(RolUsuario.repartidor), id);
    }

    @DeleteMapping("/pedidos/{id}/reserva")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void liberar(@PathVariable int id) {
        planeacionService.liberar(SecurityUtils.exigirRol(RolUsuario.repartidor), id);
    }
}
