package com.h2togo.backend.solicitudes;

import com.h2togo.backend.common.enums.CodigoCambioPerfil;
import com.h2togo.backend.common.enums.EstadoSolicitud;
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
 * Solicitud de cambio de perfil del negocio (v6, tabla {@code solicitudes_cambio_perfil}).
 * El solicitante siempre es {@code negocios.id_dueno} (RN-021); solo un administrador la
 * revisa. Aprobar aplica el cambio en la misma transacción (F10).
 */
@Entity
@Table(name = "solicitudes_cambio_perfil")
@Getter
@Setter
@NoArgsConstructor
public class SolicitudCambioPerfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Integer id;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "codigo_cambio", nullable = false, columnDefinition = "codigo_cambio_perfil")
    private CodigoCambioPerfil codigoCambio;

    @Column(name = "id_negocio", nullable = false)
    private Integer idNegocio;

    @Column(name = "id_admin_revisor")
    private Integer idAdminRevisor;

    @Column(name = "id_vehiculo")
    private Integer idVehiculo;

    @Column(name = "id_producto_negocio")
    private Integer idProductoNegocio;

    @Column(name = "valor_anterior")
    private String valorAnterior;

    @Column(name = "valor_nuevo", nullable = false)
    private String valorNuevo;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado", nullable = false, columnDefinition = "estado_solicitud")
    private EstadoSolicitud estado = EstadoSolicitud.pendiente;

    @Column(name = "comentario_admin", length = 500)
    private String comentarioAdmin;

    /** DEFAULT now() en la BD; solo lectura. */
    @Column(name = "fecha_solicitud", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaSolicitud;

    @Column(name = "fecha_resolucion")
    private OffsetDateTime fechaResolucion;
}
