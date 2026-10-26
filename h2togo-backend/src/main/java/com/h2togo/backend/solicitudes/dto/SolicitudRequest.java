package com.h2togo.backend.solicitudes.dto;

import com.h2togo.backend.common.enums.CodigoCambioPerfil;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * Solicitud de cambio de perfil del negocio por el dueño (CU-020). {@code valorNuevo} lleva
 * los datos del cambio propuesto (su forma depende del {@code codigoCambio}); {@code idVehiculo}
 * indica el vehículo afectado en DATOS_VEHICULO y ELIMINAR_VEHICULO.
 */
public record SolicitudRequest(
        @NotNull CodigoCambioPerfil codigoCambio,
        Integer idVehiculo,
        @NotNull Map<String, Object> valorNuevo
) {
}
