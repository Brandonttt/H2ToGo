package com.h2togo.backend.solicitudes;

import com.h2togo.backend.security.SecurityUtils;
import com.h2togo.backend.solicitudes.dto.SolicitudRequest;
import com.h2togo.backend.solicitudes.dto.SolicitudResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Solicitudes de cambio del dueño del negocio (CU-020/CU-009/CU-023-alta). Rol REPARTIDOR;
 * la propiedad del negocio (RN-021) se valida en el servicio.
 */
@RestController
@RequestMapping("/api/v1/solicitudes")
public class SolicitudesController {

    private final SolicitudService solicitudService;

    public SolicitudesController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudResponse crear(@Valid @RequestBody SolicitudRequest request) {
        return solicitudService.crear(SecurityUtils.idActual(), request);
    }

    @GetMapping
    public List<SolicitudResponse> misSolicitudes() {
        return solicitudService.misSolicitudes(SecurityUtils.idActual());
    }
}
