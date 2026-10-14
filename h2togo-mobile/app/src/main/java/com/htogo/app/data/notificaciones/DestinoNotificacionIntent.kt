package com.htogo.app.data.notificaciones

import android.content.Intent

/** Desde MainActivity (onCreate / onNewIntent). Ignora intents que no vienen de notificaciones. */
fun DestinoNotificacion.desdeIntent(intent: Intent?) {
    val tipo = intent?.getStringExtra(DestinoNotificacion.EXTRA_TIPO) ?: return
    abrir(tipo, intent.getStringExtra(DestinoNotificacion.EXTRA_PEDIDO)?.toIntOrNull())
    intent.removeExtra(DestinoNotificacion.EXTRA_TIPO) // que una rotación no vuelva a navegar
}
