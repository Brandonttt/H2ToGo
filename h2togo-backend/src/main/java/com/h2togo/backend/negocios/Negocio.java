package com.h2togo.backend.negocios;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;

/**
 * Purificadora (tabla {@code negocios}). El dueño ya no es una columna (v7 [R1]): es el
 * repartidor del negocio con {@code es_dueno}; {@link #idDueno} se lee con una subconsulta y no
 * se escribe. La ubicación de la base ({@code ubicacion_base}, geography) NO se mapea (§1); el
 * alta con dirección completa se hace por query nativa (respeta el CHECK de dirección all-or-nothing).
 */
@Entity
@Table(name = "negocios")
@Getter
@Setter
@NoArgsConstructor
public class Negocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_negocio")
    private Integer id;

    @Column(name = "nombre_comercial", nullable = false, length = 150)
    private String nombreComercial;

    /** Solo lectura. Para cambiar de dueño se actualiza {@code repartidores.es_dueno}. */
    @Formula("(SELECT r.id_usuario FROM repartidores r WHERE r.id_negocio = id_negocio AND r.es_dueno)")
    @Setter(AccessLevel.NONE)
    private Integer idDueno;

    @Column(name = "calle", length = 150)
    private String calle;

    @Column(name = "numero_exterior", length = 20)
    private String numeroExterior;

    @Column(name = "numero_interior", length = 20)
    private String numeroInterior;

    @Column(name = "colonia", length = 100)
    private String colonia;

    @Column(name = "codigo_postal", length = 10)
    private String codigoPostal;

    @Column(name = "referencias")
    private String referencias;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    /** DEFAULT now() en la BD; solo lectura. */
    @Column(name = "fecha_creacion", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;
}
