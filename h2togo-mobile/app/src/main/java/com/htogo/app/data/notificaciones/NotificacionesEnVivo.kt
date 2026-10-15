package com.htogo.app.data.notificaciones

import android.content.Context
import kotlinx.coroutines.CoroutineScope

/**
 * Notificaciones por WebSocket mientras la app está en primer plano (sin Firebase). El canal es
 * común con iOS ([CanalNotificaciones]); aquí solo se muestran con el Notificador de Android.
 */
object NotificacionesEnVivo {

    fun iniciar(context: Context, scope: CoroutineScope) {
        val app = context.applicationContext
        CanalNotificaciones.iniciar(scope) { a -> Notificador.mostrar(app, a.id, a.titulo, a.mensaje, a.datos) }
    }

    fun detener() = CanalNotificaciones.detener()
}
