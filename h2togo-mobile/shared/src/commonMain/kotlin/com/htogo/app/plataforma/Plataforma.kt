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

    /** Íconos de la barra de estado claros (true) u oscuros; ver [com.htogo.app.ui.components.BarraEstadoClara]. */
    var barraEstadoClara: (Boolean) -> Unit = {}

    /** GPS, geocodificador, etc. de la plataforma (ver [ServiciosPlataforma]). */
    var servicios: ServiciosPlataforma
        get() = ServiciosRegistro.servicios
        set(value) { ServiciosRegistro.servicios = value }
}

/**
 * Mensaje breve al usuario, igual en Android e iOS: una tarjeta arriba de la pantalla que pinta
 * la raíz de la app ([AvisosEnPantalla]). [largo]: se queda más tiempo (errores, instrucciones).
 */
fun mostrarAviso(texto: String, largo: Boolean = false) = AvisosEnPantalla.mostrar(texto, largo)

/** Aviso en pantalla; [id] distingue dos avisos seguidos con el mismo texto. */
data class Aviso(val texto: String, val largo: Boolean, val id: Long = ahoraMs())

/** Aviso actual; la raíz de la app lo muestra y lo limpia. */
object AvisosEnPantalla {
    private val _actual = MutableStateFlow<Aviso?>(null)
    val actual: StateFlow<Aviso?> = _actual.asStateFlow()
    fun mostrar(texto: String, largo: Boolean = false) { _actual.value = Aviso(texto, largo) }
    fun limpiar(aviso: Aviso) { _actual.compareAndSet(aviso, null) }
}
