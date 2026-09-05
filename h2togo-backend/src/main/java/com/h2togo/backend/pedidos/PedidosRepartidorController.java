package com.h2togo.backend.pedidos;

import com.h2togo.backend.common.PagedResponse;
import com.h2togo.backend.pedidos.dto.PedidoDisponibleResponse;
import com.h2togo.backend.pedidos.dto.PedidoResumen;
import com.h2togo.backend.security.SecurityUtils;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Entregas y pedidos disponibles del repartidor (CU-010/013). Rol REPARTIDOR (SecurityConfig). */
@RestController
@RequestMapping("/api/v1/repartidores/me")
public class PedidosRepartidorController {

    private final PedidoService pedidoService;
    private final AsignacionService asignacionService;

    public PedidosRepartidorController(PedidoService pedidoService, AsignacionService asignacionService) {
        this.pedidoService = pedidoService;
        this.asignacionService = asignacionService;
    }

    @GetMapping("/entregas")
    public PagedResponse<PedidoResumen> misEntregas(@PageableDefault(size = 20) Pageable pageable) {
        return pedidoService.misEntregas(SecurityUtils.idActual(), pageable);
    }

    @GetMapping("/pedidos-disponibles")
    public List<PedidoDisponibleResponse> disponibles() {
        return asignacionService.disponibles(SecurityUtils.idActual());
    }
}
