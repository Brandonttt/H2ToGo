package com.h2togo.backend.inventario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lote de garrafones en la base de un negocio (v6, tabla {@code lotes_inventario}).
 * Base del FIFO por {@code fecha_caducidad} (F5). {@code fecha_actualizacion} la maneja
 * el trigger {@code trg_lotes_touch} → solo lectura.
 */
@Entity
@Table(name = "lotes_inventario")
@Getter
@Setter
@NoArgsConstructor
public class LoteInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lote")
    private Integer id;

    @Column(name = "id_negocio", nullable = false)
    private Integer idNegocio;

    @Column(name = "id_marca", nullable = false)
    private Integer idMarca;

    @Column(name = "fecha_caducidad", nullable = false)
    private LocalDate fechaCaducidad;

    /** DEFAULT now() en la BD; solo lectura. */
    @Column(name = "fecha_entrada", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaEntrada;

    @Column(name = "cantidad_inicial", nullable = false)
    private Integer cantidadInicial;

    @Column(name = "cantidad_actual", nullable = false)
    private Integer cantidadActual = 0;

    @Column(name = "cantidad_apartada", nullable = false)
    private Integer cantidadApartada = 0;

    @Column(name = "proveedor", length = 150)
    private String proveedor;

    @Column(name = "costo_unitario", precision = 10, scale = 2)
    private BigDecimal costoUnitario;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    /** La mantiene el trigger {@code trg_lotes_touch}; solo lectura. */
    @Column(name = "fecha_actualizacion", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaActualizacion;
}
