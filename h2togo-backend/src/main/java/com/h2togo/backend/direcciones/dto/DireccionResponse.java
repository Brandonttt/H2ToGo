package com.h2togo.backend.direcciones.dto;

/**
 * Dirección del cliente. {@code enZonaCobertura} la deriva el trigger (RN-001); si es
 * {@code false} la app avisa que está fuera de cobertura (el pedido se bloquea en F6).
 */
public record DireccionResponse(
        Integer id,
        String alias,
        String calle,
        String numeroExterior,
        String numeroInterior,
        String colonia,
        String codigoPostal,
        String referencias,
        double lat,
        double lon,
        boolean enZonaCobertura,
        boolean activo
) {
}
