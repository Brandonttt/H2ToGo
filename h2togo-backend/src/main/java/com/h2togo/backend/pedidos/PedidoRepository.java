package com.h2togo.backend.pedidos;

import com.h2togo.backend.common.enums.EstadoPedido;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio del agregado Pedido. Los hijos ({@code DetallePedido}, {@code ApartadoPedido},
 * {@code HistorialEstadoPedido}) se gestionan a través de la raíz, no tienen repositorio propio.
 */
public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    Page<Pedido> findByIdClienteOrderByFechaCreacionDesc(Integer idCliente, Pageable pageable);

    Page<Pedido> findByIdRepartidorOrderByFechaCreacionDesc(Integer idRepartidor, Pageable pageable);

    List<Pedido> findByEstadoActual(EstadoPedido estadoActual);
}
