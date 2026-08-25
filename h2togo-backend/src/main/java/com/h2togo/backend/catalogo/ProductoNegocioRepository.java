package com.h2togo.backend.catalogo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoNegocioRepository extends JpaRepository<ProductoNegocio, Integer> {

    List<ProductoNegocio> findByIdNegocioAndActivoTrue(Integer idNegocio);

    Optional<ProductoNegocio> findByIdNegocioAndIdMarca(Integer idNegocio, Integer idMarca);
}
