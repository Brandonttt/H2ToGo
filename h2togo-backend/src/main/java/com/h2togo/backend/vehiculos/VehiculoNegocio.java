package com.h2togo.backend.vehiculos;

import com.h2togo.backend.common.enums.TipoVehiculo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Vehículo de reparto de un negocio (v6, tabla {@code vehiculos_negocio}). Las
 * altas/bajas van por solicitudes (F10). El vehículo en uso por un repartidor debe
 * pertenecer a su negocio (FK compuesta en el esquema).
 */
@Entity
@Table(name = "vehiculos_negocio")
@Getter
@Setter
@NoArgsConstructor
public class VehiculoNegocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vehiculo")
    private Integer id;

    @Column(name = "id_negocio", nullable = false)
    private Integer idNegocio;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_vehiculo", nullable = false, columnDefinition = "tipo_vehiculo")
    private TipoVehiculo tipoVehiculo;

    @Column(name = "marca", nullable = false, length = 50)
    private String marca;

    @Column(name = "modelo", length = 50)
    private String modelo;

    @Column(name = "color", nullable = false, length = 30)
    private String color;

    @Column(name = "placas", length = 20)
    private String placas;

    @Column(name = "capacidad_garrafones", nullable = false)
    private Integer capacidadGarrafones;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}
