package com.h2togo.backend.usuarios;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepartidorRepository extends JpaRepository<Repartidor, Integer> {

    List<Repartidor> findByIdNegocio(Integer idNegocio);
}
