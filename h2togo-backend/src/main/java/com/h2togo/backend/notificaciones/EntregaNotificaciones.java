package com.h2togo.backend.notificaciones;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Entrega final de una notificación por los dos canales:
 * <ul>
 *   <li>WebSocket ({@code /user/queue/notificaciones}): inmediato mientras la app está abierta.</li>
 *   <li>FCM: llega aunque la app esté cerrada; solo si hay credenciales y el usuario registró
 *       su dispositivo.</li>
 * </ul>
 * Un fallo transitorio de FCM se propaga para que RabbitMQ reintente; la app descarta el
 * duplicado del WebSocket por {@link Notificacion#id()}.
 */
@Component
public class EntregaNotificaciones {

    private static final Logger log = LoggerFactory.getLogger(EntregaNotificaciones.class);
    public static final String DESTINO_USUARIO = "/queue/notificaciones";

    private final SimpMessagingTemplate messaging;
    private final FcmClient fcm;

    public EntregaNotificaciones(SimpMessagingTemplate messaging, FcmClient fcm) {
        this.messaging = messaging;
        this.fcm = fcm;
    }

    public void entregar(Notificacion n) {
        log.info("[PUSH] usuario {} · {}: {}", n.idUsuario(), n.titulo(), n.mensaje());
        // El principal STOMP se llama como el id del usuario (StompPrincipal#getName).
        messaging.convertAndSendToUser(String.valueOf(n.idUsuario()), DESTINO_USUARIO, n);
        fcm.enviar(n);
    }
}
