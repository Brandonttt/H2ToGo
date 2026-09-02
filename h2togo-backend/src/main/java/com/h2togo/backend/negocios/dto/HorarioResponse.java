package com.h2togo.backend.negocios.dto;

import java.time.LocalTime;

/** Un día del horario semanal del negocio (dia_semana 1=lunes … 7=domingo). */
public record HorarioResponse(
        short diaSemana,
        LocalTime horaApertura,
        LocalTime horaCierre,
        boolean cerrado
) {
}
