package com.h2togo.backend.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Notificaciones por RabbitMQ de punta a punta: productor → exchange → cola → listener →
 * WebSocket del usuario. También verifica que no se publique si la transacción se revierte y
 * que un fallo persistente de entrega acabe en la DLQ tras los reintentos.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "h2togo.mensajeria.habilitada=true",
        "spring.rabbitmq.listener.simple.retry.initial-interval=100ms"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class NotificacionesRabbitIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"))
            .withCopyFileToContainer(MountableFile.forHostPath("../H2ToGo_v6_postgresql.sql"),
                    "/docker-entrypoint-initdb.d/01_schema.sql");

    @Container
    @ServiceConnection
    static final RabbitMQContainer RABBIT = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management"));

    @Autowired private PushService pushService;
    @Autowired private RabbitTemplate rabbit;
    @Autowired private NamedParameterJdbcTemplate jdbc;
    @Autowired private TransactionTemplate tx;
    @Autowired private MockMvc mvc;
    @MockitoBean private FcmClient fcm;
    @LocalServerPort private int port;

    @Test
    void laNotificacionViajaPorRabbitHastaElWebSocketDelUsuario() throws Exception {
        assertThat(pushService).isInstanceOf(NotificacionesEnCola.class);
        Object[] u = usuarioConSesion();
        StompSession sesion = conectar((String) u[1]);
        CompletableFuture<Map<String, Object>> recibida = suscribir(sesion);
        Thread.sleep(300);

        pushService.notificar((int) u[0], "Pedido en camino", "Tu repartidor va en camino.",
                Map.of("tipo", "pedido_en_camino", "idPedido", "7"));

        Map<String, Object> n = recibida.get(10, TimeUnit.SECONDS);
        assertThat(n.get("titulo")).isEqualTo("Pedido en camino");
        assertThat(n.get("id")).isNotNull();
        assertThat(((Map<?, ?>) n.get("datos")).get("idPedido")).isEqualTo("7");
        sesion.disconnect();
    }

    @Test
    void siLaTransaccionSeRevierteNoSeNotifica() throws Exception {
        Object[] u = usuarioConSesion();
        StompSession sesion = conectar((String) u[1]);
        CompletableFuture<Map<String, Object>> recibida = suscribir(sesion);
        Thread.sleep(300);

        tx.executeWithoutResult(status -> {
            pushService.notificar((int) u[0], "No debe llegar", "Rollback");
            status.setRollbackOnly();
        });

        Thread.sleep(1500);
        assertThat(recibida).isNotDone();
        sesion.disconnect();
    }

    @Test
    void unFalloPersistenteSeReintentaYTerminaEnLaDlq() throws Exception {
        Object[] u = usuarioConSesion();
        int idUsuario = (int) u[0];
        doThrow(new IllegalStateException("FCM caído"))
                .when(fcm).enviar(argThat(n -> n != null && n.idUsuario() == idUsuario));

        pushService.notificar(idUsuario, "Lotes por caducar", "Revisa tu inventario.");

        Message muerto = null;
        for (int i = 0; i < 50 && muerto == null; i++) {
            muerto = rabbit.receive(MensajeriaConfig.DLQ, 200);
        }
        assertThat(muerto).as("el mensaje debe llegar a la DLQ tras agotar reintentos").isNotNull();
        assertThat(new String(muerto.getBody())).contains("Lotes por caducar");
        verify(fcm, atLeast(4)).enviar(argThat(n -> n != null && n.idUsuario() == idUsuario));
    }

    @Test
    void registroDelTokenDelDispositivo() throws Exception {
        Object[] u = usuarioConSesion();
        mvc.perform(put("/api/v1/usuarios/me/dispositivo").header("Authorization", "Bearer " + u[1])
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tokenFcm\":\"tok-123\"}"))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT token_fcm FROM usuarios WHERE id_usuario = :id",
                new MapSqlParameterSource("id", u[0]), String.class)).isEqualTo("tok-123");
    }

    /** Cliente con sesión vigente. Devuelve [idUsuario, token]. */
    private Object[] usuarioConSesion() {
        String token = "ntf-" + UUID.randomUUID();
        Integer id = jdbc.queryForObject("""
                INSERT INTO usuarios (nombre, apellidos, correo, password_hash, telefono, rol,
                    telefono_verificado, cuenta_activa, token_sesion, sesion_fecha_creacion, sesion_fecha_expiracion)
                VALUES ('N','T',:correo,'x',:tel,'cliente'::rol_usuario, TRUE, TRUE, :token, now(), now() + interval '30 days')
                RETURNING id_usuario""",
                new MapSqlParameterSource().addValue("correo", token + "@test.mx")
                        .addValue("tel", "77" + (System.nanoTime() % 100000000L)).addValue("token", token),
                Integer.class);
        jdbc.update("INSERT INTO clientes (id_usuario) VALUES (:id)", new MapSqlParameterSource("id", id));
        return new Object[]{id, token};
    }

    private StompSession conectar(String token) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter());
        StompHeaders headers = new StompHeaders();
        headers.add("token", token);
        return client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), headers,
                new StompSessionHandlerAdapter() { }).get(5, TimeUnit.SECONDS);
    }

    private CompletableFuture<Map<String, Object>> suscribir(StompSession sesion) {
        CompletableFuture<Map<String, Object>> recibida = new CompletableFuture<>();
        sesion.subscribe("/user/queue/notificaciones", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                recibida.complete((Map<String, Object>) payload);
            }
        });
        return recibida;
    }
}
