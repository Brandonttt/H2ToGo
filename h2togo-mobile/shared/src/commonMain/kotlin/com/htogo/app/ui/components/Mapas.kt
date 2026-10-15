package com.htogo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htogo.app.plataforma.ControlWeb
import com.htogo.app.plataforma.LocalUiPlataforma
import com.htogo.shared.recursos.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Leaflet (CSS y JS) desde los recursos compartidos; se lee una vez por ejecución. */
private object Leaflet {
    private var cache: Pair<String, String>? = null

    @OptIn(ExperimentalResourceApi::class)
    suspend fun cargar(): Pair<String, String> = cache ?: Pair(
        Res.readBytes("files/leaflet/leaflet.css").decodeToString(),
        Res.readBytes("files/leaflet/leaflet.js").decodeToString()
    ).also { cache = it }
}

@Composable
private fun rememberLeaflet(): Pair<String, String>? =
    produceState<Pair<String, String>?>(null) { value = Leaflet.cargar() }.value

/** Mapa con un pin (opcionalmente arrastrable) para elegir o mostrar una ubicación. */
@Composable
fun OsmMapView(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier,
    heightDp: Int = 220,
    zoom: Int = 16,
    isInteractive: Boolean = true,
    isDraggablePin: Boolean = false,
    onLocationChange: ((Double, Double) -> Unit)? = null
) {
    val leaflet = rememberLeaflet()
    var control by remember { mutableStateOf<ControlWeb?>(null) }
    var listo by remember { mutableStateOf(false) }
    val alCambiar by rememberUpdatedState(onLocationChange)

    Box(modifier = modifier) {
        if (leaflet == null) {
            CargandoMapa()
        } else {
            // La página se arma una vez; los cambios de posición se aplican con JavaScript.
            val html = remember(leaflet, zoom, isInteractive, isDraggablePin, heightDp) {
                htmlMapaSelector(leaflet.first, leaflet.second, latitude, longitude, zoom, isInteractive, isDraggablePin, heightDp)
            }
            LocalUiPlataforma.current.VistaWeb(
                html = html,
                modifier = Modifier.fillMaxSize(),
                interactiva = isInteractive,
                alCrear = { control = it },
                alMensaje = { m ->
                    when {
                        m == "listo" -> listo = true
                        m.startsWith("ubicacion:") -> {
                            val (lat, lon) = m.removePrefix("ubicacion:").split(",").map { it.toDoubleOrNull() }
                            if (lat != null && lon != null) alCambiar?.invoke(lat, lon)
                        }
                    }
                }
            )
            if (isInteractive) SelectorCapa(control, Modifier.align(Alignment.TopEnd).padding(8.dp), compacto = true)
        }
    }

    LaunchedEffect(listo, latitude, longitude) {
        if (listo) control?.ejecutar("fixMapHeight(); updatePosition($latitude, $longitude);")
    }
}

/**
 * Mapa con el repartidor, el destino y la ruta (seguimiento del cliente y navegación del
 * repartidor). El marcador se desliza entre posiciones del GPS y lleva una flecha con su rumbo;
 * con [seguirRepartidor] la cámara lo sigue (rumbo arriba o norte arriba) hasta que el usuario
 * arrastra el mapa, y un botón lo vuelve a centrar.
 */
