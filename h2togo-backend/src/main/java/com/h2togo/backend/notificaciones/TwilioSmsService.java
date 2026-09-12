package com.h2togo.backend.notificaciones;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Implementación de producción para envío de SMS usando Twilio (RN-002).
 * Se activa únicamente en el perfil "prod".
 */
@Service
@Profile("prod")
public class TwilioSmsService implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsService.class);

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;

    public TwilioSmsService(
            @Value("${h2togo.twilio.account-sid}") String accountSid,
            @Value("${h2togo.twilio.auth-token}") String authToken,
            @Value("${h2togo.twilio.from-number}") String fromNumber) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
    }

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
        log.info("[TWILIO] Cliente de Twilio inicializado correctamente para el número: {}", fromNumber);
    }

    @Override
    public void enviarCodigoVerificacion(String telefono, String codigo) {
        try {
            // Aseguramos que el teléfono tenga formato internacional.
            // Si viene como "5512345678" asumiendo México, agregamos +52 si no lo tiene.
            String toNumber = telefono;
            if (!toNumber.startsWith("+")) {
                toNumber = "+52" + toNumber;
            }

            Message message = Message.creator(
                    new PhoneNumber(toNumber),
                    new PhoneNumber(fromNumber),
                    "H2ToGo: Tu código de verificación es " + codigo + ". Válido por 5 minutos. No lo compartas."
            ).create();

            log.info("[TWILIO] SMS enviado a {}. SID: {}", toNumber, message.getSid());
        } catch (Exception e) {
            log.error("[TWILIO] Error al enviar el SMS a {}: {}", telefono, e.getMessage(), e);
            // No bloqueamos la transacción principal si el SMS falla, el usuario puede pedir reenvío.
        }
    }
}
