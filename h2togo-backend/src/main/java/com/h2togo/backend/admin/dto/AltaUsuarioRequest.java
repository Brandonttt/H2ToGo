package com.h2togo.backend.admin.dto;

import com.h2togo.backend.common.enums.RolUsuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta manual de usuario por el administrador (CU-014). Si {@code rol=repartidor},
 * {@code negocio} indica crear uno nuevo (dueño, RN-024) o unirse a existente. El usuario
 * queda pre-verificado (§10 #30).
 */
public record AltaUsuarioRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 100) String apellidos,
        @NotBlank @Email @Size(max = 150) String correo,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 20) String telefono,
        @NotNull RolUsuario rol,
        @Valid NegocioAlta negocio
) {

    public record NegocioAlta(Integer idExistente, @Size(max = 150) String nombreComercial) {
    }
}
