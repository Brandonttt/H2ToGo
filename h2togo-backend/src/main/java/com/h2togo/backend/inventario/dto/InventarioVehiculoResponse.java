package com.h2togo.backend.inventario.dto;

import java.util.List;

/**
 * Existencias del vehículo del repartidor. {@code capacidadGarrafones} es la capacidad del
 * vehículo; {@code ocupado} la suma cargada; {@code libre} lo que aún cabe.
 */
public record InventarioVehiculoResponse(
        Integer idVehiculo,
        int capacidadGarrafones,
        int ocupado,
        int libre,
        List<InventarioItem> lotes
) {
}
