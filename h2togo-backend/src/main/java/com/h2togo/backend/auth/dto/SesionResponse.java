package com.h2togo.backend.auth.dto;

import com.h2togo.backend.common.enums.RolUsuario;
import java.time.OffsetDateTime;

/**
 * Sesión iniciada (CU-002). {@code suspendidoHasta} viaja solo para clientes suspendidos
 * (RN-006, §10 #16): el login NO se bloquea, solo informa la fecha para que la app avise
 * que no podrá pedir hasta entonces.
 */
public record SesionResponse(
        String token,
        OffsetDateTime expiraEn,
        RolUsuario rol,
        OffsetDateTime suspendidoHasta,
        PerfilResponse perfil
) {
}
