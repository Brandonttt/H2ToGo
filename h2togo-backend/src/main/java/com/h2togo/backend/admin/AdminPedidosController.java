package com.h2togo.backend.admin;

import com.h2togo.backend.common.PagedResponse;
import com.h2togo.backend.common.enums.EstadoPedido;
import com.h2togo.backend.pedidos.dto.PedidoResumen;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Historial global de pedidos con filtros combinables (CU-016). Rol ADMIN. */
@RestController
@RequestMapping("/api/v1/admin/pedidos")
public class AdminPedidosController {

    private final AdminPedidosService service;

    public AdminPedidosController(AdminPedidosService service) {
        this.service = service;
    }

    @GetMapping
    public PagedResponse<PedidoResumen> historial(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            @RequestParam(required = false) EstadoPedido estado,
            @RequestParam(required = false) Integer idCliente,
            @RequestParam(required = false) Integer idRepartidor,
            @RequestParam(required = false) Integer idNegocio,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.historial(desde, hasta, estado, idCliente, idRepartidor, idNegocio, pageable);
    }
}
