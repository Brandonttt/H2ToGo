package com.htogo.app.ui.components

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Mapa Leaflet con el repartidor, el destino y la ruta. La página se carga una sola vez y los
 * cambios (posición en vivo, ruta recalculada) se aplican con JavaScript, para que el mapa no
 * parpadee ni pierda el zoom en cada actualización del GPS.
 *
 * @param originLat posición del repartidor; null mientras no se conoce (no se dibuja el marcador).
 * @param repartidorLabel texto del globo del marcador del repartidor.
 */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun OsmRouteMapView(
    originLat: Double?,
    originLon: Double?,
    destLat: Double,
    destLon: Double,
    modifier: Modifier = Modifier,
    routePoints: List<Pair<Double, Double>> = emptyList(),
    isInteractive: Boolean = true,
    repartidorLabel: String = "Tu ubicación"
) {
    val context = LocalContext.current
    val css = remember { LeafletAssets.getCss(context) }
    val js = remember { LeafletAssets.getJs(context) }
    var listo by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // La página no depende de datos que cambien: se arma una vez con el destino inicial.
    val html = remember {
        """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <style>
            $css
            html, body, #map { width: 100%; height: 100%; margin: 0; padding: 0; background: #DDE7EE; overflow: hidden; }
            .pin { display: flex; align-items: center; justify-content: center; }
            .leaflet-control-attribution { display: none !important; }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <script>$js</script>
          <script>
            var repIcon = L.divIcon({ className: 'pin', iconSize: [40, 40], iconAnchor: [20, 20], html:
              '<svg width="40" height="40" viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg">' +
              '<circle cx="20" cy="20" r="18" fill="#0284C7" stroke="#FFFFFF" stroke-width="3"/>' +
              '<path d="M20 10C16 16 13 20 13 24A7 7 0 0 0 27 24C27 20 24 16 20 10Z" fill="#FFFFFF"/></svg>' });
            var destIcon = L.divIcon({ className: 'pin', iconSize: [42, 48], iconAnchor: [21, 46], html:
              '<svg width="42" height="48" viewBox="0 0 42 48" xmlns="http://www.w3.org/2000/svg">' +
              '<path d="M21 2C11.61 2 4 9.61 4 19C4 31.75 21 46 21 46S38 31.75 38 19C38 9.61 30.39 2 21 2Z" fill="#10B981" stroke="#FFFFFF" stroke-width="2.5"/>' +
              '<circle cx="21" cy="19" r="6.5" fill="#FFFFFF"/></svg>' });

            // Dentro de Compose el WebView arranca con altura 0 y "height: 100%" deja el mapa en
            // 0 px: se fija la altura real en píxeles y Leaflet recalcula su tamaño (igual que OsmMapView).
            function ajustarTamano() {
              var div = document.getElementById('map');
              var h = window.innerHeight;
              div.style.height = (h && h > 50 ? h : 400) + 'px';
              if (typeof map !== 'undefined' && map) map.invalidateSize(true);
            }
            ajustarTamano();

            var map = L.map('map', {
              center: [$destLat, $destLon], zoom: 15, zoomControl: false, attributionControl: false,
              dragging: $isInteractive, touchZoom: $isInteractive, doubleClickZoom: $isInteractive, scrollWheelZoom: $isInteractive
            });
            L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);
            if (window.ResizeObserver) new ResizeObserver(ajustarTamano).observe(document.body);
            window.addEventListener('resize', ajustarTamano);
            [100, 350, 800].forEach(function (ms) { setTimeout(ajustarTamano, ms); });

            var markerDest = L.marker([$destLat, $destLon], { icon: destIcon }).addTo(map).bindPopup('<b>Destino de entrega</b>');
            var markerRep = null, linea = null, encuadrado = false, rutaPrevia = '';

            // Llamada desde Kotlin en cada cambio. ruta = [[lat,lon],...] o [] (línea recta punteada).
            function actualizar(repLat, repLon, destLat, destLon, ruta, etiqueta) {
              ajustarTamano();
              markerDest.setLatLng([destLat, destLon]);
              if (repLat !== null) {
                if (!markerRep) markerRep = L.marker([repLat, repLon], { icon: repIcon }).addTo(map);
                markerRep.setLatLng([repLat, repLon]).bindPopup('<b>' + etiqueta + '</b>');
              }
              var puntos = ruta.length >= 2 ? ruta : (repLat !== null ? [[repLat, repLon], [destLat, destLon]] : []);
              var firma = JSON.stringify(puntos.length >= 2 ? [puntos[0], puntos[puntos.length - 1], puntos.length] : []);
              if (linea) { map.removeLayer(linea); linea = null; }
              if (puntos.length >= 2) {
                linea = L.polyline(puntos, ruta.length >= 2
                  ? { color: '#0284C7', weight: 6, opacity: 0.85, lineJoin: 'round' }
                  : { color: '#0284C7', weight: 4, opacity: 0.7, dashArray: '8, 8' }).addTo(map);
              }
              // Encuadra al tener por primera vez repartidor + destino, o si cambió la ruta;
              // si no, respeta el zoom/arrastre del usuario.
              if (repLat !== null && (!encuadrado || (ruta.length >= 2 && firma !== rutaPrevia))) {
                var b = linea ? linea.getBounds() : L.latLngBounds([[repLat, repLon], [destLat, destLon]]);
                if (b.isValid()) map.fitBounds(b, { padding: [70, 70], maxZoom: 16 });
                encuadrado = true;
              }
              if (ruta.length >= 2) rutaPrevia = firma;
            }
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    val rutaJson = remember(routePoints) {
        routePoints.joinToString(prefix = "[", postfix = "]") { "[${it.first},${it.second}]" }
    }
    val etiquetaJs = remember(repartidorLabel) { repartidorLabel.replace("\\", "").replace("'", "\\'") }

    LaunchedEffect(listo, originLat, originLon, destLat, destLon, rutaJson, etiquetaJs) {
        if (!listo) return@LaunchedEffect
        val rep = if (originLat != null && originLon != null) "$originLat, $originLon" else "null, null"
        webViewRef?.evaluateJavascript("actualizar($rep, $destLat, $destLon, $rutaJson, '$etiquetaJs');", null)
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = false
                    settings.useWideViewPort = false
                    // La política de tiles de OpenStreetMap exige identificar la app.
                    settings.userAgentString = "HToGo-App/1.0 (Android; info@h2togo.mx) AppleWebKit/537.36"
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            Log.d("OsmRouteMap", "[JS] ${consoleMessage?.message()}")
                            return true
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            listo = true
                        }
                    }
                    loadDataWithBaseURL("https://openstreetmap.org", html, "text/html", "UTF-8", null)
                    webViewRef = this
                }
            }
        )
    }
}
