package com.h2togo.backend.zonas;

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
 * Polígono de la zona de servicio (v6, tabla {@code zonas_cobertura}). La columna
 * espacial {@code geom} (geography MultiPolygon) NO se mapea (§1): se carga por
 * script SQL (F3) y se consulta con queries nativas ({@code ST_Covers}).
 */
@Entity
@Table(name = "zonas_cobertura")
@Getter
@Setter
@NoArgsConstructor
public class ZonaCobertura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_zona")
    private Integer id;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}
