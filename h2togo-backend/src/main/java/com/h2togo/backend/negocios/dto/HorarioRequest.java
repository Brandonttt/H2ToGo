package com.h2togo.backend.negocios.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

/**
 * Un día del horario a definir (RF-031). La consistencia (si {@code cerrado} entonces sin
 * horas, y viceversa; apertura &lt; cierre) se valida en el servicio y la refuerza el CHECK de v6.
 */
public record HorarioRequest(
        @NotNull @Min(1) @Max(7) Short diaSemana,
        LocalTime horaApertura,
        LocalTime horaCierre,
        // Boxed y requerido: evita el fallo de booleanos primitivos omitidos (§10 #21).
        @NotNull Boolean cerrado
) {
}
