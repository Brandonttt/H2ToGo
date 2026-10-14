package com.htogo.app.plataforma

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

/** Milisegundos desde 1970 (reemplaza System.currentTimeMillis en código común). */
fun ahoraMs(): Long = Clock.System.now().toEpochMilliseconds()

/** Ganchos que cada app configura al arrancar. */
object Plataforma {
    /** Registra el token de notificaciones del dispositivo tras iniciar sesión (FCM / APNs). */
    var registrarDispositivo: () -> Unit = {}

    /** GPS, geocodificador, etc. de la plataforma (ver [ServiciosPlataforma]). */
    var servicios: ServiciosPlataforma
        get() = ServiciosRegistro.servicios
        set(value) { ServiciosRegistro.servicios = value }
}

/** Mensaje breve al usuario: Toast en Android; en iOS, una franja que pinta la raíz de la app. */
expect fun mostrarAviso(texto: String, largo: Boolean = false)

/** Aviso pendiente en plataformas sin Toast (iOS); la raíz de la app lo muestra y lo limpia. */
object AvisosEnPantalla {
    private val _actual = MutableStateFlow<String?>(null)
    val actual: StateFlow<String?> = _actual.asStateFlow()
    fun mostrar(texto: String) { _actual.value = texto }
    fun limpiar() { _actual.value = null }
}
