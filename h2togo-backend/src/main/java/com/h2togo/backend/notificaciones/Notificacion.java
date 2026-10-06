package com.h2togo.backend.notificaciones;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Notificación para un usuario: es el mensaje que viaja por RabbitMQ y el que recibe la app (por
 * WebSocket o FCM). {@code id} permite a la app descartar duplicados cuando un reintento la
 * reenvía o llega por los dos canales. {@code datos} lleva contexto, p. ej. {@code tipo} e
 * {@code idPedido}, para que la app abra la pantalla correcta.
 */
public record Notificacion(
        String id,
        int idUsuario,
        String titulo,
        String mensaje,
        Map<String, String> datos,
        String creadaEn
) {

    public static Notificacion nueva(int idUsuario, String titulo, String mensaje, Map<String, String> datos) {
        return new Notificacion(UUID.randomUUID().toString(), idUsuario, titulo, mensaje,
                datos == null ? Map.of() : Map.copyOf(datos), OffsetDateTime.now().toString());
    }
}
