package com.htogo.app.data.notificaciones

import com.htogo.app.data.local.SessionManager
import com.htogo.app.data.realtime.EventoStomp
import com.htogo.app.data.realtime.StompCliente
import com.htogo.shared.data.jsonApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Una notificación del backend: {"id", "titulo", "mensaje", "datos": {"tipo", "idPedido", …}}. */
data class AvisoBackend(val id: String, val titulo: String, val mensaje: String, val datos: Map<String, String>)

/**
 * Notificaciones mientras la app está abierta, comunes a Android e iOS: suscripción a
 * {@code /user/queue/notificaciones}. Funciona sin Firebase ni APNs. Se reconecta si cae la red y
 * se desconecta al cerrar sesión (o cambiar de cuenta) para no recibir avisos ajenos. Cada
 * plataforma decide cómo mostrarlas ([mostrar]).
 */
object CanalNotificaciones {

    private var job: Job? = null

    fun iniciar(scope: CoroutineScope, mostrar: (AvisoBackend) -> Unit) {
        if (job?.isActive == true) return
        val sesion = SessionManager.getInstance()
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
                        .collect { evento -> if (evento is EventoStomp.Mensaje) leer(evento.cuerpo)?.let(mostrar) }
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

    /** Mensaje con formato inesperado: se ignora (null). */
    internal fun leer(json: String): AvisoBackend? = runCatching {
        val o = jsonApi.parseToJsonElement(json).jsonObject
        val datos = (o["datos"] as? JsonObject)?.mapValues { it.value.jsonPrimitive.content }.orEmpty()
        AvisoBackend(
            o.getValue("id").jsonPrimitive.content,
            o.getValue("titulo").jsonPrimitive.content,
            o.getValue("mensaje").jsonPrimitive.content,
            datos
        )
    }.getOrNull()
}
