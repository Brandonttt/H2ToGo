package com.h2togo.backend.notificaciones;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología de RabbitMQ. Spring la declara al conectar (idempotente):
 * <pre>
 *  productor ──notificacion.push──▶ [h2togo.eventos] (topic) ──▶ h2togo.notificaciones ──▶ listener
 *                                                                     │ rechazo tras reintentos
 *                                   [h2togo.eventos.dlx] (direct) ◀───┘ ──▶ h2togo.notificaciones.dlq
 * </pre>
 * El exchange es de tipo topic para que futuros eventos (p. ej. {@code pedido.*}) compartan la
 * infraestructura con colas propias.
 */
@Configuration
@ConditionalOnProperty(name = "h2togo.mensajeria.habilitada", havingValue = "true")
public class MensajeriaConfig {

    public static final String EXCHANGE = "h2togo.eventos";
    public static final String RK_NOTIFICACION = "notificacion.push";
    public static final String COLA = "h2togo.notificaciones";
    static final String DLX = "h2togo.eventos.dlx";
    static final String DLQ = "h2togo.notificaciones.dlq";
    static final String RK_FALLIDA = "notificacion.fallida";

    @Bean
    TopicExchange eventosExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange eventosDlx() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    Queue colaNotificaciones() {
        return QueueBuilder.durable(COLA)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(RK_FALLIDA)
                .build();
    }

    @Bean
    Queue colaNotificacionesFallidas() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    Binding bindingNotificaciones() {
        return BindingBuilder.bind(colaNotificaciones()).to(eventosExchange()).with("notificacion.#");
    }

    @Bean
    Binding bindingFallidas() {
        return BindingBuilder.bind(colaNotificacionesFallidas()).to(eventosDlx()).with(RK_FALLIDA);
    }

    /** Mensajes en JSON (legibles en la consola de RabbitMQ) en lugar de serialización Java. */
    @Bean
    MessageConverter mensajeriaJson() {
        return new JacksonJsonMessageConverter();
    }
}
