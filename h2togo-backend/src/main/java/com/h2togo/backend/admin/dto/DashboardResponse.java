package com.h2togo.backend.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Panorama operativo del panel de administración. Los cortes "hoy"/"ayer" se calculan en la
 * zona horaria de operación (America/Mexico_City), no en la del servidor.
 */
public record DashboardResponse(
        OffsetDateTime generadoEn,
        long pedidosHoy,
        long pedidosAyer,
        BigDecimal ingresosHoy,
        BigDecimal ingresosAyer,
        /** entregados / (entregados + no_entregados) de los últimos 30 días; null si no hay cierres. */
        Double tasaExito30d,
        long usuariosNuevos7d,
        long usuariosTotales,
        long solicitudesPendientes,
        /** Conteo global por estado actual (todas las fechas). */
        Map<String, Long> estadoActual,
        /** Conteo por estado de los pedidos creados hoy. */
        Map<String, Long> distribucionHoy,
        long repartidoresEnLinea,
        long repartidoresActivos,
        List<PuntoSerie> serie30d,
        List<TopRepartidor> topRepartidores,
        List<Actividad> actividad,
        List<PuntoMapa> mapa
) {

    public record PuntoSerie(LocalDate fecha, long total, long entregados) {
    }

    public record TopRepartidor(Integer id, String nombre, String negocio, long entregas, BigDecimal ingresos) {
    }

    public record Actividad(Integer idPedido, String estado, String notas, OffsetDateTime fecha) {
    }

    /** {@code tipo}: "repartidor" (en línea) o "pedido" (activo, en el domicilio de entrega). */
    public record PuntoMapa(String tipo, Integer id, String etiqueta, double lat, double lon) {
    }
}
