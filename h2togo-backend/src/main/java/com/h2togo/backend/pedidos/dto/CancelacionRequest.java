package com.h2togo.backend.pedidos.dto;

/** Cancelación de un pedido (CU-005); el motivo es opcional. */
public record CancelacionRequest(String motivo) {
}
