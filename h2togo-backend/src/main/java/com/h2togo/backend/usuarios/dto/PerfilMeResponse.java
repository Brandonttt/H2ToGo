package com.h2togo.backend.usuarios.dto;

import com.h2togo.backend.common.enums.RolUsuario;
import java.time.OffsetDateTime;

/**
 * Perfil del usuario autenticado: datos comunes + los del subtipo según el rol
 * (GET /usuarios/me). Los bloques {@code cliente}/{@code repartidor} son nulos si no aplican.
 */
public record PerfilMeResponse(
        Integer id,
        String nombre,
        String apellidos,
        String correo,
        String telefono,
        RolUsuario rol,
        String urlFotoPerfil,
        boolean telefonoVerificado,
        ClienteInfo cliente,
        RepartidorInfo repartidor
) {

    public record ClienteInfo(int ausenciasConsecutivas, OffsetDateTime suspendidoHasta) {
    }

    public record RepartidorInfo(Integer idNegocio, boolean estadoOperativo,
            Integer idVehiculoActual, boolean esDueno) {
    }
}
