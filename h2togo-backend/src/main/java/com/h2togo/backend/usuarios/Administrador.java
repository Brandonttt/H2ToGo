package com.h2togo.backend.usuarios;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Subtipo administrador (v6, tabla {@code administradores}). Sin atributos propios;
 * existe para que las FKs que exigen rol admin (p. ej. {@code id_admin_revisor})
 * apunten aquí. La columna {@code rol} la fija el DEFAULT del esquema ('admin').
 */
@Entity
@Table(name = "administradores")
@Getter
@Setter
@NoArgsConstructor
public class Administrador {

    @Id
    @Column(name = "id_usuario")
    private Integer id;

    @MapsId
    @OneToOne
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;
}
