package com.htogo.app.data.realtime

import com.htogo.app.data.api.ApiConstants
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import com.htogo.app.plataforma.ahoraMs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

/** Eventos de una suscripción STOMP; [Desconectado] permite reintentar o caer a consultas REST. */
sealed interface EventoStomp {
    data object Conectado : EventoStomp
    data class Mensaje(val cuerpo: String) : EventoStomp
    data object Desconectado : EventoStomp
}

/**
 * Cliente STOMP mínimo sobre el WebSocket de Ktor (común a Android e iOS). Implementa solo los
 * frames que usa el backend (CONNECT, SUBSCRIBE, MESSAGE, ERROR), sin librerías extra. El
 * servidor autoriza cada SUBSCRIBE: la ubicación de un pedido solo la ven su cliente y su
 * repartidor, y {@code /user/queue/...} solo entrega los mensajes del propio usuario.
 */
object StompCliente {

    const val COLA_NOTIFICACIONES = "/user/queue/notificaciones"
    private const val LATIDO_MS = 10_000L

    fun topicoUbicacion(idPedido: Int) = "/topic/pedidos/$idPedido/ubicacion"

    /** Base REST del backend; ApiClient la ajusta si la app usa otra URL (p. ej. local en iOS). */
    var baseUrl: String = ApiConstants.BASE_URL

    private val client by lazy {
        HttpClient {
            install(WebSockets) { pingIntervalMillis = 20_000 } // mantiene viva la conexión tras proxies/ingress
        }
    }

    /** https://host/api/v1/ → wss://host/ws */
    private val wsUrl: String
        get() = baseUrl.substringBefore("/api/")
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://") + "/ws"

    /** Cada cambio cierra todas las conexiones abiertas para que se reconecten (p. ej. al volver al frente). */
    private val generacion = MutableStateFlow(0)

    /**
     * Fuerza a reconectar todas las suscripciones. Tras suspenderse (iOS lo hace al bloquear o
     * cambiar de app) el socket puede quedar muerto sin que nadie lo note.
     */
    fun reconectarTodo() {
        generacion.value++
    }

    /** Se cierra la conexión al cancelar la recolección del flujo. */
    fun suscribir(destino: String, token: String): Flow<EventoStomp> = channelFlow {
        try {
            client.webSocket(wsUrl) {
                val inicio = generacion.value
                var ultimoFrameMs = ahoraMs()
                var latidosServidor = false
                var latidosCliente = false
                // Pide latidos cada 10 s; el servidor contesta en CONNECTED cuánto acepta.
                send(Frame.Text(frame("CONNECT", "accept-version" to "1.2", "heart-beat" to "$LATIDO_MS,$LATIDO_MS", "token" to token)))
                coroutineScope {
                    // Si el servidor deja de latir o se pide reconectar, el error corta la lectura.
                    val vigilante = launch {
                        var ultimoLatidoMs = ahoraMs()
                        while (isActive) {
                            delay(2_000)
                            if (generacion.value != inicio) throw ConexionMuerta("reconexión solicitada")
                            if (latidosServidor && ahoraMs() - ultimoFrameMs > LATIDO_MS * 3) throw ConexionMuerta("sin latidos")
                            if (latidosCliente && ahoraMs() - ultimoLatidoMs >= LATIDO_MS / 2) {
                                send(Frame.Text("\n"))
                                ultimoLatidoMs = ahoraMs()
                            }
                        }
                    }
                    for (f in incoming) {
                        ultimoFrameMs = ahoraMs()
                        val texto = (f as? Frame.Text)?.readText() ?: continue
                        when (texto.trimStart('\n', '\r').substringBefore('\n').trim()) {
                            "CONNECTED" -> {
                                // heart-beat:sx,sy → el servidor late cada sx y espera latidos cada sy.
                                val latidos = cabecera(texto, "heart-beat")?.split(",")?.map { it.trim().toLongOrNull() ?: 0L }
                                latidosServidor = (latidos?.getOrNull(0) ?: 0L) > 0
                                latidosCliente = (latidos?.getOrNull(1) ?: 0L) > 0
                                send(Frame.Text(frame("SUBSCRIBE", "id" to "sub-0", "destination" to destino)))
                                this@channelFlow.send(EventoStomp.Conectado)
                            }
                            // El cuerpo va después de la línea en blanco y termina en NUL.
                            "MESSAGE" -> this@channelFlow.send(
                                EventoStomp.Mensaje(texto.substringAfter("\n\n").substringBefore('\u0000'))
                            )
                            "ERROR" -> break
                            // "" es un latido del servidor: solo actualiza ultimoFrameMs.
                        }
                    }
                    vigilante.cancel()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Caída de red, rechazo del servidor o conexión muerta: se informa como desconexión.
        }
        send(EventoStomp.Desconectado)
    }

    private class ConexionMuerta(motivo: String) : Exception(motivo)

    private fun cabecera(frame: String, nombre: String): String? =
        frame.substringBefore("\n\n").lineSequence()
            .firstOrNull { it.startsWith("$nombre:") }?.substringAfter(':')

    private fun frame(comando: String, vararg cabeceras: Pair<String, String>): String =
        buildString {
            append(comando).append('\n')
            cabeceras.forEach { (k, v) -> append(k).append(':').append(v).append('\n') }
            append('\n').append('\u0000')
        }
}
