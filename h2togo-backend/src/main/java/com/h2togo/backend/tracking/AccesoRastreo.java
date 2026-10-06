package com.h2togo.backend.tracking;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Reglas de acceso al rastreo de un pedido (RN-016). Separado de {@link RastreoService} porque lo
 * usa el interceptor STOMP, que se crea antes que la mensajería y no puede depender de ella.
 */
@Component
public class AccesoRastreo {

    private final NamedParameterJdbcTemplate jdbc;

    public AccesoRastreo(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Solo el repartidor asignado, y solo mientras el pedido está asignado o en camino. */
    public boolean puedePublicar(int idUsuario, int idPedido) {
        return existe("id_repartidor = :u AND estado_actual IN " + RastreoService.ACTIVOS, idUsuario, idPedido);
    }

    /** El cliente dueño del pedido o su repartidor asignado. */
    public boolean puedeVer(int idUsuario, int idPedido) {
        return existe("(id_cliente = :u OR id_repartidor = :u)", idUsuario, idPedido);
    }

    private boolean existe(String condicion, int idUsuario, int idPedido) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pedidos WHERE id_pedido = :p AND " + condicion,
                new MapSqlParameterSource().addValue("p", idPedido).addValue("u", idUsuario), Integer.class);
        return n != null && n > 0;
    }
}
