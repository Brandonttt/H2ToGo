package com.h2togo.backend.auth.dto;

import com.h2togo.backend.common.enums.RolUsuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta de cuenta (CU-001). Si {@code rol=repartidor}, {@code negocio} indica si crea
 * una purificadora nueva (queda como dueño, RN-024) o se une a una existente. La
 * coherencia de ese bloque según el rol se valida en el servicio.
 */
public record RegistroRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 100) String apellidos,
        @NotBlank @Email @Size(max = 150) String correo,
        // Mínimo 8 caracteres (CU-001, Requerimientos especiales del TT).
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 20) String telefono,
        @NotNull RolUsuario rol,
        @Valid NegocioRegistroRequest negocio
) {

    /** Datos del negocio para el alta de un repartidor. */
    public record NegocioRegistroRequest(
            Integer idExistente,
            @Size(max = 150) String nombreComercial
    ) {
    }
}
