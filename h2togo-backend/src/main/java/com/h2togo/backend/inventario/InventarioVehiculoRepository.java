package com.h2togo.backend.inventario;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventarioVehiculoRepository extends JpaRepository<InventarioVehiculo, Integer> {

    List<InventarioVehiculo> findByIdVehiculoAndActivoTrue(Integer idVehiculo);
}
