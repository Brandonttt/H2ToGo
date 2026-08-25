package com.h2togo.backend.pedidos;

import com.h2togo.backend.common.enums.EstadoPedido;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Bitácora de cambios de estado de un pedido (v6, tabla {@code historial_estados_pedido}).
 * Hijo del agregado {@link Pedido}, solo-INSERT (RN-013). La columna espacial
 * {@code ubicacion} (geography Point, del repartidor al cambiar de estado) NO se mapea (§1):
 * se escribe por query nativa cuando aplica.
 */
@Entity
@Table(name = "historial_estados_pedido")
@Getter
@Setter
@NoArgsConstructor
public class HistorialEstadoPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado", nullable = false, columnDefinition = "estado_pedido")
    private EstadoPedido estado;

    @Column(name = "notas_adicionales", length = 500)
    private String notasAdicionales;

    /** DEFAULT now() en la BD; solo lectura. */
    @Column(name = "fecha_cambio", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaCambio;
}
