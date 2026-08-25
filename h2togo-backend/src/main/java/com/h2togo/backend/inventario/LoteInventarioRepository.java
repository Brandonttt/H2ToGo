package com.h2togo.backend.inventario;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoteInventarioRepository extends JpaRepository<LoteInventario, Integer> {

    List<LoteInventario> findByIdNegocioAndIdMarcaAndActivoTrueOrderByFechaCaducidadAscIdAsc(
            Integer idNegocio, Integer idMarca);
}
