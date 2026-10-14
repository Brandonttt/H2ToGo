package com.htogo.app.plataforma

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.htogo.app.data.api.GeocodingHelper
import com.htogo.app.data.location.Posicion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Servicios que dependen del sistema operativo (GPS, geocodificador, servicio de entrega en
 * segundo plano). Cada app registra su implementación en [Plataforma.servicios] al arrancar;
 * las pantallas comunes los usan a través de fachadas (GeocodingHelper, UbicacionTracker,
 * EntregaEnCurso).
 */
interface ServiciosPlataforma {
    suspend fun buscarCoordenadas(texto: String): GeocodingHelper.GeoResult? = null
    suspend fun direccionDeCoordenadas(lat: Double, lon: Double): GeocodingHelper.GeoResult? = null
    fun tienePermisoUbicacion(): Boolean = false
    /** Inicia el seguimiento GPS de una entrega (en Android, servicio en primer plano). */
    fun iniciarEntrega(idPedido: Int) {}
    fun detenerEntrega() {}
    /** Última posición del seguimiento de la entrega en curso. */
    val posicionEntrega: StateFlow<Posicion?>
    /** Abre los ajustes de la app (para conceder permisos negados). */
    fun abrirAjustesApp() {}
    /** Abre la navegación hacia un destino en una app externa (Google Maps / Waze). */
    fun abrirNavegacion(app: AppNavegacion, lat: Double, lon: Double, etiqueta: String) {}
}

enum class AppNavegacion { GOOGLE_MAPS, WAZE }

/** Implementación vacía hasta que la app registre la suya. */
private object SinServicios : ServiciosPlataforma {
    override val posicionEntrega: StateFlow<Posicion?> = MutableStateFlow(null)
}

/** Piezas de interfaz propias de cada plataforma: mapas (WebView/WKWebView), red y permisos. */
interface UiPlataforma {
    @Composable
    fun MapaSelector(
        latitude: Double, longitude: Double, modifier: Modifier, heightDp: Int, zoom: Int,
        isInteractive: Boolean, isDraggablePin: Boolean, onLocationChange: ((Double, Double) -> Unit)?
    )

    @Composable
    fun MapaRuta(
        originLat: Double?, originLon: Double?, destLat: Double, destLon: Double, modifier: Modifier,
        routePoints: List<Pair<Double, Double>>, isInteractive: Boolean, repartidorLabel: String,
        seguirRepartidor: Boolean, avisoSinUbicacion: String?, estadoPedido: String?
    )

    /** true mientras hay internet (RN-029). */
    @Composable
    fun rememberHayConexion(): State<Boolean> = remember { mutableStateOf(true) }

    /** Devuelve una función que pide el permiso de ubicación y responde con el resultado. */
    @Composable
    fun rememberSolicitudUbicacion(alResponder: (Boolean) -> Unit): () -> Unit = { alResponder(false) }
}

val LocalUiPlataforma = staticCompositionLocalOf<UiPlataforma> {
    error("Falta proveer LocalUiPlataforma en la raíz de la app")
}

/** Registro de las implementaciones de cada plataforma. */
object ServiciosRegistro {
    var servicios: ServiciosPlataforma = SinServicios
}
