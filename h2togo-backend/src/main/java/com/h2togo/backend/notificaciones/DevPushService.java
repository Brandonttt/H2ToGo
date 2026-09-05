package com.h2togo.backend.notificaciones;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** Implementación de desarrollo/demo: registra la notificación en el log (§10 #4). */
@Service
@Profile("!prod")
public class DevPushService implements PushService {

    private static final Logger log = LoggerFactory.getLogger(DevPushService.class);

    @Override
    public void notificar(int idUsuario, String titulo, String mensaje) {
        log.info("[PUSH-DEV] usuario {} · {}: {}", idUsuario, titulo, mensaje);
    }
}
