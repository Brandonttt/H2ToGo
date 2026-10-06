package com.htogo.app.data.realtime

import com.htogo.app.data.api.ApiConstants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/** Eventos de una suscripción STOMP; [Desconectado] permite reintentar o caer a consultas REST. */
sealed interface EventoStomp {
    data object Conectado : EventoStomp
    data class Mensaje(val cuerpo: String) : EventoStomp
    data object Desconectado : EventoStomp
}

/**
 * Cliente STOMP mínimo sobre el WebSocket de OkHttp. Implementa solo los frames que usa el backend
 * (CONNECT, SUBSCRIBE, MESSAGE, ERROR), sin librerías extra. El servidor autoriza cada SUBSCRIBE:
 * la ubicación de un pedido solo la ven su cliente y su repartidor, y {@code /user/queue/...}
 * solo entrega los mensajes del propio usuario.
 */
object StompCliente {

    const val COLA_NOTIFICACIONES = "/user/queue/notificaciones"

    fun topicoUbicacion(idPedido: Int) = "/topic/pedidos/$idPedido/ubicacion"

    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS) // mantiene viva la conexión tras proxies/ingress
        .build()

    /** https://host/api/v1/ → wss://host/ws */
    private val wsUrl: String = ApiConstants.BASE_URL
        .substringBefore("/api/")
        .replaceFirst("https://", "wss://")
        .replaceFirst("http://", "ws://") + "/ws"

    /** Se cierra la conexión al cancelar la recolección del flujo. */
    fun suscribir(destino: String, token: String): Flow<EventoStomp> = callbackFlow {
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(frame("CONNECT", "accept-version" to "1.2", "heart-beat" to "0,0", "token" to token))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                when (text.substringBefore('\n').trim()) {
                    "CONNECTED" -> {
                        webSocket.send(frame("SUBSCRIBE", "id" to "sub-0", "destination" to destino))
                        trySend(EventoStomp.Conectado)
                    }
                    // El cuerpo va después de la línea en blanco y termina en NUL.
                    "MESSAGE" -> trySend(EventoStomp.Mensaje(text.substringAfter("\n\n").substringBefore('\u0000')))
                    "ERROR" -> {
                        trySend(EventoStomp.Desconectado)
                        webSocket.close(1000, null)
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                trySend(EventoStomp.Desconectado)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                trySend(EventoStomp.Desconectado)
            }
        }
        val socket = client.newWebSocket(Request.Builder().url(wsUrl).build(), listener)
        awaitClose { socket.close(1000, null) }
    }

    private fun frame(comando: String, vararg cabeceras: Pair<String, String>): String =
        buildString {
            append(comando).append('\n')
            cabeceras.forEach { (k, v) -> append(k).append(':').append(v).append('\n') }
            append('\n').append('\u0000')
        }
}
