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
import org.hibernate.annotations.DynamicUpdate;

/**
 * Subtipo repartidor + datos operativos (v6, tabla {@code repartidores}). Comparte PK
 * con {@link Usuario} vía {@code @MapsId}.
 * <p>{@code id_negocio} se mapea como escalar (no como asociación). El dueño del negocio es el
 * repartidor con {@code es_dueno} (v7: ya no hay {@code negocios.id_dueno} ni ciclo); solo lo
 * cambian el registro y el admin. {@code @DynamicUpdate}: el repartidor solo puede modificar sus
 * columnas operativas. {@code ubicacion_actual} (geography) no se mapea: viaja por queries nativas (§1).
 */
@Entity
@Table(name = "repartidores")
@DynamicUpdate
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

    /** FALSE mientras el dueño no aprueba a quien se unió a una purificadora existente. */
    @Column(name = "es_dueno", nullable = false)
    private boolean esDueno = false;

    @Column(name = "aprobado", nullable = false)
    private boolean aprobado = true;

    @Column(name = "estado_operativo", nullable = false)
    private boolean estadoOperativo = false;

    @Column(name = "id_vehiculo_actual")
    private Integer idVehiculoActual;

    @Column(name = "ubicacion_reportada_en")
    private OffsetDateTime ubicacionReportadaEn;
}
