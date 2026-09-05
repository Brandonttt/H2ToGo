package com.h2togo.backend.solicitudes.dto;

import com.h2togo.backend.common.enums.CodigoCambioPerfil;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * Solicitud de cambio de perfil del negocio por el dueño (CU-020). {@code valorNuevo} lleva
 * los datos del cambio propuesto (su forma depende del {@code codigoCambio}); {@code idVehiculo}/
 * {@code idProductoNegocio} referencian la entidad afectada cuando aplica.
 */
public record SolicitudRequest(
        @NotNull CodigoCambioPerfil codigoCambio,
        Integer idVehiculo,
        Integer idProductoNegocio,
        @NotNull Map<String, Object> valorNuevo
) {
}
