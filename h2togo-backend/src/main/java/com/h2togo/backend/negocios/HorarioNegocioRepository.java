package com.h2togo.backend.negocios;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HorarioNegocioRepository extends JpaRepository<HorarioNegocio, Integer> {

    List<HorarioNegocio> findByIdNegocioOrderByDiaSemana(Integer idNegocio);

    void deleteByIdNegocio(Integer idNegocio);
}
