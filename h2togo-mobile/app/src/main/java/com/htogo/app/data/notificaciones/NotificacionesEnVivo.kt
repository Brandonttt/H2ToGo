package com.htogo.app.data.notificaciones

import android.content.Context
import com.htogo.app.data.local.SessionManager
import com.htogo.app.data.realtime.EventoStomp
import com.htogo.app.data.realtime.StompCliente
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Canal de notificaciones mientras la app está en primer plano: suscripción a
 * {@code /user/queue/notificaciones}. Funciona sin Firebase. Se reconecta si cae la red y se
 * desconecta al cerrar sesión (o al cambiar de cuenta) para no recibir avisos ajenos.
 */
object NotificacionesEnVivo {

    private var job: Job? = null

    fun iniciar(context: Context, scope: CoroutineScope) {
        if (job?.isActive == true) return
        val app = context.applicationContext
        val sesion = SessionManager.getInstance(app)
        job = scope.launch {
            while (isActive) {
                val token = sesion.obtenerToken()
                if (token.isNullOrBlank()) {
                    delay(3_000) // aún sin sesión: espera al login
                    continue
                }
                val conexion = launch {
                    StompCliente.suscribir(StompCliente.COLA_NOTIFICACIONES, token)
                        .takeWhile { it !is EventoStomp.Desconectado }
                        .collect { evento ->
                            if (evento is EventoStomp.Mensaje) mostrar(app, evento.cuerpo)
                        }
                }
                // Vigila la sesión: si cambia el token (logout / otra cuenta), corta la conexión.
                while (conexion.isActive && sesion.obtenerToken() == token) delay(3_000)
                conexion.cancel()
                delay(3_000) // espera antes de reconectar
            }
        }
    }

    fun detener() {
        job?.cancel()
        job = null
    }

    private fun mostrar(context: Context, json: String) {
        try {
            val o = JSONObject(json)
            val datos = o.optJSONObject("datos")?.let { d -> d.keys().asSequence().associateWith { d.getString(it) } }
            Notificador.mostrar(context, o.getString("id"), o.getString("titulo"), o.getString("mensaje"), datos.orEmpty())
        } catch (_: Exception) {
            // Mensaje con formato inesperado: se ignora.
        }
    }
}
