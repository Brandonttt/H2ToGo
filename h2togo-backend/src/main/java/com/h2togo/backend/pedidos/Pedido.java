package com.h2togo.backend.pedidos;

import com.h2togo.backend.common.enums.EstadoPedido;
import com.h2togo.backend.common.enums.TipoSolicitudPedido;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Raíz del agregado Pedido (tabla {@code pedidos}). Sus hijos —{@link DetallePedido} e
 * {@link HistorialEstadoPedido}— se manejan a través de esta raíz (no tienen repositorio propio).
 * Los apartados de inventario no son parte del agregado: los manejan por SQL la asignación y la
 * entrega (el cliente no tiene permiso sobre ellos en la BD). Las FKs a otros agregados
 * (cliente, negocio, repartidor, dirección) se mapean como escalares.
 * <p>{@code @DynamicUpdate}: al cancelar, el cliente solo puede cambiar {@code estado_actual}.
 */
@Entity
@Table(name = "pedidos")
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Integer id;

    @Column(name = "id_cliente", nullable = false)
    private Integer idCliente;

    @Column(name = "id_negocio_solicitado")
    private Integer idNegocioSolicitado;

    @Column(name = "id_repartidor")
    private Integer idRepartidor;

    @Column(name = "id_direccion_entrega", nullable = false)
    private Integer idDireccionEntrega;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_solicitud", nullable = false, columnDefinition = "tipo_solicitud_pedido")
    private TipoSolicitudPedido tipoSolicitud = TipoSolicitudPedido.directa;

    @Column(name = "precio_maximo_garrafon", precision = 10, scale = 2)
    private BigDecimal precioMaximoGarrafon;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado_actual", nullable = false, columnDefinition = "estado_pedido")
    private EstadoPedido estadoActual = EstadoPedido.pendiente;

    @Column(name = "total_pagar", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPagar;

    @Column(name = "garrafones_totales", nullable = false)
    private Integer garrafonesTotales;

    @Column(name = "indicaciones_entrega", length = 500)
    private String indicacionesEntrega;

    /** Columna generada en la BD: {@code fecha_programada IS NOT NULL}. */
    @Column(name = "es_programado", insertable = false, updatable = false)
    @Setter(lombok.AccessLevel.NONE)
    private boolean esProgramado;

    @Column(name = "fecha_programada")
    private OffsetDateTime fechaProgramada;

    @Column(name = "notificado_programado", nullable = false)
    private boolean notificadoProgramado = false;

    /** DEFAULT now() en la BD; solo lectura. */
    @Column(name = "fecha_creacion", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_entrega")
    private OffsetDateTime fechaEntrega;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles = new ArrayList<>();

    /** Bitácora (RN-013): se agregan entradas, nunca se borran ni modifican (la BD lo impide). */
    @OneToMany(mappedBy = "pedido", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    private List<HistorialEstadoPedido> historial = new ArrayList<>();

    public void agregarDetalle(DetallePedido detalle) {
        detalle.setPedido(this);
        this.detalles.add(detalle);
    }

    public void agregarHistorial(HistorialEstadoPedido entrada) {
        entrada.setPedido(this);
        this.historial.add(entrada);
    }
}
