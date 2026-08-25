package com.h2togo.backend.vehiculos;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehiculoNegocioRepository extends JpaRepository<VehiculoNegocio, Integer> {

    List<VehiculoNegocio> findByIdNegocioAndActivoTrue(Integer idNegocio);
}
