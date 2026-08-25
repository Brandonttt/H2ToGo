package com.h2togo.backend.inventario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Existencias de un lote cargadas en un vehículo (v6, tabla {@code inventario_vehiculo}).
 * La marca y el negocio se obtienen vía {@link LoteInventario}. {@code fecha_actualizacion}
 * la maneja el trigger {@code trg_inventario_vehiculo_touch} → solo lectura.
 */
@Entity
@Table(name = "inventario_vehiculo")
@Getter
@Setter
@NoArgsConstructor
public class InventarioVehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_inventario_vehiculo")
    private Integer id;

    @Column(name = "id_vehiculo", nullable = false)
    private Integer idVehiculo;

    @Column(name = "id_lote", nullable = false)
    private Integer idLote;

    @Column(name = "cantidad_actual", nullable = false)
    private Integer cantidadActual = 0;

    @Column(name = "cantidad_apartada", nullable = false)
    private Integer cantidadApartada = 0;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    /** La mantiene el trigger {@code trg_inventario_vehiculo_touch}; solo lectura. */
    @Column(name = "fecha_actualizacion", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaActualizacion;
}
