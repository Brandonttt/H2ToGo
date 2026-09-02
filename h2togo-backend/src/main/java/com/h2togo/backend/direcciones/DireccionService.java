package com.h2togo.backend.direcciones;

import com.h2togo.backend.common.NotFoundException;
import com.h2togo.backend.direcciones.dto.DireccionRequest;
import com.h2togo.backend.direcciones.dto.DireccionResponse;
import java.util.List;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD de direcciones del cliente (soporte CU-004). Usa SQL nativo para las columnas
 * espaciales (§1): {@code ST_MakePoint} al escribir y {@code ST_X/ST_Y} al leer. El
 * trigger de BD calcula {@code en_zona_cobertura} (RN-001). El actor se resuelve del token;
 * toda operación filtra por {@code id_cliente} (propiedad, RN-016/RN-021).
 */
@Service
public class DireccionService {

    private static final String COLUMNAS = """
            id_direccion, alias, calle, numero_exterior, numero_interior, colonia,
            codigo_postal, referencias,
            ST_Y(ubicacion::geometry) AS lat, ST_X(ubicacion::geometry) AS lon,
            en_zona_cobertura, activo
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public DireccionService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<DireccionResponse> listar(int idCliente) {
        return jdbc.query(
                "SELECT " + COLUMNAS + " FROM direcciones_clientes "
                        + "WHERE id_cliente = :idCliente AND activo ORDER BY id_direccion",
                new MapSqlParameterSource("idCliente", idCliente),
                DireccionService::mapRow);
    }

    @Transactional
    public DireccionResponse crear(int idCliente, DireccionRequest req) {
        MapSqlParameterSource params = paramsComunes(idCliente, req);
        // El trigger BEFORE INSERT calcula en_zona_cobertura → lo devolvemos con RETURNING.
        Boolean enZona = jdbc.queryForObject("""
                INSERT INTO direcciones_clientes
                    (id_cliente, alias, calle, numero_exterior, numero_interior, colonia,
                     codigo_postal, referencias, ubicacion, activo)
                VALUES
                    (:idCliente, :alias, :calle, :numExt, :numInt, :colonia, :cp, :referencias,
                     ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, TRUE)
                RETURNING id_direccion, en_zona_cobertura
                """, params, (rs, n) -> {
            params.addValue("idGenerado", rs.getInt("id_direccion"));
            return rs.getBoolean("en_zona_cobertura");
        });
        int id = (int) params.getValue("idGenerado");
        return toResponse(id, req, enZona, true);
    }

    @Transactional
    public DireccionResponse actualizar(int idCliente, int idDireccion, DireccionRequest req) {
        MapSqlParameterSource params = paramsComunes(idCliente, req).addValue("id", idDireccion);
        try {
            Boolean enZona = jdbc.queryForObject("""
                    UPDATE direcciones_clientes SET
                        alias = :alias, calle = :calle, numero_exterior = :numExt,
                        numero_interior = :numInt, colonia = :colonia, codigo_postal = :cp,
                        referencias = :referencias,
                        ubicacion = ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography
                    WHERE id_direccion = :id AND id_cliente = :idCliente AND activo
                    RETURNING en_zona_cobertura
                    """, params, (rs, n) -> rs.getBoolean("en_zona_cobertura"));
            return toResponse(idDireccion, req, enZona, true);
        } catch (EmptyResultDataAccessException e) {
            throw noEncontrada(idDireccion);
        }
    }

    /** Baja lógica si la dirección ya fue usada en pedidos (conserva histórico, RN-013); si no, se borra. */
    @Transactional
    public void eliminar(int idCliente, int idDireccion) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", idDireccion).addValue("idCliente", idCliente);

        Integer existe = jdbc.queryForObject(
                "SELECT COUNT(*) FROM direcciones_clientes WHERE id_direccion = :id AND id_cliente = :idCliente",
                params, Integer.class);
        if (existe == null || existe == 0) {
            throw noEncontrada(idDireccion);
        }

        Boolean usada = jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM pedidos WHERE id_direccion_entrega = :id)",
                params, Boolean.class);
        if (Boolean.TRUE.equals(usada)) {
            jdbc.update("UPDATE direcciones_clientes SET activo = FALSE "
                    + "WHERE id_direccion = :id AND id_cliente = :idCliente", params);
        } else {
            jdbc.update("DELETE FROM direcciones_clientes "
                    + "WHERE id_direccion = :id AND id_cliente = :idCliente", params);
        }
    }

    private MapSqlParameterSource paramsComunes(int idCliente, DireccionRequest req) {
        return new MapSqlParameterSource()
                .addValue("idCliente", idCliente)
                .addValue("alias", req.alias())
                .addValue("calle", req.calle())
                .addValue("numExt", req.numeroExterior())
                .addValue("numInt", req.numeroInterior())
                .addValue("colonia", req.colonia())
                .addValue("cp", req.codigoPostal())
                .addValue("referencias", req.referencias())
                .addValue("lat", req.lat())
                .addValue("lon", req.lon());
    }

    private static DireccionResponse toResponse(int id, DireccionRequest req, boolean enZona, boolean activo) {
        return new DireccionResponse(id, req.alias(), req.calle(), req.numeroExterior(),
                req.numeroInterior(), req.colonia(), req.codigoPostal(), req.referencias(),
                req.lat(), req.lon(), enZona, activo);
    }

    private static DireccionResponse mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new DireccionResponse(
                rs.getInt("id_direccion"), rs.getString("alias"), rs.getString("calle"),
                rs.getString("numero_exterior"), rs.getString("numero_interior"),
                rs.getString("colonia"), rs.getString("codigo_postal"), rs.getString("referencias"),
                rs.getDouble("lat"), rs.getDouble("lon"),
                rs.getBoolean("en_zona_cobertura"), rs.getBoolean("activo"));
    }

    private static NotFoundException noEncontrada(int idDireccion) {
        return new NotFoundException("DIRECCION_NO_ENCONTRADA",
                "No existe la dirección " + idDireccion + " para este cliente.");
    }
}
