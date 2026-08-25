package com.h2togo.backend.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.h2togo.backend.common.enums.RolUsuario;
import com.h2togo.backend.negocios.Negocio;
import com.h2togo.backend.negocios.NegocioRepository;
import com.h2togo.backend.usuarios.Cliente;
import com.h2togo.backend.usuarios.ClienteRepository;
import com.h2togo.backend.usuarios.Repartidor;
import com.h2togo.backend.usuarios.RepartidorRepository;
import com.h2togo.backend.usuarios.Usuario;
import com.h2togo.backend.usuarios.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Verifica el modelo de datos de F1 contra la BD v6 REAL (PostgreSQL 16 + PostGIS 3):
 * <ul>
 *   <li>arranca el contexto con {@code ddl-auto: validate} → las 19 tablas y los enums cuadran
 *       con las entidades (si algo no coincide, el contexto no arranca);</li>
 *   <li>guarda y lee un grafo mínimo usuario+cliente;</li>
 *   <li>da de alta dueño+negocio en UNA sola transacción, ejercitando las FKs
 *       {@code DEFERRABLE INITIALLY DEFERRED} del ciclo {@code negocios.id_dueno ↔ repartidores.id_negocio}
 *       (la verificación ocurre en el COMMIT).</li>
 * </ul>
 * Se salta automáticamente si no hay Docker, de modo que {@code mvn verify} queda verde
 * en equipos sin Docker y valida de verdad donde sí lo hay.
 */
// webEnvironment MOCK (default): provee entorno servlet para que Spring Security cree el
// bean HttpSecurity que usa SecurityConfig. No abre puerto Tomcat. NONE no sirve aquí porque
// sin contexto web no existe HttpSecurity y el contexto no arranca.
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ModeloDatosIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"))
            .withCopyFileToContainer(
                    MountableFile.forHostPath("../H2ToGo_v6_postgresql.sql"),
                    "/docker-entrypoint-initdb.d/01_schema.sql");

    @Autowired
    private UsuarioRepository usuarioRepo;
    @Autowired
    private ClienteRepository clienteRepo;
    @Autowired
    private RepartidorRepository repartidorRepo;
    @Autowired
    private NegocioRepository negocioRepo;
    @Autowired
    private PlatformTransactionManager txManager;

    @Test
    void guardaYLeeUsuarioConCliente() {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        Integer[] idUsuario = new Integer[1];

        tx.executeWithoutResult(s -> {
            Usuario u = nuevoUsuario("Ana", "García", "ana.cliente@test.mx",
                    "5550000001", RolUsuario.cliente);
            usuarioRepo.save(u);

            Cliente c = new Cliente();
            c.setUsuario(u);
            clienteRepo.save(c);

            idUsuario[0] = u.getId();
        });

        Cliente leido = clienteRepo.findById(idUsuario[0]).orElseThrow();
        assertThat(leido.getUsuario().getCorreo()).isEqualTo("ana.cliente@test.mx");
        assertThat(leido.getUsuario().getRol()).isEqualTo(RolUsuario.cliente);
        assertThat(leido.getAusenciasConsecutivas()).isZero();
    }

    @Test
    void altaDuenoConNegocioEnUnaTransaccion() {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        int[] ids = new int[2]; // [0]=idUsuario/repartidor, [1]=idNegocio

        // Orden de inserción como en pruebas_v6.sql §1: usuario → negocio → repartidor.
        // Las FKs del ciclo son DEFERRABLE, así que negocio.id_dueno puede apuntar a un
        // repartidor que aún no existe; el COMMIT verifica ambas.
        tx.executeWithoutResult(s -> {
            Usuario u = nuevoUsuario("Beto", "López", "beto.dueno@test.mx",
                    "5550000002", RolUsuario.repartidor);
            usuarioRepo.save(u);

            Negocio n = new Negocio();
            n.setNombreComercial("Purificadora Test");
            n.setIdDueno(u.getId());
            negocioRepo.save(n);

            Repartidor r = new Repartidor();
            r.setUsuario(u);
            r.setIdNegocio(n.getId());
            repartidorRepo.save(r);

            ids[0] = u.getId();
            ids[1] = n.getId();
        });

        Repartidor r = repartidorRepo.findById(ids[0]).orElseThrow();
        Negocio n = negocioRepo.findById(ids[1]).orElseThrow();
        assertThat(r.getIdNegocio()).isEqualTo(n.getId());
        assertThat(n.getIdDueno()).isEqualTo(r.getId());
        assertThat(r.getUsuario().getRol()).isEqualTo(RolUsuario.repartidor);
        assertThat(r.isEstadoOperativo()).isFalse();
    }

    private static Usuario nuevoUsuario(String nombre, String apellidos, String correo,
                                        String telefono, RolUsuario rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setApellidos(apellidos);
        u.setCorreo(correo);
        u.setPasswordHash("hash-de-prueba");
        u.setTelefono(telefono);
        u.setRol(rol);
        return u;
    }
}
