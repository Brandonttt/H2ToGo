package com.h2togo.backend.security;

import com.h2togo.backend.tracking.AccesoRastreo;
import com.h2togo.backend.usuarios.Usuario;
import com.h2togo.backend.usuarios.UsuarioRepository;
import java.time.OffsetDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * en la sesión. Reusa la misma regla que el filtro REST (RN-018/RNF-004). En el SUBSCRIBE
 * autoriza el tópico: la ubicación de un pedido solo la ven su cliente y su repartidor (RN-016).
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String PREFIJO = "Bearer ";
    private static final String COLA_NOTIFICACIONES = "/user/queue/notificaciones";
    private static final Pattern TOPICO_UBICACION = Pattern.compile("^/topic/pedidos/(\\d+)/ubicacion$");

    private final UsuarioRepository usuarioRepository;
    private final AccesoRastreo accesoRastreo;

    public StompAuthChannelInterceptor(UsuarioRepository usuarioRepository, AccesoRastreo accesoRastreo) {
        this.usuarioRepository = usuarioRepository;
        this.accesoRastreo = accesoRastreo;
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
        } else if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            autorizarSuscripcion(accessor);
        }
        return message;
    }

    /**
     * Destinos permitidos: la cola de notificaciones propia (Spring la resuelve a la sesión del
     * usuario, nadie puede leer la de otro) y la ubicación de un pedido del que se es parte.
     */
    private void autorizarSuscripcion(StompHeaderAccessor accessor) {
        String destino = accessor.getDestination();
        if (COLA_NOTIFICACIONES.equals(destino) && accessor.getUser() instanceof StompPrincipal) {
            return;
        }
        Matcher m = destino == null ? null : TOPICO_UBICACION.matcher(destino);
        if (m == null || !m.matches() || !(accessor.getUser() instanceof StompPrincipal p)
                || !accesoRastreo.puedeVer(p.idUsuario(), Integer.parseInt(m.group(1)))) {
            throw new MessagingException("No tiene permiso para suscribirse a " + destino + ".");
        }
    }

    private static boolean vigente(Usuario u) {
        return u.isCuentaActiva() && u.getSesionFechaExpiracion() != null
                && u.getSesionFechaExpiracion().isAfter(OffsetDateTime.now());
    }
}
