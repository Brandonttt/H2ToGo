package com.h2togo.backend.pedidos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Apartado de inventario para un pedido (v6, tabla {@code apartados_pedido}). Hijo del
 * agregado {@link Pedido}. El origen es exclusivamente la base ({@code id_lote_base}) o
 * el vehículo ({@code id_inventario_vehiculo}) — nunca ambos (CHECK del esquema).
 */
@Entity
@Table(name = "apartados_pedido")
@Getter
@Setter
@NoArgsConstructor
public class ApartadoPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_apartado")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @Column(name = "id_lote_base")
    private Integer idLoteBase;

    @Column(name = "id_inventario_vehiculo")
    private Integer idInventarioVehiculo;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    /** DEFAULT now() en la BD; solo lectura. */
    @Column(name = "fecha_apartado", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaApartado;
}
