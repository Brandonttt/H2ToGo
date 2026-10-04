package com.h2togo.backend.admin;

import com.h2togo.backend.admin.dto.SolicitudAdminFila;
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
    private final AdminConsultasService consultas;

    public AdminSolicitudesController(SolicitudService solicitudService, AdminConsultasService consultas) {
        this.solicitudService = solicitudService;
        this.consultas = consultas;
    }

    /** Sin {@code estado} devuelve las pendientes; con {@code todas=true} ignora el estado. */
    @GetMapping
    public List<SolicitudAdminFila> listar(
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(defaultValue = "false") boolean todas) {
        if (todas) {
            return consultas.solicitudes(null);
        }
        return consultas.solicitudes(estado == null ? EstadoSolicitud.pendiente : estado);
    }

    @PostMapping("/{id}/resolucion")
    public SolicitudResponse resolver(@PathVariable int id, @Valid @RequestBody ResolucionRequest request) {
        return solicitudService.resolver(SecurityUtils.idActual(), id, request);
    }
}
