package com.h2togo.backend.notificaciones;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Implementación de desarrollo/demo: en vez de enviar un SMS real, registra el
 * código en el log (§10 #3). Activa en los perfiles distintos de {@code prod}.
 */
@Service
@Profile("!prod")
public class DevSmsService implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(DevSmsService.class);

    @Override
    public void enviarCodigoVerificacion(String telefono, String codigo) {
        log.info("[SMS-DEV] Código de verificación para {}: {}", telefono, codigo);
    }
}
