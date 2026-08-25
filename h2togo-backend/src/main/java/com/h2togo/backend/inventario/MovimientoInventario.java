package com.h2togo.backend.inventario;

import com.h2togo.backend.common.enums.TipoMovimiento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Bitácora de movimientos de inventario (v6, tabla {@code movimientos_inventario}).
 * Solo-INSERT (RN-013). {@code cantidad} siempre positiva; el {@code tipo} determina la
 * dirección y qué columnas de origen (lote base / inventario de vehículo) aplican (CHECK).
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

    @Column(name = "id_lote_base")
    private Integer idLoteBase;

    @Column(name = "id_inventario_vehiculo")
    private Integer idInventarioVehiculo;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo", nullable = false, columnDefinition = "tipo_movimiento")
    private TipoMovimiento tipo;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "id_pedido")
    private Integer idPedido;

    @Column(name = "id_repartidor_responsable")
    private Integer idRepartidorResponsable;

    @Column(name = "costo_unitario", precision = 10, scale = 2)
    private BigDecimal costoUnitario;

    @Column(name = "proveedor", length = 150)
    private String proveedor;

    @Column(name = "notas", length = 255)
    private String notas;

    /** DEFAULT now() en la BD; solo lectura. */
    @Column(name = "fecha", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fecha;
}
