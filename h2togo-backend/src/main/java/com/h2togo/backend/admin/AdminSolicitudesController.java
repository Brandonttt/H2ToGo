package com.h2togo.backend.admin;

import com.h2togo.backend.common.enums.EstadoSolicitud;
import com.h2togo.backend.security.SecurityUtils;
import com.h2togo.backend.solicitudes.SolicitudService;
import com.h2togo.backend.solicitudes.dto.ResolucionRequest;
import com.h2togo.backend.solicitudes.dto.SolicitudResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Revisión y resolución de solicitudes por el administrador (CU-021). Rol ADMIN. */
@RestController
@RequestMapping("/api/v1/admin/solicitudes")
public class AdminSolicitudesController {

    private final SolicitudService solicitudService;

    public AdminSolicitudesController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @GetMapping
    public List<SolicitudResponse> listar(@RequestParam(required = false) EstadoSolicitud estado) {
        return solicitudService.pendientes(estado);
    }

    @PostMapping("/{id}/resolucion")
    public SolicitudResponse resolver(@PathVariable int id, @Valid @RequestBody ResolucionRequest request) {
        return solicitudService.resolver(SecurityUtils.idActual(), id, request);
    }
}
