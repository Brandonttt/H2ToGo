package com.h2togo.backend.usuarios;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Subtipo repartidor + datos operativos (v6, tabla {@code repartidores}). Comparte PK
 * con {@link Usuario} vía {@code @MapsId}.
 * <p>{@code id_negocio} se mapea como escalar (no como asociación): participa en el
 * ciclo diferido {@code negocios.id_dueno ↔ repartidores.id_negocio}, cuyo orden de
 * inserción se controla mejor con el id crudo (ver test de alta dueño+negocio).
 * {@code ubicacion_actual} (geography) no se mapea: viaja por queries nativas (§1).
 */
@Entity
@Table(name = "repartidores")
@Getter
@Setter
@NoArgsConstructor
public class Repartidor {

    @Id
    @Column(name = "id_usuario")
    private Integer id;

    @MapsId
    @OneToOne
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "id_negocio", nullable = false)
    private Integer idNegocio;

    @Column(name = "estado_operativo", nullable = false)
    private boolean estadoOperativo = false;

    @Column(name = "id_vehiculo_actual")
    private Integer idVehiculoActual;

    @Column(name = "ubicacion_reportada_en")
    private OffsetDateTime ubicacionReportadaEn;
}
