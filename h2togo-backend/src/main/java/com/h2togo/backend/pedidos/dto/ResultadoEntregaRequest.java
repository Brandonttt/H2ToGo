package com.h2togo.backend.pedidos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

/**
 * Resultado de la entrega (CU-012). Para {@code ENTREGADO} se validan lat/lon (≤50 m,
 * RF-014); {@code motivoNoEntrega} se exige en {@code NO_ENTREGADO}. Las líneas permiten
 * entregas parciales y líneas agregadas en sitio (RN-032).
 */
public record ResultadoEntregaRequest(
        @NotNull Resultado resultado,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double lat,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double lon,
        String motivoNoEntrega,
        List<@Valid Linea> lineas
) {

    public enum Resultado {
        ENTREGADO, NO_ENTREGADO
    }

    /**
     * Una línea del resultado. Para líneas existentes se da {@code idDetalle}; para las
     * agregadas en sitio, {@code agregadaEnSitio=true} con {@code idMarca} (RN-032).
     */
    public record Linea(
            Integer idDetalle,
            Integer idMarca,
            @NotNull @PositiveOrZero Integer cantidadEntregada,
            boolean agregadaEnSitio
    ) {
    }
}
