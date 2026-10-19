package com.h2togo.backend.planeacion.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Pedido programado de hoy en la agenda del repartidor: libre para tomarlo o ya reservado por él.
 * {@code distanciaM}: desde su ubicación actual (o la base del negocio).
 */
public record AgendaPedidoResponse(
        Integer idPedido,
        OffsetDateTime fechaProgramada,
        String estado,
        boolean reservadoPorMi,
        Integer garrafonesTotales,
        BigDecimal totalEstimado,
        String tipoSolicitud,
        String nombreCliente,
        String telefonoCliente,
        String direccion,
        String colonia,
        Double latEntrega,
        Double lonEntrega,
        Double distanciaM
) {
}
