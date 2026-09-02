package com.h2togo.backend.direcciones.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta/edición de una dirección del cliente (soporte CU-004). Las coordenadas vienen
 * del mapa (RN-003: no se captura manualmente). {@code referencias} es obligatorio
 * (RN-003, §10 #19). La zona de cobertura la calcula el trigger, no el cliente.
 */
public record DireccionRequest(
        @NotBlank @Size(max = 50) String alias,
        @NotBlank @Size(max = 150) String calle,
        @NotBlank @Size(max = 20) String numeroExterior,
        @Size(max = 20) String numeroInterior,
        @NotBlank @Size(max = 100) String colonia,
        @NotBlank @Size(max = 10) String codigoPostal,
        @NotBlank String referencias,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double lat,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double lon
) {
}
