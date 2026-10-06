package com.htogo.app.data.notificaciones

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pantalla a abrir al tocar una notificación. Las notificaciones (WebSocket y FCM) traen en sus
 * datos {@code tipo} e {@code idPedido}; en las de FCM con la app cerrada, Android los pasa como
 * extras del intent con esas mismas claves, así que ambos casos se leen igual.
 */
object DestinoNotificacion {

    const val EXTRA_TIPO = "tipo"
    const val EXTRA_PEDIDO = "idPedido"
    /** Notificación fija del servicio de ubicación: vuelve a la pantalla de ruta. */
    const val TIPO_ENTREGA_EN_CURSO = "entrega_en_curso"

    data class Destino(val tipo: String, val idPedido: Int?)

    private val _pendiente = MutableStateFlow<Destino?>(null)
    val pendiente: StateFlow<Destino?> = _pendiente.asStateFlow()

    /** Desde MainActivity (onCreate / onNewIntent). Ignora intents que no vienen de notificaciones. */
    fun desdeIntent(intent: Intent?) {
        val tipo = intent?.getStringExtra(EXTRA_TIPO) ?: return
        _pendiente.value = Destino(tipo, intent.getStringExtra(EXTRA_PEDIDO)?.toIntOrNull())
        intent.removeExtra(EXTRA_TIPO) // que una rotación no vuelva a navegar
    }

    fun consumir() {
        _pendiente.value = null
    }
}
