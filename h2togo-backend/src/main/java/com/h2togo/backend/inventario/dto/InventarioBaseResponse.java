package com.h2togo.backend.inventario.dto;

import java.util.List;

/** Existencias de la base del negocio, por lote, próximos a caducar primero. */
public record InventarioBaseResponse(List<InventarioItem> lotes) {
}
