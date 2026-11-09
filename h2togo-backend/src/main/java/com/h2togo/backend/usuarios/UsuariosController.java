package com.h2togo.backend.usuarios;

import com.h2togo.backend.auth.AuthService;
import com.h2togo.backend.security.SecurityUtils;
import com.h2togo.backend.usuarios.dto.CambioContrasenaRequest;
import com.h2togo.backend.usuarios.dto.CambioTelefonoConfirmacionRequest;
import com.h2togo.backend.usuarios.dto.CambioTelefonoSolicitudRequest;
import com.h2togo.backend.usuarios.dto.DispositivoRequest;
import com.h2togo.backend.usuarios.dto.EliminarCuentaRequest;
import com.h2togo.backend.usuarios.dto.PerfilMeResponse;
import com.h2togo.backend.usuarios.dto.PerfilUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final AuthService authService;

    public UsuariosController(UsuarioService usuarioService, AuthService authService) {
        this.usuarioService = usuarioService;
        this.authService = authService;
    }

    @GetMapping
    public PerfilMeResponse perfil() {
        return usuarioService.perfil(SecurityUtils.idActual());
    }

    @PatchMapping
    public PerfilMeResponse actualizar(@Valid @RequestBody PerfilUpdateRequest request) {
        return usuarioService.actualizar(SecurityUtils.idActual(), request);
    }

    /** Cambio de contraseña con sesión iniciada. 422 CONTRASENA_ACTUAL_INCORRECTA si la actual no coincide. */
    @PutMapping("/contrasena")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarContrasena(@Valid @RequestBody CambioContrasenaRequest request) {
        usuarioService.cambiarContrasena(SecurityUtils.idActual(), request);
    }

    /**
     * Cambio de teléfono con sesión iniciada, paso 1: envía un código por SMS al número nuevo.
     * 409 si el número ya pertenece a otra cuenta.
     */
    @PostMapping("/telefono/solicitud")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void solicitarCambioTelefono(@Valid @RequestBody CambioTelefonoSolicitudRequest request) {
        authService.solicitarCambioTelefono(SecurityUtils.idActual(), request.telefono());
    }

    /**
     * Cambio de teléfono con sesión iniciada, paso 2: valida el código y guarda el número nuevo.
     * Devuelve el perfil actualizado. 422 OTP_INVALIDO si el código no es válido.
     */
    @PostMapping("/telefono/confirmacion")
    public PerfilMeResponse confirmarCambioTelefono(@Valid @RequestBody CambioTelefonoConfirmacionRequest request) {
        int id = SecurityUtils.idActual();
        authService.confirmarCambioTelefono(id, request.telefono(), request.codigo());
        return usuarioService.perfil(id);
    }

    /** Registra el token FCM del dispositivo para recibir notificaciones con la app cerrada. */
    @PutMapping("/dispositivo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registrarDispositivo(@Valid @RequestBody DispositivoRequest request) {
        usuarioService.registrarDispositivo(SecurityUtils.idActual(), request.tokenFcm());
    }

    /** Baja voluntaria de la propia cuenta con confirmación de contraseña actual. */
    @PostMapping("/baja")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void darDeBaja(@Valid @RequestBody EliminarCuentaRequest request) {
        usuarioService.eliminarCuenta(SecurityUtils.idActual(), request);
    }

    /** Alias RESTful DELETE para baja voluntaria de la propia cuenta. */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarCuenta(@Valid @RequestBody EliminarCuentaRequest request) {
        usuarioService.eliminarCuenta(SecurityUtils.idActual(), request);
    }
}
