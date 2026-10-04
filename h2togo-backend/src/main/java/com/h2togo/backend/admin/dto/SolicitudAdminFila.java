package com.h2togo.backend.admin.dto;

import com.h2togo.backend.common.enums.CodigoCambioPerfil;
import com.h2togo.backend.common.enums.EstadoSolicitud;
import java.time.OffsetDateTime;

/**
 * Solicitud de cambio de perfil para el panel (CU-021): los campos de {@code SolicitudResponse}
 * más el nombre del negocio y de su dueño (el solicitante, RN-021).
 */
public record SolicitudAdminFila(
        Integer id,
        CodigoCambioPerfil codigoCambio,
        Integer idNegocio,
        String negocio,
        Integer idSolicitante,
        String solicitante,
        Integer idVehiculo,
        Integer idProductoNegocio,
        String valorAnterior,
        String valorNuevo,
        EstadoSolicitud estado,
        String comentarioAdmin,
        String revisor,
        OffsetDateTime fechaSolicitud,
        OffsetDateTime fechaResolucion
) {
}
