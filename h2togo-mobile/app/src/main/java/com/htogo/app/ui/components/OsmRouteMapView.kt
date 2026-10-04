package com.htogo.app.ui.components

import android.annotation.SuppressLint
import android.content.Context
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

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun OsmRouteMapView(
    originLat: Double,
    originLon: Double,
    destLat: Double,
    destLon: Double,
    modifier: Modifier = Modifier,
    routePoints: List<Pair<Double, Double>> = emptyList(),
    isInteractive: Boolean = true
) {
    val context = LocalContext.current
    val css = remember { LeafletAssets.getCss(context) }
    val js = remember { LeafletAssets.getJs(context) }

    val routeCoordsJson = remember(routePoints, originLat, originLon, destLat, destLon) {
        if (routePoints.size >= 2) {
            routePoints.joinToString(prefix = "[", postfix = "]") { "[${it.first}, ${it.second}]" }
        } else {
            "[[$originLat, $originLon], [$destLat, $destLon]]"
        }
    }

    val isDashed = routePoints.size < 2

    val html = remember(originLat, originLon, destLat, destLon, routeCoordsJson, isDashed) {
        """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <style>
            $css

            html, body {
              width: 100%;
              height: 100%;
              margin: 0;
              padding: 0;
              background-color: #DDE7EE;
              overflow: hidden;
            }
            #map {
              width: 100%;
              height: 100%;
              background-color: #DDE7EE;
            }
            .custom-repartidor-pin {
              display: flex;
              align-items: center;
              justify-content: center;
            }
            .custom-destino-pin {
              display: flex;
              align-items: center;
              justify-content: center;
            }
            .leaflet-control-attribution {
              display: none !important;
            }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <script>
            $js
          </script>
          <script>
            var map = null;
            var originLat = $originLat;
            var originLon = $originLon;
            var destLat = $destLat;
            var destLon = $destLon;
            var routeCoords = $routeCoordsJson;
            var isDashed = $isDashed;

            var repartidorSvg = '<svg width="40" height="40" viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">' +
              '<circle cx="20" cy="20" r="18" fill="#0284C7" stroke="#FFFFFF" stroke-width="3" filter="drop-shadow(0 2px 4px rgba(0,0,0,0.3))"/>' +
              '<path d="M20 10C16 16 13 20 13 24A7 7 0 0 0 27 24C27 20 24 16 20 10Z" fill="#FFFFFF"/>' +
            '</svg>';

            var destinoSvg = '<svg width="42" height="48" viewBox="0 0 42 48" fill="none" xmlns="http://www.w3.org/2000/svg">' +
              '<path d="M21 2C11.61 2 4 9.61 4 19C4 31.75 21 46 21 46S38 31.75 38 19C38 9.61 30.39 2 21 2Z" fill="#10B981" stroke="#FFFFFF" stroke-width="2.5"/>' +
              '<circle cx="21" cy="19" r="6.5" fill="#FFFFFF"/>' +
            '</svg>';

            var repIcon = L.divIcon({
              className: 'custom-repartidor-pin',
              html: repartidorSvg,
              iconSize: [40, 40],
              iconAnchor: [20, 20]
            });

            var destIcon = L.divIcon({
              className: 'custom-destino-pin',
              html: destinoSvg,
              iconSize: [42, 48],
              iconAnchor: [21, 46]
            });

            function initMap() {
              try {
                if (map) return;
                map = L.map('map', {
                  center: [originLat, originLon],
                  zoom: 14,
                  zoomControl: false,
                  dragging: $isInteractive,
                  touchZoom: $isInteractive,
                  doubleClickZoom: $isInteractive,
                  scrollWheelZoom: $isInteractive,
                  attributionControl: false
                });

                L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                  maxZoom: 19,
                  attribution: '&copy; OpenStreetMap'
                }).addTo(map);

                var markerRep = L.marker([originLat, originLon], { icon: repIcon }).addTo(map);
                var markerDest = L.marker([destLat, destLon], { icon: destIcon }).addTo(map);

                markerRep.bindPopup("<b>Tu ubicación (Repartidor)</b>");
                markerDest.bindPopup("<b>Destino de entrega</b>");

                var polylineOptions = {
                  color: '#0284C7',
                  weight: 6,
                  opacity: 0.85,
                  lineJoin: 'round'
                };

                if (isDashed) {
                  polylineOptions.dashArray = '8, 8';
                  polylineOptions.weight = 4;
                }

                var polyline = L.polyline(routeCoords, polylineOptions).addTo(map);

                var bounds = polyline.getBounds();
                if (bounds.isValid()) {
                  map.fitBounds(bounds, {
                    padding: [70, 70],
                    maxZoom: 16
                  });
                }
              } catch (e) {
                console.error("Error initMap: " + e);
              }
            }

            window.addEventListener('DOMContentLoaded', initMap);
            setTimeout(initMap, 200);
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            Log.d("OsmRouteMap", "[JS] ${consoleMessage?.message()}")
                            return true
                        }
                    }

                    webViewClient = object : WebViewClient() {}
                    loadDataWithBaseURL("https://openstreetmap.org", html, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL("https://openstreetmap.org", html, "text/html", "UTF-8", null)
            }
        )
    }
}
