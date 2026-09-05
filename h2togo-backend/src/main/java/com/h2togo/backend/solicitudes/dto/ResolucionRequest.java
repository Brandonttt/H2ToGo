package com.h2togo.backend.solicitudes.dto;

import jakarta.validation.constraints.NotNull;

/** Resolución de una solicitud por el admin (CU-021). El comentario se exige al rechazar. */
public record ResolucionRequest(
        @NotNull Decision decision,
        String comentario
) {

    public enum Decision {
        APROBADO, RECHAZADO
    }
}
