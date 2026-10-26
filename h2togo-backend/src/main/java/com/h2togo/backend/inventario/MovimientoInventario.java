package com.h2togo.backend.inventario;

import com.h2togo.backend.common.enums.TipoMovimiento;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Bitácora de movimientos de inventario (tabla {@code movimientos_inventario}). Solo INSERT
 * (RN-013; un trigger rechaza UPDATE y DELETE). {@code cantidad} siempre positiva; el {@code tipo}
 * determina la dirección y si interviene un vehículo (CHECK). Proveedor y costo se leen del lote.
 */
@Entity
@Table(name = "movimientos_inventario")
@Getter
@Setter
@NoArgsConstructor
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Integer id;

    /** v7 [R2]: siempre se conoce el lote. */
    @Column(name = "id_lote", nullable = false)
    private Integer idLote;

    /** NULL si el movimiento solo toca la base. */
    @Column(name = "id_vehiculo")
    private Integer idVehiculo;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo", nullable = false, columnDefinition = "tipo_movimiento")
    private TipoMovimiento tipo;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "id_pedido")
    private Integer idPedido;

    @Column(name = "id_repartidor_responsable")
    private Integer idRepartidorResponsable;

    @Column(name = "notas", length = 255)
    private String notas;

    /** DEFAULT now() en la BD; solo lectura. */
    @Column(name = "fecha", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fecha;
}
