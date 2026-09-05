package com.h2togo.backend.admin;

import com.h2togo.backend.admin.dto.AltaUsuarioRequest;
import com.h2togo.backend.admin.dto.BajaRequest;
import com.h2togo.backend.auth.dto.PerfilResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Gestión de usuarios por el administrador (CU-014/015). Rol ADMIN (SecurityConfig). */
@RestController
@RequestMapping("/api/v1/admin/usuarios")
public class AdminUsuariosController {

    private final AdminUsuariosService service;

    public AdminUsuariosController(AdminUsuariosService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PerfilResponse alta(@Valid @RequestBody AltaUsuarioRequest request) {
        return service.alta(request);
    }

    @PostMapping("/{id}/baja")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void baja(@PathVariable int id, @Valid @RequestBody BajaRequest request) {
        service.baja(id, request);
    }

    @PostMapping("/{id}/reactivacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reactivar(@PathVariable int id) {
        service.reactivar(id);
    }
}
