package com.htogo.app.data.realtime

import com.htogo.app.data.api.ApiConstants
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
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

    /** Se cierra la conexión al cancelar la recolección del flujo. */
    fun suscribir(destino: String, token: String): Flow<EventoStomp> = channelFlow {
        try {
            client.webSocket(wsUrl) {
                send(Frame.Text(frame("CONNECT", "accept-version" to "1.2", "heart-beat" to "0,0", "token" to token)))
                for (f in incoming) {
                    val texto = (f as? Frame.Text)?.readText() ?: continue
                    when (texto.substringBefore('\n').trim()) {
                        "CONNECTED" -> {
                            send(Frame.Text(frame("SUBSCRIBE", "id" to "sub-0", "destination" to destino)))
                            this@channelFlow.send(EventoStomp.Conectado)
                        }
                        // El cuerpo va después de la línea en blanco y termina en NUL.
                        "MESSAGE" -> this@channelFlow.send(
                            EventoStomp.Mensaje(texto.substringAfter("\n\n").substringBefore('\u0000'))
                        )
                        "ERROR" -> {
                            close()
                            break
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Caída de red o rechazo del servidor: se informa como desconexión.
        }
        send(EventoStomp.Desconectado)
    }

    private fun frame(comando: String, vararg cabeceras: Pair<String, String>): String =
        buildString {
            append(comando).append('\n')
            cabeceras.forEach { (k, v) -> append(k).append(':').append(v).append('\n') }
            append('\n').append('\u0000')
        }
}
