package com.h2togo.backend.common.enums;

/** Tipo de movimiento de inventario (enum nativo {@code tipo_movimiento} de v6). */
public enum TipoMovimiento {
    entrada_proveedor,
    traspaso_a_vehiculo,
    devolucion_a_base,
    salida_pedido,
    salida_manual,
    ajuste
}
