package com.h2togo.backend.negocios;

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

/**
 * Purificadora (v6, tabla {@code negocios}). {@code id_dueno} se mapea como escalar:
 * participa en el ciclo diferido con {@code repartidores.id_negocio}. La ubicación de
 * la base ({@code ubicacion_base}, geography) NO se mapea (§1); el alta con dirección
 * completa se hace por query nativa (respeta el CHECK de dirección all-or-nothing).
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

    @Column(name = "id_dueno", nullable = false)
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
