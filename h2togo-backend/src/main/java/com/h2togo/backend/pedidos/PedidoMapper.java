package com.h2togo.backend.pedidos;

import com.h2togo.backend.pedidos.dto.DetalleResponse;
import com.h2togo.backend.pedidos.dto.HistorialResponse;
import com.h2togo.backend.pedidos.dto.PedidoResponse;
import com.h2togo.backend.pedidos.dto.PedidoResumen;
import java.util.Comparator;
import java.util.List;

/** Mapeo estático del agregado Pedido a DTOs (§3.2). */
public final class PedidoMapper {

    private PedidoMapper() {
    }

    public static PedidoResponse toResponse(Pedido p) {
        List<DetalleResponse> detalles = p.getDetalles().stream()
                .sorted(Comparator.comparing(DetallePedido::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(PedidoMapper::toDetalle)
                .toList();
        List<HistorialResponse> historial = p.getHistorial().stream()
                .sorted(Comparator.comparing(HistorialEstadoPedido::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(h -> new HistorialResponse(h.getEstado(), h.getFechaCambio(), h.getNotasAdicionales()))
                .toList();
        return new PedidoResponse(
                p.getId(), p.getEstadoActual(), p.getTipoSolicitud(), p.getIdNegocioSolicitado(),
                p.getIdRepartidor(), p.getIdDireccionEntrega(), p.getPrecioMaximoGarrafon(),
                p.getTotalPagar(), p.getGarrafonesTotales(), p.getIndicacionesEntrega(),
                p.isEsProgramado(), p.getFechaProgramada(), p.getFechaCreacion(), detalles, historial);
    }

    public static PedidoResumen toResumen(Pedido p) {
        return new PedidoResumen(p.getId(), p.getEstadoActual(), p.getTipoSolicitud(),
                p.getTotalPagar(), p.getGarrafonesTotales(), p.isEsProgramado(), p.getFechaCreacion());
    }

    private static DetalleResponse toDetalle(DetallePedido d) {
        return new DetalleResponse(d.getId(), d.getIdMarca(), d.getCantidadSolicitada(),
                d.getCantidadEntregada(), d.isTieneEnvase(), d.getPrecioUnitario(),
                d.getPrecioEnvaseUnitario(), d.isAgregadaEnSitio());
    }
}
