package com.h2togo.backend.usuarios;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> findByTelefono(String telefono);

    Optional<Usuario> findByTokenSesion(String tokenSesion);

    boolean existsByCorreo(String correo);

    boolean existsByTelefono(String telefono);
}
