package com.h2togo.backend.pedidos.dto;

import com.h2togo.backend.common.enums.EstadoPedido;
import com.h2togo.backend.common.enums.TipoSolicitudPedido;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Pedido con sus líneas (e historial en la vista de detalle). El total es estimado hasta el cierre (RN-033). */
public record PedidoResponse(
        Integer id,
        EstadoPedido estado,
        TipoSolicitudPedido tipoSolicitud,
        Integer idNegocioSolicitado,
        Integer idRepartidor,
        Integer idDireccionEntrega,
        BigDecimal precioMaximoGarrafon,
        BigDecimal totalPagar,
        Integer garrafonesTotales,
        String indicaciones,
        boolean esProgramado,
        OffsetDateTime fechaProgramada,
        OffsetDateTime fechaCreacion,
        List<DetalleResponse> detalles,
        List<HistorialResponse> historial,
        String nombreCliente,
        String telefonoCliente,
        String direccionTexto,
        Double latEntrega,
        Double lonEntrega,
        /** Purificadora que atiende (la elegida o la del repartidor que aceptó). */
        String nombreNegocio,
        /** Datos del repartidor para el seguimiento del cliente; null mientras no hay asignado. */
        InfoRepartidor repartidor
) {
    /** {@code telefono} solo viaja mientras el pedido está asignado o en camino. */
    public record InfoRepartidor(String nombre, String telefono, String negocio, String vehiculo, String placas) {
    }

    public PedidoResponse(
            Integer id,
            EstadoPedido estado,
            TipoSolicitudPedido tipoSolicitud,
            Integer idNegocioSolicitado,
            Integer idRepartidor,
            Integer idDireccionEntrega,
            BigDecimal precioMaximoGarrafon,
            BigDecimal totalPagar,
            Integer garrafonesTotales,
            String indicaciones,
            boolean esProgramado,
            OffsetDateTime fechaProgramada,
            OffsetDateTime fechaCreacion,
            List<DetalleResponse> detalles,
            List<HistorialResponse> historial) {
        this(id, estado, tipoSolicitud, idNegocioSolicitado, idRepartidor, idDireccionEntrega,
                precioMaximoGarrafon, totalPagar, garrafonesTotales, indicaciones, esProgramado,
                fechaProgramada, fechaCreacion, detalles, historial, null, null, null, null, null, null, null);
    }
}

