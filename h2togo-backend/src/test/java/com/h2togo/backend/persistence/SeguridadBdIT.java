package com.h2togo.backend.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Seguridad en la propia BD (esquema v7, sección 16): se conecta con el rol de cada actor, igual
 * que el backend, y manda SQL directo. Así se comprueba que aunque el código tuviera un error, la
 * BD no deja a un cliente leer inventario, ni ver pedidos ajenos, ni hacerse administrador. Cada
 * prueba corre en una transacción que se deshace.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SeguridadBdIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"))
            .withCopyFileToContainer(MountableFile.forHostPath("../H2ToGo_v7_postgresql.sql"),
                    "/docker-entrypoint-initdb.d/01_schema.sql")
            .withCopyFileToContainer(MountableFile.forHostPath("sql/zona_benito_juarez.sql"),
                    "/docker-entrypoint-initdb.d/02_zona.sql");

    private static final String SIN_PERMISO = "42501"; // insufficient_privilege (también lo usa la RLS)
    private static final String BITACORA = "23001";    // restrict_violation, del trigger de bitácoras

    @Autowired private DataSource dataSource;
    @Autowired private NamedParameterJdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transacciones;

    private int negocioA, negocioB, duenoA, duenoB, pendienteA, cliente1, cliente2, pedido, loteA, vehiculoA;

    /** Datos de dos purificadoras y dos clientes, sembrados como el sistema (fuera de una petición). */
    @BeforeAll
    void sembrar() {
        new TransactionTemplate(transacciones).executeWithoutResult(t -> {
            negocioA = id("INSERT INTO negocios (nombre_comercial) VALUES ('Purif A') RETURNING id_negocio");
            negocioB = id("INSERT INTO negocios (nombre_comercial) VALUES ('Purif B') RETURNING id_negocio");
            duenoA = repartidor("seg-da@test.mx", "5551110001", negocioA, true, true);
            duenoB = repartidor("seg-db@test.mx", "5551110002", negocioB, true, true);
            pendienteA = repartidor("seg-pa@test.mx", "5551110003", negocioA, false, false);
            cliente1 = cliente("seg-c1@test.mx", "5551110004");
            cliente2 = cliente("seg-c2@test.mx", "5551110005");
            int marca = id("INSERT INTO marcas (nombre) VALUES ('Marca Seguridad') RETURNING id_marca");
            // Cada lote debe ser de una marca que su negocio vende.
            for (int negocio : new int[] {negocioA, negocioB}) {
                jdbc.update("""
                        INSERT INTO productos_negocio (id_negocio, id_marca, precio, precio_envase, capacidad_maxima)
                        VALUES (:n, :m, 25, 30, 500)""", new MapSqlParameterSource().addValue("n", negocio).addValue("m", marca));
            }
            vehiculoA = id("""
                    INSERT INTO vehiculos_negocio (id_negocio, tipo_vehiculo, marca, color, capacidad_garrafones)
                    VALUES (%d, 'motocicleta', 'Italika', 'rojo', 30) RETURNING id_vehiculo""".formatted(negocioA));
            loteA = lote(negocioA, marca);
            lote(negocioB, marca);
            jdbc.update("""
                    INSERT INTO inventario_vehiculo (id_vehiculo, id_lote, id_negocio, cantidad_actual, cantidad_apartada)
                    VALUES (:v, :l, :n, 10, 5)""",
                    new MapSqlParameterSource().addValue("v", vehiculoA).addValue("l", loteA).addValue("n", negocioA));
            int direccion = direccion(cliente1);
            direccion(cliente2);
            pedido = id("""
                    INSERT INTO pedidos (id_cliente, id_negocio_solicitado, id_repartidor, id_direccion_entrega,
                        estado_actual, total_pagar, garrafones_totales)
                    VALUES (%d, %d, %d, %d, 'asignado', 125, 5) RETURNING id_pedido"""
                    .formatted(cliente1, negocioA, duenoA, direccion));
            jdbc.update("""
                    INSERT INTO detalles_pedido (id_pedido, id_marca, cantidad_solicitada, tiene_envase, precio_unitario)
                    VALUES (:p, :m, 5, TRUE, 25)""", new MapSqlParameterSource().addValue("p", pedido).addValue("m", marca));
            jdbc.update("INSERT INTO historial_estados_pedido (id_pedido, estado) VALUES (:p, 'asignado')",
                    new MapSqlParameterSource("p", pedido));
            // Dos apartados del MISMO lote en el mismo vehículo (dos líneas de la misma marca).
            for (int cantidad : new int[] {2, 3}) {
                jdbc.update("""
                        INSERT INTO apartados_pedido (id_pedido, id_lote, id_vehiculo, cantidad)
                        VALUES (:p, :l, :v, :c)""", new MapSqlParameterSource().addValue("p", pedido)
                        .addValue("l", loteA).addValue("v", vehiculoA).addValue("c", cantidad));
            }
        });
    }

    @Test
    void elClienteNoTienePermisoSobreElInventario() throws SQLException {
        for (String tabla : new String[] {"lotes_inventario", "inventario_vehiculo", "movimientos_inventario",
                "apartados_pedido", "solicitudes_cambio_perfil"}) {
            comoActor("h2togo_cliente", cliente1, st ->
                    assertThatThrownBy(() -> st.executeQuery("SELECT * FROM " + tabla))
                            .as(tabla).isInstanceOf(SQLException.class)
                            .extracting(e -> ((SQLException) e).getSQLState()).isEqualTo(SIN_PERMISO));
        }
        // Del inventario solo ve el total disponible por marca (vista).
        comoActor("h2togo_cliente", cliente1, st ->
                assertThat(numero(st, "SELECT disponibles FROM v_existencias_negocio WHERE id_negocio = " + negocioA))
                        .isEqualTo(20));
    }

    @Test
    void cadaClienteSoloVeSusPedidosYDirecciones() throws SQLException {
        comoActor("h2togo_cliente", cliente1, st -> {
            assertThat(numero(st, "SELECT COUNT(*) FROM pedidos")).isEqualTo(1);
            assertThat(numero(st, "SELECT COUNT(*) FROM direcciones_clientes")).isEqualTo(1);
            // De los usuarios solo se ve a sí mismo y al repartidor de su pedido.
            assertThat(numero(st, "SELECT COUNT(*) FROM usuarios")).isEqualTo(2);
        });
        comoActor("h2togo_cliente", cliente2, st -> {
            assertThat(numero(st, "SELECT COUNT(*) FROM pedidos")).isZero();
            assertThat(numero(st, "SELECT COUNT(*) FROM detalles_pedido")).isZero();
            assertThat(numero(st, "SELECT COUNT(*) FROM direcciones_clientes")).isEqualTo(1);
            assertThat(numero(st, "SELECT COUNT(*) FROM usuarios")).isEqualTo(1);
        });
    }

    @Test
    void cadaRepartidorSoloVeElInventarioDeSuPurificadora() throws SQLException {
        comoActor("h2togo_repartidor", duenoB, st -> {
            assertThat(numero(st, "SELECT COUNT(*) FROM lotes_inventario")).isEqualTo(1);
            assertThat(numero(st, "SELECT COUNT(*) FROM lotes_inventario WHERE id_negocio = " + negocioA)).isZero();
            assertThat(numero(st, "SELECT COUNT(*) FROM inventario_vehiculo")).isZero();
            assertThat(numero(st, "SELECT COUNT(*) FROM vehiculos_negocio")).isZero();
            // Tampoco ve el domicilio de un pedido que atiende otra purificadora.
            assertThat(numero(st, "SELECT COUNT(*) FROM direcciones_clientes")).isZero();
        });
        comoActor("h2togo_repartidor", duenoA, st ->
                assertThat(numero(st, "SELECT COUNT(*) FROM inventario_vehiculo")).isEqualTo(1));
    }

    @Test
    void nadieSeRegistraComoAdministrador() throws SQLException {
        comoActor("h2togo_publico", null, st -> assertThatThrownBy(() -> st.executeUpdate("""
                INSERT INTO usuarios (nombre, apellidos, correo, password_hash, telefono, rol)
                VALUES ('X', 'Y', 'intruso@test.mx', 'h', '5551119999', 'admin')"""))
                .extracting(e -> ((SQLException) e).getSQLState()).isEqualTo(SIN_PERMISO));
    }

    @Test
    void unClienteNoCambiaSuRolNiSeReactivaNiMarcaEntregado() throws SQLException {
        for (String cambio : new String[] {"rol = 'admin'", "cuenta_activa = TRUE", "telefono_verificado = TRUE"}) {
            comoActor("h2togo_cliente", cliente1, st ->
                    assertThatThrownBy(() -> st.executeUpdate("UPDATE usuarios SET " + cambio + " WHERE id_usuario = " + cliente1))
                            .as(cambio).extracting(e -> ((SQLException) e).getSQLState()).isEqualTo(SIN_PERMISO));
        }
        // Sobre su pedido solo puede cancelarlo, y solo mientras está pendiente.
        comoActor("h2togo_cliente", cliente1, st ->
                assertThat(st.executeUpdate("UPDATE pedidos SET estado_actual = 'entregado' WHERE id_pedido = " + pedido))
                        .isZero());
    }

    @Test
    void unMiembroPendienteNoSeApruebaASiMismo() throws SQLException {
        comoActor("h2togo_repartidor", pendienteA, st ->
                assertThatThrownBy(() -> st.executeUpdate("UPDATE repartidores SET aprobado = TRUE WHERE id_usuario = " + pendienteA))
                        .extracting(e -> ((SQLException) e).getSQLState()).isEqualTo(SIN_PERMISO));
        comoActor("h2togo_repartidor", pendienteA, st ->
                assertThatThrownBy(() -> st.execute("SELECT fn_aprobar_repartidor(" + pendienteA + ")"))
                        .extracting(e -> ((SQLException) e).getSQLState()).isEqualTo(SIN_PERMISO));
        // El dueño sí puede.
        comoActor("h2togo_repartidor", duenoA, st -> {
            st.execute("SELECT fn_aprobar_repartidor(" + pendienteA + ")");
            assertThat(numero(st, "SELECT COUNT(*) FROM repartidores WHERE aprobado AND id_usuario = " + pendienteA))
                    .isEqualTo(1);
        });
    }

    @Test
    void liberarApartadosValidaQuienLlamaYSumaLosDelMismoLote() throws SQLException {
        comoActor("h2togo_cliente", cliente2, st ->
                assertThatThrownBy(() -> st.execute("SELECT fn_liberar_apartados(" + pedido + ")"))
                        .extracting(e -> ((SQLException) e).getSQLState()).isEqualTo(SIN_PERMISO));
        comoActor("h2togo_cliente", cliente1, st -> {
            st.execute("SELECT fn_liberar_apartados(" + pedido + ")");
            st.execute("SELECT set_config('role', 'h2togo_sistema', true)");
            // 2 + 3 apartados del mismo lote: se liberan los 5 (antes solo se descontaba uno de los dos).
            assertThat(numero(st, "SELECT cantidad_apartada FROM inventario_vehiculo WHERE id_lote = " + loteA)).isZero();
            assertThat(numero(st, "SELECT COUNT(*) FROM apartados_pedido WHERE id_pedido = " + pedido)).isZero();
        });
    }

    @Test
    void lasBitacorasNoSeModificanNiSiquieraComoSistema() throws SQLException {
        comoActor("h2togo_sistema", null, st ->
                assertThatThrownBy(() -> st.executeUpdate("DELETE FROM historial_estados_pedido WHERE id_pedido = " + pedido))
                        .extracting(e -> ((SQLException) e).getSQLState()).isEqualTo(SIN_PERMISO));
        // El dueño del esquema sí tiene permiso de UPDATE, pero el trigger lo impide (RN-013).
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            c.setAutoCommit(false);
            st.execute("RESET ROLE");
            assertThatThrownBy(() -> st.executeUpdate("UPDATE historial_estados_pedido SET estado = 'cancelado'"))
                    .extracting(e -> ((SQLException) e).getSQLState()).isEqualTo(BITACORA);
            c.rollback();
        }
    }

    // ---------------------------------------------------------------- Apoyo

    private interface ConSentencia {
        void ejecutar(Statement st) throws SQLException;
    }

    /** Como el backend: adopta el rol del actor y su id; al final deshace todo. */
    private void comoActor(String rol, Integer idUsuario, ConSentencia cuerpo) throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT set_config('role', ?, true), set_config('h2togo.id_usuario', ?, true)")) {
                ps.setString(1, rol);
                ps.setString(2, idUsuario == null ? "" : idUsuario.toString());
                ps.execute();
            }
            // Un error deja la transacción abortada: la consulta que debe fallar va al final del bloque.
            try (Statement st = c.createStatement()) {
                cuerpo.ejecutar(st);
            } finally {
                c.rollback();
            }
        }
    }

    private static long numero(Statement st, String sql) throws SQLException {
        try (ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private int id(String sql) {
        return jdbc.queryForObject(sql, new MapSqlParameterSource(), Integer.class);
    }

    private int usuario(String correo, String telefono, String rol) {
        return jdbc.queryForObject("""
                INSERT INTO usuarios (nombre, apellidos, correo, password_hash, telefono, rol, telefono_verificado)
                VALUES ('Ana', 'Prueba', :c, 'hash', :t, CAST(:r AS rol_usuario), TRUE) RETURNING id_usuario""",
                new MapSqlParameterSource().addValue("c", correo).addValue("t", telefono).addValue("r", rol),
                Integer.class);
    }

    private int repartidor(String correo, String telefono, int negocio, boolean dueno, boolean aprobado) {
        int id = usuario(correo, telefono, "repartidor");
        jdbc.update("INSERT INTO repartidores (id_usuario, id_negocio, es_dueno, aprobado) VALUES (:u, :n, :d, :a)",
                new MapSqlParameterSource().addValue("u", id).addValue("n", negocio)
                        .addValue("d", dueno).addValue("a", aprobado));
        return id;
    }

    private int cliente(String correo, String telefono) {
        int id = usuario(correo, telefono, "cliente");
        jdbc.update("INSERT INTO clientes (id_usuario) VALUES (:u)", new MapSqlParameterSource("u", id));
        return id;
    }

    private int direccion(int idCliente) {
        return jdbc.queryForObject("""
                INSERT INTO direcciones_clientes (id_cliente, alias, calle, numero_exterior, colonia, codigo_postal, ubicacion)
                VALUES (:c, 'Casa', 'Calle', '1', 'Del Valle', '03100',
                        ST_SetSRID(ST_MakePoint(-99.17, 19.37), 4326)::geography) RETURNING id_direccion""",
                new MapSqlParameterSource("c", idCliente), Integer.class);
    }

    private int lote(int negocio, int marca) {
        return jdbc.queryForObject("""
                INSERT INTO lotes_inventario (id_negocio, id_marca, fecha_caducidad, cantidad_inicial, cantidad_actual)
                VALUES (:n, :m, CURRENT_DATE + 60, 20, 20) RETURNING id_lote""",
                new MapSqlParameterSource().addValue("n", negocio).addValue("m", marca), Integer.class);
    }
}
