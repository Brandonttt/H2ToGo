package com.htogo.app.data.notificaciones

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Recibe las notificaciones de FCM. Con la app en segundo plano, Android muestra la notificación
 * él solo con el bloque "notification"; aquí llegan las que se reciben con la app abierta, que
 * pasan por el mismo [Notificador] (y su deduplicación) que el canal WebSocket.
 */
class HToGoMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        DispositivoFcm.enviar(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val id = message.data["idNotificacion"] ?: message.messageId ?: return
        val titulo = message.notification?.title ?: return
        Notificador.mostrar(applicationContext, id, titulo, message.notification?.body.orEmpty(), message.data)
    }
}
