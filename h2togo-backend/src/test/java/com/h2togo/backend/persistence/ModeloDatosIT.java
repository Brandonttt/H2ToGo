package com.h2togo.backend.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
 * Verifica el modelo de datos contra la BD v7 REAL (PostgreSQL 16 + PostGIS 3):
 * <ul>
 *   <li>arranca el contexto con {@code ddl-auto: validate} → las 19 tablas y los enums cuadran
 *       con las entidades (si algo no coincide, el contexto no arranca);</li>
 *   <li>guarda y lee un grafo mínimo usuario+cliente;</li>
 *   <li>da de alta dueño+negocio en una transacción: sin el ciclo de v6, el dueño es el
 *       repartidor con {@code es_dueno}, y la BD exige al COMMIT exactamente un dueño por negocio.</li>
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
                    MountableFile.forHostPath("../H2ToGo_v7_postgresql.sql"),
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

        // Sin ciclo: usuario → negocio → repartidor (dueño). El trigger diferido revisa al COMMIT.
        tx.executeWithoutResult(s -> {
            Usuario u = nuevoUsuario("Beto", "López", "beto.dueno@test.mx",
                    "5550000002", RolUsuario.repartidor);
            usuarioRepo.save(u);

            Negocio n = new Negocio();
            n.setNombreComercial("Purificadora Test");
            negocioRepo.save(n);

            Repartidor r = new Repartidor();
            r.setUsuario(u);
            r.setIdNegocio(n.getId());
            r.setEsDueno(true);
            repartidorRepo.save(r);

            ids[0] = u.getId();
            ids[1] = n.getId();
        });

        Repartidor r = repartidorRepo.findById(ids[0]).orElseThrow();
        Negocio n = negocioRepo.findById(ids[1]).orElseThrow();
        assertThat(r.getIdNegocio()).isEqualTo(n.getId());
        assertThat(n.getIdDueno()).isEqualTo(r.getId()); // @Formula sobre repartidores.es_dueno
        assertThat(r.getUsuario().getRol()).isEqualTo(RolUsuario.repartidor);
        assertThat(r.isEstadoOperativo()).isFalse();
    }

    @Test
    void unNegocioSinDuenoNoSeConfirma() {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
            Negocio n = new Negocio();
            n.setNombreComercial("Purificadora Huérfana");
            negocioRepo.save(n);
        })).rootCause().hasMessageContaining("debe tener exactamente un dueño");
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
