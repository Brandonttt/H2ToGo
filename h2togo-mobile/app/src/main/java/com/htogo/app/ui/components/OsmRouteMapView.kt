package com.htogo.app.ui.components

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Mapa Leaflet con el repartidor, el destino y la ruta. La página se carga una sola vez y los
 * cambios (posición en vivo, ruta recalculada) se aplican con JavaScript, para que el mapa no
 * parpadee ni pierda el zoom en cada actualización del GPS.
 *
 * El marcador del repartidor se desliza entre posiciones del GPS y lleva una flecha con su rumbo;
 * con [seguirRepartidor] la cámara lo sigue (rumbo arriba o norte arriba) hasta que el usuario
 * arrastra el mapa, y un botón lo vuelve a centrar.
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
    repartidorLabel: String = "Tu ubicación",
    /** Navegación del repartidor: la cámara lo sigue y el mapa gira con su rumbo (brújula para alternar). */
    seguirRepartidor: Boolean = false,
    /** Aviso mientras no hay posición; null para no mostrarlo (p. ej. el cliente tiene el suyo). */
    avisoSinUbicacion: String? = "Buscando señal GPS..."
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
            html, body { width: 100%; height: 100%; margin: 0; padding: 0; background: #DDE7EE; overflow: hidden; }
            #wrap { position: absolute; inset: 0; overflow: hidden; }
            #map { position: absolute; left: 0; top: 0; width: 100%; height: 100%; background: #DDE7EE;
                   transform-origin: 50% 50%; transition: transform .6s ease-out; }
            .pin { display: flex; align-items: center; justify-content: center; }
            .rep { position: relative; width: 48px; height: 48px; }
            .rep .rumbo { position: absolute; inset: 0; transition: transform .4s linear; }
            .ctl { position: absolute; right: 12px; width: 44px; height: 44px; border-radius: 22px; background: #fff;
                   box-shadow: 0 2px 8px rgba(15,23,42,.25); display: flex; align-items: center; justify-content: center;
                   z-index: 1000; -webkit-tap-highlight-color: transparent; }
            #brujula { top: 150px; }
            #brujula svg { transition: transform .6s ease-out; }
            #centrar { top: 202px; display: none; color: #0284C7; font: 600 12px sans-serif; width: auto; padding: 0 14px; }
            .leaflet-control-attribution { display: none !important; }
          </style>
        </head>
        <body>
          <div id="wrap"><div id="map"></div></div>
          <div id="brujula" class="ctl" style="display:none" title="Norte / rumbo">
            <svg width="28" height="28" viewBox="0 0 28 28"><text x="14" y="7.5" text-anchor="middle" font-size="7.5" font-weight="700" font-family="sans-serif" fill="#EF4444">N</text>
              <path d="M14 9 L18 15.5 L14 14 L10 15.5 Z" fill="#EF4444"/><path d="M14 25 L10 15.5 L14 17 L18 15.5 Z" fill="#94A3B8"/></svg>
          </div>
          <div id="centrar" class="ctl">◎ Centrar</div>
          <script>$js</script>
          <script>
            // Marcador del repartidor: círculo con gota y una flecha exterior que apunta a su rumbo.
            var repIcon = L.divIcon({ className: 'pin', iconSize: [48, 48], iconAnchor: [24, 24], html:
              '<div class="rep"><svg class="rumbo" width="48" height="48" viewBox="0 0 48 48">' +
              '<path d="M24 0 L30 9 L18 9 Z" fill="#0284C7" stroke="#FFFFFF" stroke-width="1.5"/></svg>' +
              '<svg width="48" height="48" viewBox="0 0 48 48" style="position:absolute;inset:0">' +
              '<circle cx="24" cy="24" r="16" fill="#0284C7" stroke="#FFFFFF" stroke-width="3"/>' +
              '<path d="M24 15C20.6 20 18 23.4 18 26.6A6 6 0 0 0 30 26.6C30 23.4 27.4 20 24 15Z" fill="#FFFFFF"/></svg></div>' });
            var destIcon = L.divIcon({ className: 'pin', iconSize: [42, 48], iconAnchor: [21, 46], html:
              '<svg width="42" height="48" viewBox="0 0 42 48" xmlns="http://www.w3.org/2000/svg">' +
              '<path d="M21 2C11.61 2 4 9.61 4 19C4 31.75 21 46 21 46S38 31.75 38 19C38 9.61 30.39 2 21 2Z" fill="#10B981" stroke="#FFFFFF" stroke-width="2.5"/>' +
              '<circle cx="21" cy="19" r="6.5" fill="#FFFFFF"/></svg>' });

            // Dentro de Compose el WebView arranca con altura 0 y "height: 100%" deja el mapa en
            // 0 px: se fija la altura real en píxeles y Leaflet recalcula su tamaño (igual que OsmMapView).
            // Al girar (rumbo arriba) el contenedor se agranda a la diagonal para no dejar esquinas vacías.
            var SEGUIR = $seguirRepartidor, girado = false;
            function ajustarTamano() {
              var div = document.getElementById('map');
              var w = window.innerWidth || 360, h = window.innerHeight;
              h = (h && h > 50 ? h : 400);
              var lado = girado ? Math.ceil(Math.sqrt(w * w + h * h)) : 0;
              div.style.width = (girado ? lado : w) + 'px';
              div.style.height = (girado ? lado : h) + 'px';
              div.style.left = (girado ? (w - lado) / 2 : 0) + 'px';
              div.style.top = (girado ? (h - lado) / 2 : 0) + 'px';
              if (typeof map !== 'undefined' && map) {
                map.invalidateSize({ pan: false });
                if (SEGUIR && siguiendo && repPos) map.setView(repPos, map.getZoom(), { animate: false });
              }
            }
            ajustarTamano();

            var map = L.map('map', {
              center: [$destLat, $destLon], zoom: 15, zoomControl: false, attributionControl: false,
              dragging: $isInteractive, touchZoom: $isInteractive, doubleClickZoom: $isInteractive, scrollWheelZoom: $isInteractive
            });
            // Capa estándar limpia (Google Maps Road) que funciona siempre sin bloqueos ni marcas de agua en WebViews Android
            var googleLayer = L.tileLayer('https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', { maxZoom: 20 });
            // Capa OpenStreetMap Humanitaria (HOT) con subdominios públicos libres
            var osmLayer = L.tileLayer('https://{s}.tile.openstreetmap.fr/hot/{z}/{x}/{y}.png', {
              subdomains: 'abc',
              maxZoom: 19
            });
            var currentLayer = googleLayer;
            currentLayer.addTo(map);

            function setCapa(tipo) {
              if (currentLayer) map.removeLayer(currentLayer);
              currentLayer = (tipo === 'osm') ? osmLayer : googleLayer;
              currentLayer.addTo(map);
            }

            if (window.ResizeObserver) new ResizeObserver(ajustarTamano).observe(document.body);
            window.addEventListener('resize', ajustarTamano);
            [100, 350, 800].forEach(function (ms) { setTimeout(ajustarTamano, ms); });

            var markerDest = L.marker([$destLat, $destLon], { icon: destIcon }).addTo(map).bindPopup('<b>Destino de entrega</b>');
            var markerRep = null, linea = null, encuadrado = false, rutaPrevia = '';
            var rutaActual = [], destino = [$destLat, $destLon];
            var repPos = null, rumbo = 0, anim = null;
            var siguiendo = SEGUIR, rumboArriba = true, anguloMapa = 0;
            var brujula = document.getElementById('brujula'), centrar = document.getElementById('centrar');
            if (SEGUIR) brujula.style.display = 'flex';

            function distM(a, b) {
              var R = 6371000, r = Math.PI / 180;
              var dLat = (b[0] - a[0]) * r, dLon = (b[1] - a[1]) * r;
              var x = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(a[0] * r) * Math.cos(b[0] * r) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
              return 2 * R * Math.asin(Math.sqrt(x));
            }
            function rumboEntre(a, b) {
              var r = Math.PI / 180, y = Math.sin((b[1] - a[1]) * r) * Math.cos(b[0] * r);
              var x = Math.cos(a[0] * r) * Math.sin(b[0] * r) - Math.sin(a[0] * r) * Math.cos(b[0] * r) * Math.cos((b[1] - a[1]) * r);
              return (Math.atan2(y, x) / r + 360) % 360;
            }

            // Línea: desde la posición actual por el resto de la ruta (lo ya recorrido desaparece).
            function dibujarLinea() {
              var puntos = [];
              if (rutaActual.length >= 2) {
                puntos = rutaActual;
                if (repPos) {
                  var mejor = 0, dMin = Infinity;
                  for (var i = 0; i < rutaActual.length; i++) {
                    var d = distM(repPos, rutaActual[i]);
                    if (d < dMin) { dMin = d; mejor = i; }
                  }
                  puntos = [repPos].concat(rutaActual.slice(mejor + 1));
                  if (puntos.length < 2) puntos = [repPos, destino];
                }
              } else if (repPos) {
                puntos = [repPos, destino];
              }
              var estilo = rutaActual.length >= 2
                ? { color: '#0284C7', weight: 6, opacity: 0.85, lineJoin: 'round' }
                : { color: '#0284C7', weight: 4, opacity: 0.7, dashArray: '8, 8' };
              if (puntos.length < 2) { if (linea) { map.removeLayer(linea); linea = null; } return; }
              if (linea && linea._rutaReal === (rutaActual.length >= 2)) { linea.setLatLngs(puntos); return; }
              if (linea) map.removeLayer(linea);
              linea = L.polyline(puntos, estilo).addTo(map);
              linea._rutaReal = rutaActual.length >= 2;
            }

            // Gira el mapa (rumbo arriba) solo mientras sigue al repartidor; si no, norte arriba.
            function aplicarRotacion() {
              var objetivo = (SEGUIR && siguiendo && rumboArriba) ? rumbo : 0;
              var delta = ((objetivo - anguloMapa) % 360 + 540) % 360 - 180; // giro más corto
              anguloMapa += delta;
              var debeGirar = objetivo !== 0 || Math.abs(anguloMapa % 360) > 0.5;
              if (debeGirar !== girado) { girado = debeGirar; ajustarTamano(); if (repPos && siguiendo) map.setView(repPos, map.getZoom(), { animate: false }); }
              document.getElementById('map').style.transform = 'rotate(' + (-anguloMapa) + 'deg)';
              brujula.querySelector('svg').style.transform = 'rotate(' + (-anguloMapa) + 'deg)';
            }
            function ponerRumboFlecha() {
              var el = markerRep && markerRep.getElement();
              var f = el && el.querySelector('.rumbo');
              if (f) f.style.transform = 'rotate(' + rumbo + 'deg)';
            }
            // Respeta el zoom que elija el usuario; el acercamiento a 17 es solo al empezar o al centrar.
            var zoomFijado = false;
            function seguirA(p) {
              if (!(SEGUIR && siguiendo)) return;
              map.setView(p, zoomFijado ? map.getZoom() : 17, { animate: false });
              zoomFijado = true;
            }

            // Desliza el marcador hasta la nueva posición (~1 s) con la cámara detrás.
            function moverA(destinoRep) {
              if (!repPos || distM(repPos, destinoRep) > 2000) {
                repPos = destinoRep; markerRep.setLatLng(repPos); dibujarLinea(); seguirA(repPos); return;
              }
              if (distM(repPos, destinoRep) > 4) {
                rumbo = rumboEntre(repPos, destinoRep); ponerRumboFlecha(); aplicarRotacion();
              }
              if (anim) cancelAnimationFrame(anim);
              var desde = repPos.slice(), t0 = performance.now(), dur = 1000;
              function paso(t) {
                var k = Math.min(1, (t - t0) / dur);
                repPos = [desde[0] + (destinoRep[0] - desde[0]) * k, desde[1] + (destinoRep[1] - desde[1]) * k];
                markerRep.setLatLng(repPos);
                dibujarLinea();
                seguirA(repPos);
                anim = k < 1 ? requestAnimationFrame(paso) : null;
              }
              anim = requestAnimationFrame(paso);
            }

            // El usuario explora el mapa: se deja de seguir y vuelve norte arriba.
            map.on('dragstart', function () {
              if (!SEGUIR || !siguiendo) return;
              siguiendo = false; centrar.style.display = 'flex'; aplicarRotacion();
            });
            centrar.addEventListener('click', function () {
              siguiendo = true; centrar.style.display = 'none'; aplicarRotacion();
              if (repPos) map.setView(repPos, Math.max(map.getZoom(), 16));
            });
            brujula.addEventListener('click', function () {
              rumboArriba = !rumboArriba;
              if (!siguiendo) { siguiendo = true; centrar.style.display = 'none'; if (repPos) map.setView(repPos, Math.max(map.getZoom(), 16)); }
              aplicarRotacion();
            });

            // Llamada desde Kotlin en cada cambio. ruta = [[lat,lon],...] o [] (línea recta punteada).
            function actualizar(repLat, repLon, destLat, destLon, ruta, etiqueta) {
              ajustarTamano();
              destino = [destLat, destLon];
              markerDest.setLatLng(destino);
              var firma = JSON.stringify(ruta.length >= 2 ? [ruta[0], ruta[ruta.length - 1], ruta.length] : []);
              rutaActual = ruta;
              if (repLat !== null) {
                if (!markerRep) {
                  markerRep = L.marker([repLat, repLon], { icon: repIcon, zIndexOffset: 1000 }).addTo(map);
                  repPos = null;
                }
                markerRep.bindPopup('<b>' + etiqueta + '</b>');
                moverA([repLat, repLon]);
              } else {
                dibujarLinea();
              }
              // Sin seguimiento: encuadra repartidor + destino la primera vez o si cambió la ruta.
              if (!(SEGUIR && siguiendo) && repLat !== null && (!encuadrado || (ruta.length >= 2 && firma !== rutaPrevia))) {
                var b = rutaActual.length >= 2 ? L.latLngBounds(rutaActual) : L.latLngBounds([[repLat, repLon], destino]);
                b.extend([repLat, repLon]);
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
    var esGoogleMaps by remember { mutableStateOf(false) }

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

        // CU-017 Flujo S1: Indicador de búsqueda de señal GPS
        if (avisoSinUbicacion != null && (originLat == null || originLon == null)) {
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.TopCenter)
                    .padding(top = 96.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        color = androidx.compose.ui.graphics.Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    androidx.compose.material3.Text(
                        avisoSinUbicacion,
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 11.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                }
            }
        }

        var capaOsm by remember { mutableStateOf(false) }

        // CU-017: Selector de capas de mapa (Google Maps / OSM)
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.95f),
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.TopEnd)
                .padding(top = 96.dp, end = 12.dp)
                .clickable {
                    capaOsm = !capaOsm
                    webViewRef?.evaluateJavascript(
                        "setCapa('${if (capaOsm) "osm" else "google"}');",
                        null
                    )
                }
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                androidx.compose.material3.Text(
                    if (capaOsm) "🗺️ OSM" else "🗺️ Google Maps",
                    fontSize = 11.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = if (capaOsm) androidx.compose.ui.graphics.Color(0xFF0284C7) else androidx.compose.ui.graphics.Color(0xFF1A73E8)
                )
            }
        }
    }
}
