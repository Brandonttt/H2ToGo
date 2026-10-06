package com.h2togo.backend.notificaciones;

import java.util.Map;

/**
 * Notificaciones al usuario. Los servicios solo declaran qué avisar; la implementación decide
 * cómo: {@link NotificacionesEnCola} las publica en RabbitMQ (producción) y
 * {@link NotificacionesDirectas} las entrega en el mismo proceso (desarrollo y pruebas sin broker).
 * En ambos casos el envío ocurre después del commit: si la transacción se revierte, no se avisa.
 */
public interface PushService {

    /** @param datos contexto para la app, p. ej. {@code tipo} e {@code idPedido}. */
    void notificar(int idUsuario, String titulo, String mensaje, Map<String, String> datos);

    default void notificar(int idUsuario, String titulo, String mensaje) {
        notificar(idUsuario, titulo, mensaje, Map.of());
    }
}
