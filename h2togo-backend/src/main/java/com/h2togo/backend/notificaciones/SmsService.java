package com.h2togo.backend.notificaciones;

/**
 * Envío de SMS (OTP de verificación, RN-002). El TT no fija proveedor; la interfaz
 * aísla la implementación real (intercambiable por perfil) para no bloquear el
 * desarrollo por credenciales (§1, §10 #3).
 */
public interface SmsService {

    /** Envía el código de verificación al teléfono indicado. */
    void enviarCodigoVerificacion(String telefono, String codigo);

    /**
     * Valida el código con el proveedor si soporta validación externa (p. ej. Twilio Verify).
     * Por defecto retorna false para delegar a la base de datos local.
     */
    default boolean verificarCodigo(String telefono, String codigo) {
        return false;
    }
}
