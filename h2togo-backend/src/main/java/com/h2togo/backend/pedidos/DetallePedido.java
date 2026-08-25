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
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Línea de un pedido (v6, tabla {@code detalles_pedido}). Hijo del agregado {@link Pedido}.
 * Los precios son snapshot al momento del pedido (RN-025/031). Soporta entregas parciales
 * y líneas agregadas en sitio (RN-032) mediante {@code cantidad_entregada}/{@code agregada_en_sitio}.
 */
@Entity
@Table(name = "detalles_pedido")
@Getter
@Setter
@NoArgsConstructor
public class DetallePedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @Column(name = "id_marca", nullable = false)
    private Integer idMarca;

    @Column(name = "cantidad_solicitada", nullable = false)
    private Integer cantidadSolicitada;

    @Column(name = "cantidad_entregada")
    private Integer cantidadEntregada;

    @Column(name = "tiene_envase", nullable = false)
    private boolean tieneEnvase;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "precio_envase_unitario", precision = 10, scale = 2)
    private BigDecimal precioEnvaseUnitario;

    @Column(name = "agregada_en_sitio", nullable = false)
    private boolean agregadaEnSitio = false;
}
