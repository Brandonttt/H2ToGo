package com.h2togo.backend.inventario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Existencias de un lote cargadas en un vehículo (tabla {@code inventario_vehiculo}). Llave
 * natural (vehículo, lote) (v7 [R3]); {@code id_negocio} repite el del lote para que la BD exija
 * que lote y vehículo sean del mismo negocio. Las existencias se mueven por SQL en
 * {@link InventarioService} y en la entrega; esta entidad documenta el mapeo y lo valida Hibernate.
 */
@Entity
@Table(name = "inventario_vehiculo")
@IdClass(InventarioVehiculo.Llave.class)
@Getter
@Setter
@NoArgsConstructor
public class InventarioVehiculo {

    @Id
    @Column(name = "id_vehiculo")
    private Integer idVehiculo;

    @Id
    @Column(name = "id_lote")
    private Integer idLote;

    @Column(name = "id_negocio", nullable = false)
    private Integer idNegocio;

    @Column(name = "cantidad_actual", nullable = false)
    private Integer cantidadActual = 0;

    @Column(name = "cantidad_apartada", nullable = false)
    private Integer cantidadApartada = 0;

    /** La mantiene el trigger {@code trg_inventario_vehiculo_touch}; solo lectura. */
    @Column(name = "fecha_actualizacion", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaActualizacion;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Llave implements Serializable {
        private Integer idVehiculo;
        private Integer idLote;
    }
}
