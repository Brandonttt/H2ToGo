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
        Double lonEntrega
) {
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
                fechaProgramada, fechaCreacion, detalles, historial, null, null, null, null, null);
    }
}

