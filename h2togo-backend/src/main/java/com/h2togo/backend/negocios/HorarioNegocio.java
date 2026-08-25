package com.h2togo.backend.negocios;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Horario semanal de un negocio (v6, tabla {@code horarios_negocio}). Una fila por día
 * ({@code dia_semana} 1=lunes … 7=domingo). El CHECK de consistencia exige que si
 * {@code cerrado} entonces las horas van nulas, y viceversa (se valida en F4).
 */
@Entity
@Table(name = "horarios_negocio")
@Getter
@Setter
@NoArgsConstructor
public class HorarioNegocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario")
    private Integer id;

    @Column(name = "id_negocio", nullable = false)
    private Integer idNegocio;

    @Column(name = "dia_semana", nullable = false)
    private Short diaSemana;

    @Column(name = "hora_apertura")
    private LocalTime horaApertura;

    @Column(name = "hora_cierre")
    private LocalTime horaCierre;

    @Column(name = "cerrado", nullable = false)
    private boolean cerrado = false;
}
