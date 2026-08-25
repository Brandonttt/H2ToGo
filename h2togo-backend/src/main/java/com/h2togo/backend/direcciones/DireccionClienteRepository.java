package com.h2togo.backend.direcciones;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DireccionClienteRepository extends JpaRepository<DireccionCliente, Integer> {

    List<DireccionCliente> findByIdClienteAndActivoTrue(Integer idCliente);
}
