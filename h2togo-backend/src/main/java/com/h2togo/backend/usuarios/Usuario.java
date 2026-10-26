package com.h2togo.backend.usuarios;

import com.h2togo.backend.common.enums.RolUsuario;
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
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Núcleo de identidad y sesión, común a los tres actores (v6, tabla {@code usuarios}).
 * Los datos exclusivos de cada actor viven en los subtipos {@link Cliente} y {@link Repartidor};
 * el administrador no tiene datos propios ni subtabla (v7 [R9]).
 * <p>{@code @DynamicUpdate}: el UPDATE lleva solo las columnas que cambiaron, porque cada rol de
 * la BD solo puede modificar algunas (p. ej. nadie salvo el admin cambia {@code rol}).
 */
@Entity
@Table(name = "usuarios")
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "correo", nullable = false, unique = true, length = 150)
    private String correo;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "telefono", nullable = false, unique = true, length = 20)
    private String telefono;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "rol", nullable = false, columnDefinition = "rol_usuario")
    private RolUsuario rol;

    @Column(name = "url_foto_perfil", length = 500)
    private String urlFotoPerfil;

    @Column(name = "telefono_verificado", nullable = false)
    private boolean telefonoVerificado = false;

    @Column(name = "cuenta_activa", nullable = false)
    private boolean cuentaActiva = true;

    @Column(name = "codigo_verificacion", length = 10)
    private String codigoVerificacion;

    @Column(name = "codigo_verificacion_expiracion")
    private OffsetDateTime codigoVerificacionExpiracion;

    /** Intentos fallidos con el código actual; al llegar al límite el código se invalida. */
    @Column(name = "codigo_intentos", nullable = false)
    private short codigoIntentos = 0;

    @Column(name = "motivo_baja", length = 255)
    private String motivoBaja;

    @Column(name = "fecha_baja")
    private OffsetDateTime fechaBaja;

    /** DEFAULT now() en la BD; solo lectura desde el backend. */
    @Column(name = "fecha_registro", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime fechaRegistro;

    /** SHA-256 del token opaco ({@link com.h2togo.backend.security.TokenSesion}); el token en claro no se guarda. */
    @Column(name = "token_sesion_hash", unique = true, length = 64)
    private String tokenSesionHash;

    @Column(name = "token_fcm", length = 255)
    private String tokenFcm;

    @Column(name = "sesion_fecha_creacion")
    private OffsetDateTime sesionFechaCreacion;

    @Column(name = "sesion_fecha_expiracion")
    private OffsetDateTime sesionFechaExpiracion;
}
