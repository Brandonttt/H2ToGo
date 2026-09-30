package com.h2togo.backend.notificaciones;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Implementación de producción para envío de SMS usando Twilio (RN-002).
 * Soporta Twilio Verify API (funciona en cuentas Trial y estándar) y fallback a Messaging API.
 * Se activa únicamente en el perfil "prod".
 */
@Service
@Profile("prod")
public class TwilioSmsService implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsService.class);

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final String verifyServiceSid;

    public TwilioSmsService(
            @Value("${h2togo.twilio.account-sid}") String accountSid,
            @Value("${h2togo.twilio.auth-token}") String authToken,
            @Value("${h2togo.twilio.from-number}") String fromNumber,
            @Value("${h2togo.twilio.verify-service-sid:VAf49c305047af03c695f90bcf94e6a209}") String verifyServiceSid) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        this.verifyServiceSid = verifyServiceSid;
    }

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
        log.info("[TWILIO] Cliente de Twilio inicializado. Emisor: {}, VerifyService: {}", fromNumber, verifyServiceSid);
    }

    @Override
    public void enviarCodigoVerificacion(String telefono, String codigo) {
        String toNumber = telefono;
        if (!toNumber.startsWith("+")) {
            toNumber = "+52" + toNumber;
        }

        // 1. Intentar con Twilio Verify API (diseñado para OTP, opera en cuentas Trial y Prod)
        if (verifyServiceSid != null && !verifyServiceSid.isBlank()) {
            try {
                Verification verification = Verification.creator(verifyServiceSid, toNumber, "sms").create();
                log.info("[TWILIO-VERIFY] SMS de verificación solicitado a {}. SID: {}", toNumber, verification.getSid());
                return;
            } catch (Exception e) {
                log.warn("[TWILIO-VERIFY] No se pudo enviar por Verify API ({}). Probando fallback...", e.getMessage());
            }
        }

        // 2. Fallback a Messaging API estándar
        try {
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

    @Override
    public boolean verificarCodigo(String telefono, String codigo) {
        if (verifyServiceSid == null || verifyServiceSid.isBlank()) {
            return false;
        }
        try {
            String toNumber = telefono;
            if (!toNumber.startsWith("+")) {
                toNumber = "+52" + toNumber;
            }
            VerificationCheck check = VerificationCheck.creator(verifyServiceSid)
                    .setTo(toNumber)
                    .setCode(codigo)
                    .create();

            boolean aprobado = "approved".equalsIgnoreCase(check.getStatus()) || Boolean.TRUE.equals(check.getValid());
            log.info("[TWILIO-VERIFY] Validación de código para {}: status={}, valid={}", toNumber, check.getStatus(), check.getValid());
            return aprobado;
        } catch (Exception e) {
            log.warn("[TWILIO-VERIFY] Error al validar código con Verify API para {}: {}", telefono, e.getMessage());
            return false;
        }
    }
}
