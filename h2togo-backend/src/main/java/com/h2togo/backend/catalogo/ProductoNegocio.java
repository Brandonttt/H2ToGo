package com.h2togo.backend.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Producto (marca) que ofrece un negocio, con su precio (v6, tabla {@code productos_negocio}).
 * {@code precio} es el agua con envase propio del cliente (RN-025); {@code precio_envase}
 * es el adicional del envase cuando no lo trae (RN-031).
 */
@Entity
@Table(name = "productos_negocio")
@Getter
@Setter
@NoArgsConstructor
public class ProductoNegocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto_negocio")
    private Integer id;

    @Column(name = "id_negocio", nullable = false)
    private Integer idNegocio;

    @Column(name = "id_marca", nullable = false)
    private Integer idMarca;

    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "precio_envase", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioEnvase;

    @Column(name = "capacidad_maxima", nullable = false)
    private Integer capacidadMaxima;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}
