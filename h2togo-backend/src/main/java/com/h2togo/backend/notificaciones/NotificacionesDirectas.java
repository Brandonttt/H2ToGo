package com.h2togo.backend.notificaciones;

import com.h2togo.backend.bd.TareaSistema;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Entrega en el mismo proceso, sin broker (h2togo.mensajeria.habilitada=false): desarrollo local
 * y pruebas. Un fallo de entrega solo se registra; no hay reintentos. Se entrega en segundo plano:
 * leer el token FCM pide una conexión, y la petición todavía tiene la suya al confirmar.
 */
@Service
@ConditionalOnProperty(name = "h2togo.mensajeria.habilitada", havingValue = "false", matchIfMissing = true)
public class NotificacionesDirectas implements PushService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesDirectas.class);

    private final EntregaNotificaciones entrega;
    private final TareaSistema tareaSistema;

    public NotificacionesDirectas(EntregaNotificaciones entrega, TareaSistema tareaSistema) {
        this.entrega = entrega;
        this.tareaSistema = tareaSistema;
    }

    @Override
    public void notificar(int idUsuario, String titulo, String mensaje, Map<String, String> datos) {
        Notificacion n = Notificacion.nueva(idUsuario, titulo, mensaje, datos);
        tareaSistema.despuesDelCommit(() -> {
            try {
                entrega.entregar(n);
            } catch (RuntimeException e) {
                log.warn("No se pudo entregar la notificación {} al usuario {}: {}", n.id(), idUsuario, e.getMessage());
            }
        });
    }
}
