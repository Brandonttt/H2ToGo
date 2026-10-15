package com.h2togo.backend.config;

import com.h2togo.backend.security.StompAuthChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket/STOMP. Handshake en {@code /ws} (token en el CONNECT). Dos usos:
 * <ul>
 *   <li>Rastreo (CU-006): el repartidor publica en {@code /app/pedidos/{id}/ubicacion} y el
 *       backend rebota a {@code /topic/pedidos/{id}/ubicacion}.</li>
 *   <li>Notificaciones en la app: cada usuario se suscribe a {@code /user/queue/notificaciones}
 *       y solo recibe las suyas (destino de usuario de Spring).</li>
 * </ul>
 * Heart-beats STOMP cada 10 s en ambos sentidos: la app detecta una conexión muerta (p. ej. tras
 * suspenderse en iOS) y se reconecta, y el servidor cierra las sesiones que dejaron de latir.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final long LATIDO_MS = 10_000;

    private final StompAuthChannelInterceptor authInterceptor;

    public WebSocketConfig(StompAuthChannelInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        ThreadPoolTaskScheduler latidos = new ThreadPoolTaskScheduler();
        latidos.setPoolSize(1);
        latidos.setThreadNamePrefix("ws-heartbeat-");
        latidos.initialize();
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[] {LATIDO_MS, LATIDO_MS})
                .setTaskScheduler(latidos);
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }
}
