package com.h2togo.backend.repartidores.dto;

import java.time.OffsetDateTime;

/**
 * Repartidor de la purificadora visto por su dueño. {@code aprobado = false}: pidió unirse y espera
 * la aprobación. {@code enLinea}: inició jornada.
 */
public record MiembroNegocioResponse(
        Integer idUsuario,
        String nombre,
        String apellidos,
        String correo,
        String telefono,
        boolean esDueno,
        boolean aprobado,
        boolean enLinea,
        boolean telefonoVerificado,
        OffsetDateTime fechaRegistro
) {
}
