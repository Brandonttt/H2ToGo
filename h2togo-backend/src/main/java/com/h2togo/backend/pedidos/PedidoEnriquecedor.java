package com.h2togo.backend.pedidos;

import com.h2togo.backend.pedidos.dto.PedidoResponse;
import com.h2togo.backend.pedidos.dto.PedidoResponse.InfoRepartidor;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Completa un {@link PedidoResponse} con los datos que no viven en el agregado Pedido: cliente y
 * domicilio (para el repartidor) y repartidor, negocio y vehículo (para el seguimiento del
 * cliente). Antes estaba copiado igual en tres servicios.
 */
@Component
public class PedidoEnriquecedor {

    private final NamedParameterJdbcTemplate jdbc;

    public PedidoEnriquecedor(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PedidoResponse enriquecer(PedidoResponse base, int idPedido) {
        try {
            // Vehículo: el usado en la entrega (snapshot) o, mientras va en ruta, el de su jornada.
            // El teléfono del repartidor solo se comparte mientras el pedido está en curso (RN-016).
            Map<String, Object> x = jdbc.queryForMap("""
                    SELECT CONCAT(u.nombre, ' ', u.apellidos) AS nombre_cliente,
                           u.telefono AS telefono_cliente,
                           CONCAT_WS(', ', NULLIF(CONCAT_WS(' ', d.calle, d.numero_exterior), ''), NULLIF(d.colonia, '')) AS direccion,
                           ST_Y(d.ubicacion::geometry) AS lat,
                           ST_X(d.ubicacion::geometry) AS lon,
                           CONCAT(ur.nombre, ' ', ur.apellidos) AS nombre_repartidor,
                           CASE WHEN p.estado_actual IN ('asignado', 'en_camino') THEN ur.telefono END AS telefono_repartidor,
                           n.nombre_comercial AS negocio,
                           NULLIF(CONCAT_WS(' ', INITCAP(REPLACE(v.tipo_vehiculo::text, '_', ' ')), v.marca, v.modelo, v.color), '') AS vehiculo,
                           v.placas
                    FROM pedidos p
                    JOIN usuarios u ON u.id_usuario = p.id_cliente
                    LEFT JOIN direcciones_clientes d ON d.id_direccion = p.id_direccion_entrega
                    LEFT JOIN usuarios ur ON ur.id_usuario = p.id_repartidor
                    LEFT JOIN repartidores r ON r.id_usuario = p.id_repartidor
                    LEFT JOIN negocios n ON n.id_negocio = COALESCE(p.id_negocio_solicitado, r.id_negocio)
                    LEFT JOIN vehiculos_negocio v ON v.id_vehiculo = COALESCE(p.id_vehiculo_utilizado, r.id_vehiculo_actual)
                    WHERE p.id_pedido = :id""",
                    new MapSqlParameterSource("id", idPedido));
            Number lat = (Number) x.get("lat");
            Number lon = (Number) x.get("lon");
            InfoRepartidor repartidor = base.idRepartidor() == null ? null : new InfoRepartidor(
                    (String) x.get("nombre_repartidor"), (String) x.get("telefono_repartidor"),
                    (String) x.get("negocio"), (String) x.get("vehiculo"), (String) x.get("placas"));
            return new PedidoResponse(
                    base.id(), base.estado(), base.tipoSolicitud(), base.idNegocioSolicitado(),
                    base.idRepartidor(), base.idDireccionEntrega(), base.precioMaximoGarrafon(),
                    base.totalPagar(), base.garrafonesTotales(), base.indicaciones(),
                    base.esProgramado(), base.fechaProgramada(), base.fechaCreacion(),
                    base.detalles(), base.historial(),
                    (String) x.get("nombre_cliente"),
                    (String) x.get("telefono_cliente"),
                    (String) x.get("direccion"),
                    lat == null ? null : lat.doubleValue(),
                    lon == null ? null : lon.doubleValue(),
                    (String) x.get("negocio"),
                    repartidor);
        } catch (Exception e) {
            return base;
        }
    }
}
