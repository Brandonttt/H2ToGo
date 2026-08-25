package com.h2togo.backend.auth;

import com.h2togo.backend.auth.dto.LoginRequest;
import com.h2togo.backend.auth.dto.OtpRequest;
import com.h2togo.backend.auth.dto.ReenviarOtpRequest;
import com.h2togo.backend.auth.dto.RegistroRequest;
import com.h2togo.backend.auth.dto.RegistroResponse;
import com.h2togo.backend.auth.dto.SesionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autenticación (CU-001/002/003). Todo público bajo {@code /api/v1/auth} (el resto de
 * la API exige token). El actor se resuelve del token, nunca del body (RNF-008).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistroResponse registro(@Valid @RequestBody RegistroRequest request) {
        return authService.registrar(request);
    }

    @PostMapping("/verificacion-telefono")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verificarTelefono(@Valid @RequestBody OtpRequest request) {
        authService.verificarTelefono(request);
    }

    @PostMapping("/verificacion-telefono/reenviar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reenviarOtp(@Valid @RequestBody ReenviarOtpRequest request) {
        authService.reenviarOtp(request);
    }

    @PostMapping("/login")
    public ResponseEntity<SesionResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        authService.logout();
    }
}
