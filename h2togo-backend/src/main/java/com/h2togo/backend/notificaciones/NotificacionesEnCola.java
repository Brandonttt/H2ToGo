package com.h2togo.backend.notificaciones;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Productor: publica la notificación en el exchange de RabbitMQ y regresa de inmediato; la petición
 * del usuario no espera a FCM. Lo entrega {@link NotificacionesListener}, con reintentos y DLQ.
 */
@Service
@ConditionalOnProperty(name = "h2togo.mensajeria.habilitada", havingValue = "true")
public class NotificacionesEnCola implements PushService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesEnCola.class);

    private final RabbitTemplate rabbit;

    public NotificacionesEnCola(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @Override
    public void notificar(int idUsuario, String titulo, String mensaje, Map<String, String> datos) {
        Notificacion n = Notificacion.nueva(idUsuario, titulo, mensaje, datos);
        DespuesDelCommit.ejecutar(() -> {
            try {
                rabbit.convertAndSend(MensajeriaConfig.EXCHANGE, MensajeriaConfig.RK_NOTIFICACION, n);
            } catch (AmqpException e) {
                // El cambio de negocio ya se confirmó: no se revierte por no poder avisar.
                log.error("RabbitMQ no disponible; se perdió la notificación {} al usuario {}: {}",
                        n.id(), idUsuario, e.getMessage());
            }
        });
    }
}
