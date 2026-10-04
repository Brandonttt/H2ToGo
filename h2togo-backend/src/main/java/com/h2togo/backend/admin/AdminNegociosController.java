package com.h2togo.backend.admin;

import com.h2togo.backend.admin.dto.NegocioAdminFila;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Negocios para el panel (selector de alta de repartidor, transferencia de dueño). Rol ADMIN. */
@RestController
@RequestMapping("/api/v1/admin/negocios")
public class AdminNegociosController {

    private final AdminConsultasService consultas;

    public AdminNegociosController(AdminConsultasService consultas) {
        this.consultas = consultas;
    }

    @GetMapping
    public List<NegocioAdminFila> listar() {
        return consultas.negocios();
    }
}
