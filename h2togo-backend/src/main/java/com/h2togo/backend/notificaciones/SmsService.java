package com.h2togo.backend.notificaciones;

/**
 * Envío de SMS (OTP de verificación, RN-002). El TT no fija proveedor; la interfaz
 * aísla la implementación real (intercambiable por perfil) para no bloquear el
 * desarrollo por credenciales (§1, §10 #3).
 */
public interface SmsService {

    /** Envía el código de verificación al teléfono indicado. */
    void enviarCodigoVerificacion(String telefono, String codigo);
}
