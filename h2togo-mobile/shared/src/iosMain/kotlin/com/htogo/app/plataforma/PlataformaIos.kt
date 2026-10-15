package com.htogo.app.plataforma

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.htogo.app.data.api.GeocodingHelper
import com.htogo.app.data.location.Posicion
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.dispatch_get_main_queue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.coroutines.flow.StateFlow
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSURL
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

/** Recibe los mensajes de enviarNativo(m) (webkit.messageHandlers.htogo). */
private class ManejadorMensajes(private val alMensaje: (String) -> Unit) : NSObject(), WKScriptMessageHandlerProtocol {
    override fun userContentController(
        userContentController: WKUserContentController,
        didReceiveScriptMessage: WKScriptMessage
    ) {
        (didReceiveScriptMessage.body as? String)?.let(alMensaje)
    }
}

/** Piezas de interfaz de iOS: los mapas usan WKWebView con el mismo HTML de Leaflet que Android. */
object UiPlataformaIos : UiPlataforma {

    /** RN-029: NWPathMonitor avisa al ganar o perder internet. */
    @Composable
    override fun rememberHayConexion(): State<Boolean> {
        val estado = remember { mutableStateOf(true) }
        DisposableEffect(Unit) {
            val monitor = nw_path_monitor_create()
            nw_path_monitor_set_update_handler(monitor) { path ->
                estado.value = nw_path_get_status(path) == nw_path_status_satisfied
            }
            nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
            nw_path_monitor_start(monitor)
            onDispose { nw_path_monitor_cancel(monitor) }
        }
        return estado
    }

    @Composable
    override fun rememberSolicitudUbicacion(alResponder: (Boolean) -> Unit): () -> Unit {
        val responder by rememberUpdatedState(alResponder)
        return remember { { UbicacionIos.solicitarPermiso { responder(it) } } }
    }

    @OptIn(ExperimentalForeignApi::class)
    @Composable
    override fun VistaWeb(
        html: String,
        modifier: Modifier,
        interactiva: Boolean,
        alCrear: (ControlWeb) -> Unit,
        alMensaje: (String) -> Unit
    ) {
        val mensaje by rememberUpdatedState(alMensaje)
        val manejador = remember { ManejadorMensajes { mensaje(it) } }
        UIKitView(
            factory = {
                val config = WKWebViewConfiguration()
                config.userContentController.addScriptMessageHandler(manejador, "htogo")
                val web = WKWebView(frame = CGRectZero.readValue(), configuration = config)
                // La política de tiles de OpenStreetMap exige identificar la app.
                web.customUserAgent = "HToGo-App/1.0 (iOS; info@h2togo.mx)"
                web.scrollView.scrollEnabled = false // el mapa maneja sus propios gestos
                web.scrollView.bounces = false
                web.loadHTMLString(html, baseURL = NSURL.URLWithString("https://openstreetmap.org"))
                alCrear(object : ControlWeb {
                    override fun ejecutar(js: String) = web.evaluateJavaScript(js, completionHandler = null)
                })
                web
            },
            modifier = modifier,
            onRelease = { it.configuration.userContentController.removeScriptMessageHandlerForName("htogo") },
            properties = UIKitInteropProperties(isInteractive = interactiva, isNativeAccessibilityEnabled = false)
        )
    }
}

/** Servicios de iOS: GPS (CoreLocation), geocodificador, ajustes y apps de navegación. */
object ServiciosIos : ServiciosPlataforma {
    override val posicionEntrega: StateFlow<Posicion?> get() = UbicacionIos.posicion
    override fun tienePermisoUbicacion(): Boolean = UbicacionIos.tienePermiso()
    override fun iniciarEntrega(idPedido: Int) = UbicacionIos.iniciar()
    override fun detenerEntrega() = UbicacionIos.detener()

    override suspend fun buscarCoordenadas(texto: String): GeocodingHelper.GeoResult? = GeocodificadorIos.buscar(texto)
    override suspend fun direccionDeCoordenadas(lat: Double, lon: Double): GeocodingHelper.GeoResult? =
        GeocodificadorIos.inversa(lat, lon)

    override fun abrirAjustesApp() {
        abrirUrl(UIApplicationOpenSettingsURLString)
    }

    /** Google Maps o Waze si están instalados; si no, Apple Maps o el enlace web de Waze. */
    override fun abrirNavegacion(app: AppNavegacion, lat: Double, lon: Double, etiqueta: String) {
        val destino = "$lat,$lon"
        when (app) {
            AppNavegacion.GOOGLE_MAPS ->
                if (!abrirUrl("comgooglemaps://?daddr=$destino&directionsmode=driving"))
                    abrirUrl("http://maps.apple.com/?daddr=$destino&dirflg=d")
            AppNavegacion.WAZE ->
                if (!abrirUrl("waze://?ll=$destino&navigate=yes"))
                    abrirUrl("https://waze.com/ul?ll=$destino&navigate=yes")
        }
    }

    private fun abrirUrl(texto: String): Boolean {
        val url = NSURL.URLWithString(texto) ?: return false
        val app = UIApplication.sharedApplication
        if (!app.canOpenURL(url)) return false
        app.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
        return true
    }
}
