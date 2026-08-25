package com.h2togo.backend.common.enums;

/** Estado del pedido (enum nativo {@code estado_pedido} de v6). Transiciones válidas en PedidoService (§3.5). */
public enum EstadoPedido {
    pendiente,
    pendiente_programado,
    asignado,
    en_camino,
    entregado,
    cancelado,
    no_entregado
}
