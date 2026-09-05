package com.h2togo.backend.security;

import com.h2togo.backend.usuarios.Usuario;
import com.h2togo.backend.usuarios.UsuarioRepository;
import java.time.OffsetDateTime;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Autentica la conexión STOMP en el CONNECT: valida el token opaco (cabecera nativa
 * {@code token}, opcionalmente con prefijo {@code Bearer}) y monta {@link StompPrincipal}
 * en la sesión. Reusa la misma regla que el filtro REST (RN-018/RNF-004).
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String PREFIJO = "Bearer ";

    private final UsuarioRepository usuarioRepository;

    public StompAuthChannelInterceptor(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("token");
            if (token != null && token.startsWith(PREFIJO)) {
                token = token.substring(PREFIJO.length()).trim();
            }
            Usuario u = (token == null || token.isBlank()) ? null
                    : usuarioRepository.findByTokenSesion(token).filter(StompAuthChannelInterceptor::vigente).orElse(null);
            if (u == null) {
                throw new MessagingException("Token de sesión inválido para el WebSocket.");
            }
            accessor.setUser(new StompPrincipal(u.getId(), u.getRol()));
        }
        return message;
    }

    private static boolean vigente(Usuario u) {
        return u.isCuentaActiva() && u.getSesionFechaExpiracion() != null
                && u.getSesionFechaExpiracion().isAfter(OffsetDateTime.now());
    }
}