@Composable
fun OsmRouteMapView(
    originLat: Double?,
    originLon: Double?,
    destLat: Double,
    destLon: Double,
    modifier: Modifier = Modifier,
    routePoints: List<Pair<Double, Double>> = emptyList(),
    isInteractive: Boolean = true,
    repartidorLabel: String = "Tu ubicación",
    /** Navegación del repartidor: la cámara lo sigue y el mapa gira con su rumbo. */
    seguirRepartidor: Boolean = false,
    /** Aviso mientras no hay posición; null para no mostrarlo. */
    avisoSinUbicacion: String? = "Buscando señal GPS...",
    /** RF-007: estado del pedido; colorea los marcadores y muestra una etiqueta. */
    estadoPedido: String? = null,
    /** Alto del panel de la pantalla que tapa la parte de abajo del mapa. */
    margenInferior: Dp = 0.dp
) {
    val leaflet = rememberLeaflet()
    var control by remember { mutableStateOf<ControlWeb?>(null) }
    var listo by remember { mutableStateOf(false) }
    // Los controles van debajo de la barra superior de la pantalla, que crece con la barra de
    // estado del teléfono (en iPhone con isla dinámica es mucho más alta que en Android).
    // Se mide con un Spacer para respetar si la pantalla ya descontó la barra de estado.
    var altoBarraEstado by remember { mutableStateOf(0.dp) }
    val densidad = LocalDensity.current
    val arriba = altoBarraEstado + 72.dp

    Box(modifier = modifier) {
        Spacer(
            Modifier.windowInsetsTopHeight(WindowInsets.statusBars)
                .onSizeChanged { altoBarraEstado = with(densidad) { it.height.toDp() } }
        )
        if (leaflet == null) {
            CargandoMapa()
        } else {
            // La página no depende de datos que cambien: se arma una vez con el destino inicial.
            val html = remember(leaflet) {
                htmlMapaRuta(leaflet.first, leaflet.second, destLat, destLon, isInteractive, seguirRepartidor)
            }
            LocalUiPlataforma.current.VistaWeb(
                html = html,
                modifier = Modifier.fillMaxSize(),
                interactiva = isInteractive,
                alCrear = { control = it },
                alMensaje = { if (it == "listo") listo = true }
            )
            SelectorCapa(control, Modifier.align(Alignment.TopEnd).padding(top = arriba, end = 12.dp))
        }

        // CU-017 S1: indicador de búsqueda de señal GPS.
        if (avisoSinUbicacion != null && (originLat == null || originLon == null)) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.75f),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = arriba)
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(12.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(avisoSinUbicacion, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    val rutaJson = remember(routePoints) {
        routePoints.joinToString(prefix = "[", postfix = "]") { "[${it.first},${it.second}]" }
    }
    val etiquetaJs = remember(repartidorLabel) { repartidorLabel.replace("\\", "").replace("'", "\\'") }

    LaunchedEffect(listo, originLat, originLon, destLat, destLon, rutaJson, etiquetaJs) {
        if (!listo) return@LaunchedEffect
        val rep = if (originLat != null && originLon != null) "$originLat, $originLon" else "null, null"
        control?.ejecutar("actualizar($rep, $destLat, $destLon, $rutaJson, '$etiquetaJs');")
    }

    LaunchedEffect(listo, margenInferior) {
        if (listo) control?.ejecutar("setMargenInferior(${margenInferior.value.toInt()});")
    }

    LaunchedEffect(listo, arriba) {
        if (listo) control?.ejecutar("document.documentElement.style.setProperty('--arriba', '${arriba.value.toInt()}px');")
    }

    LaunchedEffect(listo, estadoPedido) {
        if (!listo || estadoPedido == null) return@LaunchedEffect
        val (color, texto) = when (estadoPedido.lowercase()) {
            "pendiente", "pendiente_programado" -> "#94A3B8" to "Buscando repartidor"
            "asignado" -> "#F59E0B" to "Repartidor asignado"
            "en_camino" -> "#0284C7" to "En camino"
            "entregado" -> "#10B981" to "Entregado"
            "no_entregado" -> "#EF4444" to "No entregado"
            "cancelado" -> "#EF4444" to "Cancelado"
            else -> "#0284C7" to ""
        }
        val cerrado = estadoPedido.lowercase() in setOf("entregado", "no_entregado", "cancelado")
        control?.ejecutar("setEstado('$color', '$texto', $cerrado);")
    }
}

/** CU-017: alterna la capa del mapa (OSM por defecto / Google). */
@Composable
private fun SelectorCapa(control: ControlWeb?, modifier: Modifier, compacto: Boolean = false) {
    var capaOsm by remember { mutableStateOf(true) }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.95f),
        shadowElevation = if (compacto) 3.dp else 4.dp,
        modifier = modifier.clickable {
            capaOsm = !capaOsm
            control?.ejecutar("if (window.setCapa) window.setCapa('${if (capaOsm) "osm" else "google"}');")
        }
    ) {
        Text(
            if (capaOsm) "🗺️ OSM" else "🗺️ Google Maps",
            fontSize = if (compacto) 10.sp else 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (capaOsm) Color(0xFF0284C7) else Color(0xFF1A73E8),
            modifier = Modifier.padding(horizontal = if (compacto) 8.dp else 10.dp, vertical = if (compacto) 4.dp else 6.dp)
        )
    }
}

@Composable
private fun CargandoMapa() {
    Box(Modifier.fillMaxSize().background(Color(0xFFDDE7EE)), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(24.dp), color = Color(0xFF0284C7), strokeWidth = 2.dp)
    }
}
