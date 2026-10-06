package com.h2togo.backend.pedidos;

import com.h2togo.backend.notificaciones.PushService;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Avisa "nuevo pedido disponible" a los repartidores en línea que pueden tomarlo, con el mismo
 * criterio que la lista de disponibles: los del negocio elegido (directa) o, en modalidad
 * abierta, los de negocios que ofrecen todas las marcas al precio máximo o menos (RN-025).
 */
@Component
public class AvisoNuevoPedido {

    private final NamedParameterJdbcTemplate jdbc;
    private final PushService pushService;

    public AvisoNuevoPedido(NamedParameterJdbcTemplate jdbc, PushService pushService) {
        this.jdbc = jdbc;
        this.pushService = pushService;
    }

    public void avisar(int idPedido) {
        List<Integer> repartidores = jdbc.queryForList("""
                SELECT r.id_usuario
                FROM pedidos p
                JOIN repartidores r ON r.estado_operativo
                JOIN usuarios u ON u.id_usuario = r.id_usuario AND u.cuenta_activa
                WHERE p.id_pedido = :p AND p.estado_actual = 'pendiente'
                  AND ( p.id_negocio_solicitado = r.id_negocio
                     OR ( p.tipo_solicitud = 'abierta' AND NOT EXISTS (
                            SELECT 1 FROM detalles_pedido dp
                            WHERE dp.id_pedido = p.id_pedido AND NOT EXISTS (
                                SELECT 1 FROM productos_negocio pn
                                WHERE pn.id_negocio = r.id_negocio AND pn.id_marca = dp.id_marca
                                  AND pn.activo AND pn.precio <= p.precio_maximo_garrafon)) ) )""",
                new MapSqlParameterSource("p", idPedido), Integer.class);
        Map<String, String> datos = Map.of("tipo", "pedido_nuevo", "idPedido", String.valueOf(idPedido));
        for (Integer rep : repartidores) {
            pushService.notificar(rep, "Nuevo pedido disponible",
                    "Hay un pedido cerca que puedes tomar.", datos);
        }
    }
}
