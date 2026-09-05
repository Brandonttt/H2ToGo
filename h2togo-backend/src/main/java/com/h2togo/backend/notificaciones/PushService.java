package com.h2togo.backend.notificaciones;

/**
 * Notificaciones push (FCM). El TT no fija credenciales del proyecto Firebase (§10 #4);
 * la interfaz aísla la implementación real (intercambiable por perfil), con impl {@code dev}
 * que registra en log para no bloquear el desarrollo.
 */
public interface PushService {

    /** Envía una notificación push al usuario indicado (resuelve su token FCM la impl real). */
    void notificar(int idUsuario, String titulo, String mensaje);
}
