package com.h2togo.backend.admin;

import com.h2togo.backend.admin.dto.AltaUsuarioRequest;
import com.h2togo.backend.admin.dto.BajaRequest;
import com.h2togo.backend.admin.dto.UsuarioAdminFila;
import com.h2togo.backend.auth.dto.PerfilResponse;
import com.h2togo.backend.common.PagedResponse;
import com.h2togo.backend.common.enums.RolUsuario;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Gestión de usuarios por el administrador (CU-014/015). Rol ADMIN (SecurityConfig). */
@RestController
@RequestMapping("/api/v1/admin/usuarios")
public class AdminUsuariosController {

    private final AdminUsuariosService service;
    private final AdminConsultasService consultas;

    public AdminUsuariosController(AdminUsuariosService service, AdminConsultasService consultas) {
        this.service = service;
        this.consultas = consultas;
    }

    /** Listado paginado; {@code q} busca en nombre, apellidos, correo, teléfono o id exacto. */
    @GetMapping
    public PagedResponse<UsuarioAdminFila> listar(
            @RequestParam(required = false) RolUsuario rol,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Integer idNegocio,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return consultas.usuarios(rol, activo, idNegocio, q, pageable);
    }

    @GetMapping("/resumen")
    public Map<String, Object> resumen() {
        return consultas.resumenUsuarios();
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
