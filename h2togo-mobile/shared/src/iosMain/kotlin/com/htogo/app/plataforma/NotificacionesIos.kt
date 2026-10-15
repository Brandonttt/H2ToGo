package com.htogo.app.plataforma

import com.htogo.app.data.notificaciones.AvisoBackend
import com.htogo.app.data.notificaciones.DestinoNotificacion
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionList
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

/** Muestra los avisos también con la app abierta y abre su pantalla al tocarlos. */
private class DelegadoNotificaciones : NSObject(), UNUserNotificationCenterDelegateProtocol {
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit
    ) {
        withCompletionHandler(
            UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionList or UNNotificationPresentationOptionSound
        )
    }

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit
    ) {
        val info = didReceiveNotificationResponse.notification.request.content.userInfo
        (info["tipo"] as? String)?.let { tipo ->
            DestinoNotificacion.abrir(tipo, (info["idPedido"] as? String)?.toIntOrNull())
        }
        withCompletionHandler()
    }
}

/**
 * Notificaciones locales de iOS para los avisos que llegan por WebSocket con la app abierta.
 * No requieren cuenta de Apple Developer (las push con la app cerrada sí: APNs).
 */
internal object NotificacionesIos {
    private val delegado = DelegadoNotificaciones()
    private val centro get() = UNUserNotificationCenter.currentNotificationCenter()

    fun preparar() {
        centro.delegate = delegado
        centro.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        ) { _, _ -> }
    }

    fun mostrar(a: AvisoBackend) {
        val contenido = UNMutableNotificationContent().apply {
            setTitle(a.titulo)
            setBody(a.mensaje)
            setSound(UNNotificationSound.defaultSound)
            setUserInfo(a.datos.mapKeys { it.key as Any? })
        }
        centro.addNotificationRequest(UNNotificationRequest.requestWithIdentifier(a.id, contenido, null)) { _ -> }
    }
}
