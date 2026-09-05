package com.h2togo.backend.solicitudes.dto;

import com.h2togo.backend.common.enums.CodigoCambioPerfil;
import com.h2togo.backend.common.enums.EstadoSolicitud;
import java.time.OffsetDateTime;

/** Solicitud de cambio de perfil (CU-020/021). */
public record SolicitudResponse(
        Integer id,
        CodigoCambioPerfil codigoCambio,
        Integer idNegocio,
        Integer idVehiculo,
        Integer idProductoNegocio,
        String valorAnterior,
        String valorNuevo,
        EstadoSolicitud estado,
        String comentarioAdmin,
        OffsetDateTime fechaSolicitud,
        OffsetDateTime fechaResolucion
) {
}
