package com.h2togo.backend.usuarios;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Subtipo cliente (v6, tabla {@code clientes}). Comparte PK con {@link Usuario}
 * vía {@code @MapsId}. La columna {@code rol} NO se mapea: la fija el DEFAULT del
 * esquema ('cliente') y la valida la FK compuesta (id_usuario, rol).
 */
@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    @Id
    @Column(name = "id_usuario")
    private Integer id;

    @MapsId
    @OneToOne
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "ausencias_consecutivas", nullable = false)
    private int ausenciasConsecutivas = 0;

    @Column(name = "suspendido_hasta")
    private OffsetDateTime suspendidoHasta;
}
