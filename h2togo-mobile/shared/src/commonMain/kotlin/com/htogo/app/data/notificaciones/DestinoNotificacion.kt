package com.htogo.app.data.notificaciones

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

    /** Pide abrir la pantalla de una notificación (Android: desde el intent; iOS: desde APNs). */
    fun abrir(tipo: String, idPedido: Int?) {
        _pendiente.value = Destino(tipo, idPedido)
    }

    fun consumir() {
        _pendiente.value = null
    }
}
