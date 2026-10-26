package com.h2togo.backend.bd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Entrega cada conexión del pool con el rol de PostgreSQL del actor ({@link ActorBd}) y su id en
 * {@code h2togo.id_usuario}, que leen las políticas de seguridad por filas. Se fija siempre al
 * tomar la conexión, así que no importa con qué rol la dejó quien la usó antes.
 */
public class RolPorActorDataSource extends DelegatingDataSource {

    private static final String FIJAR_ACTOR =
            "SELECT set_config('role', ?, false), set_config('h2togo.id_usuario', ?, false)";

    public RolPorActorDataSource(DataSource destino) {
        super(destino);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return conRolDelActor(super.getConnection());
    }

    @Override
    public Connection getConnection(String usuario, String password) throws SQLException {
        return conRolDelActor(super.getConnection(usuario, password));
    }

    private static Connection conRolDelActor(Connection conexion) throws SQLException {
        ActorBd actor = ActorBd.actual();
        try (PreparedStatement ps = conexion.prepareStatement(FIJAR_ACTOR)) {
            ps.setString(1, actor.rol());
            ps.setString(2, actor.idUsuario() == null ? "" : actor.idUsuario().toString());
            ps.execute();
        } catch (SQLException e) {
            conexion.close();
            throw e;
        }
        return conexion;
    }
}
