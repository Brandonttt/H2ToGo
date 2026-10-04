package com.h2togo.backend.admin.dto;

import com.h2togo.backend.common.enums.RolUsuario;
import java.time.OffsetDateTime;

/** Fila del listado de usuarios del panel (CU-014/015). Sin credenciales ni tokens. */
public record UsuarioAdminFila(
        Integer id,
        String nombre,
        String apellidos,
        String correo,
        String telefono,
        RolUsuario rol,
        boolean cuentaActiva,
        boolean telefonoVerificado,
        OffsetDateTime fechaRegistro,
        String motivoBaja,
        OffsetDateTime fechaBaja,
        Integer idNegocio,
        String negocio,
        boolean esDueno,
        Boolean enLinea,
        OffsetDateTime suspendidoHasta,
        long pedidos
) {
}
