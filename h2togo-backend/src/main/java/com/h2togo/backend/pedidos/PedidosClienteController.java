package com.h2togo.backend.pedidos;

import com.h2togo.backend.common.PagedResponse;
import com.h2togo.backend.common.enums.RolUsuario;
import com.h2togo.backend.pedidos.dto.CancelacionRequest;
import com.h2togo.backend.pedidos.dto.PedidoCreateRequest;
import com.h2togo.backend.pedidos.dto.PedidoResponse;
import com.h2togo.backend.pedidos.dto.PedidoResumen;
import com.h2togo.backend.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pedidos del cliente (CU-004/005/007) + detalle compartido (CU-006/007). Crear y cancelar
 * exigen rol CLIENTE (se resuelve del token); el detalle aplica ownership por rol (RN-016).
 */
@RestController
@RequestMapping("/api/v1")
public class PedidosClienteController {

    private final PedidoService pedidoService;

    public PedidosClienteController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping("/pedidos")
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse crear(@Valid @RequestBody PedidoCreateRequest request) {
        return pedidoService.crear(SecurityUtils.exigirRol(RolUsuario.cliente), request);
    }

    @PostMapping("/pedidos/{id}/cancelacion")
    public PedidoResponse cancelar(@PathVariable int id,
            @RequestBody(required = false) CancelacionRequest request) {
        String motivo = request == null ? null : request.motivo();
        return pedidoService.cancelar(SecurityUtils.exigirRol(RolUsuario.cliente), id, motivo);
    }

    @GetMapping("/pedidos/{id}")
    public PedidoResponse detalle(@PathVariable int id) {
        return pedidoService.detalle(id);
    }

    @GetMapping("/clientes/me/pedidos")
    public PagedResponse<PedidoResumen> misPedidos(@PageableDefault(size = 20) Pageable pageable) {
        return pedidoService.misPedidos(SecurityUtils.idActual(), pageable);
    }
}
