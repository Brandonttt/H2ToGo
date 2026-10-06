package com.h2togo.backend.notificaciones;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Consumidor de la cola de notificaciones. Si la entrega lanza una excepción (p. ej. FCM caído),
 * Spring AMQP reintenta con espera exponencial (spring.rabbitmq.listener.simple.retry) y, agotados
 * los intentos, rechaza el mensaje: RabbitMQ lo manda a la DLQ para revisarlo después.
 */
@Component
@ConditionalOnProperty(name = "h2togo.mensajeria.habilitada", havingValue = "true")
public class NotificacionesListener {

    private final EntregaNotificaciones entrega;

    public NotificacionesListener(EntregaNotificaciones entrega) {
        this.entrega = entrega;
    }

    @RabbitListener(queues = MensajeriaConfig.COLA)
    public void recibir(Notificacion notificacion) {
        entrega.entregar(notificacion);
    }
}
