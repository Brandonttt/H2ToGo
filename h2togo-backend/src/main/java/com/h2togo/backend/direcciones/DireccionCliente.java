package com.h2togo.backend.direcciones;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dirección de entrega de un cliente (v6, tabla {@code direcciones_clientes}).
 * <p>La columna espacial {@code ubicacion} (geography Point) NO se mapea (§1): el
 * alta/edición usa query nativa con {@code ST_MakePoint} (F3). {@code en_zona_cobertura}
 * es derivada por trigger → solo lectura ({@code insertable/updatable=false}).
 */
@Entity
@Table(name = "direcciones_clientes")
@Getter
@Setter
@NoArgsConstructor
public class DireccionCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_direccion")
    private Integer id;

    @Column(name = "id_cliente", nullable = false)
    private Integer idCliente;

    @Column(name = "alias", nullable = false, length = 50)
    private String alias;

    @Column(name = "calle", nullable = false, length = 150)
    private String calle;

    @Column(name = "numero_exterior", nullable = false, length = 20)
    private String numeroExterior;

    @Column(name = "numero_interior", length = 20)
    private String numeroInterior;

    @Column(name = "colonia", nullable = false, length = 100)
    private String colonia;

    @Column(name = "codigo_postal", nullable = false, length = 10)
    private String codigoPostal;

    @Column(name = "referencias")
    private String referencias;

    /** Derivada por el trigger {@code trg_direcciones_zona}; solo lectura. */
    @Column(name = "en_zona_cobertura", nullable = false, insertable = false, updatable = false)
    private boolean enZonaCobertura;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}
