package com.h2togo.backend.usuarios;

import com.h2togo.backend.security.SecurityUtils;
import com.h2togo.backend.usuarios.dto.DispositivoRequest;
import com.h2togo.backend.usuarios.dto.PerfilMeResponse;
import com.h2togo.backend.usuarios.dto.PerfilUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Perfil propio (C/R/A). El usuario se resuelve del token (RNF-008). */
@RestController
@RequestMapping("/api/v1/usuarios/me")
public class UsuariosController {

    private final UsuarioService usuarioService;

    public UsuariosController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public PerfilMeResponse perfil() {
        return usuarioService.perfil(SecurityUtils.idActual());
    }

    @PatchMapping
    public PerfilMeResponse actualizar(@Valid @RequestBody PerfilUpdateRequest request) {
        return usuarioService.actualizar(SecurityUtils.idActual(), request);
    }

    /** Registra el token FCM del dispositivo para recibir notificaciones con la app cerrada. */
    @PutMapping("/dispositivo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registrarDispositivo(@Valid @RequestBody DispositivoRequest request) {
        usuarioService.registrarDispositivo(SecurityUtils.idActual(), request.tokenFcm());
    }
}
