package com.h2togo.backend.negocios;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NegocioRepository extends JpaRepository<Negocio, Integer> {

    List<Negocio> findByActivoTrue();
}
