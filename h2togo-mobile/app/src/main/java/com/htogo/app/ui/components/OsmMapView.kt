package com.htogo.app.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class OsmBridge(private val onLocationChange: (Double, Double) -> Unit) {
    @JavascriptInterface
    fun onLocationChanged(lat: Double, lon: Double) {
        onLocationChange(lat, lon)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OsmMapView(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier,
    zoom: Int = 15,
    isInteractive: Boolean = true,
    isDraggablePin: Boolean = false,
    onLocationChange: ((Double, Double) -> Unit)? = null
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }

    val updatedOnLocationChange by rememberUpdatedState(onLocationChange)

    val html = remember(latitude, longitude, zoom, isInteractive, isDraggablePin) {
        """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
          <style>
            html, body, #map {
              height: 100%;
              width: 100%;
              margin: 0;
              padding: 0;
              background: #f1f5f9;
            }
            .leaflet-control-attribution {
              font-size: 8px !important;
              background: rgba(255,255,255,0.7) !important;
            }
            .custom-pin {
              display: flex;
              align-items: center;
              justify-content: center;
            }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
          <script>
            let map, marker;
            const pinSvg = `<svg width="36" height="36" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z" fill="#0284C7" stroke="#FFFFFF" stroke-width="1.5"/>
              <circle cx="12" cy="9" r="3" fill="#FFFFFF"/>
            </svg>`;

            const customIcon = L.divIcon({
              className: 'custom-pin',
              html: pinSvg,
              iconSize: [36, 36],
              iconAnchor: [18, 36]
            });

            function initMap() {
              map = L.map('map', {
                center: [$latitude, $longitude],
                zoom: $zoom,
                zoomControl: $isInteractive,
                dragging: $isInteractive,
                touchZoom: $isInteractive,
                doubleClickZoom: $isInteractive,
                scrollWheelZoom: $isInteractive,
                attributionControl: true
              });

              L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                maxZoom: 19,
                attribution: '© OpenStreetMap'
              }).addTo(map);

              marker = L.marker([$latitude, $longitude], {
                icon: customIcon,
                draggable: $isDraggablePin
              }).addTo(map);

              if ($isDraggablePin) {
                marker.on('dragend', function() {
                  const pos = marker.getLatLng();
                  if (window.AndroidBridge && window.AndroidBridge.onLocationChanged) {
                    window.AndroidBridge.onLocationChanged(pos.lat, pos.lng);
                  }
                });
              }

              if ($isInteractive && $isDraggablePin) {
                map.on('click', function(e) {
                  marker.setLatLng(e.latlng);
                  if (window.AndroidBridge && window.AndroidBridge.onLocationChanged) {
                    window.AndroidBridge.onLocationChanged(e.latlng.lat, e.latlng.lng);
                  }
                });
              }
            }

            window.onload = initMap;

            function updatePosition(lat, lon) {
              if (map && marker) {
                map.panTo([lat, lon]);
                marker.setLatLng([lat, lon]);
              }
            }
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    LaunchedEffect(latitude, longitude) {
        if (isMapLoaded) {
            webViewInstance?.evaluateJavascript("updatePosition($latitude, $longitude);", null)
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { context: Context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true

                    addJavascriptInterface(OsmBridge { lat, lon ->
                        updatedOnLocationChange?.invoke(lat, lon)
                    }, "AndroidBridge")

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isMapLoaded = true
                        }
                    }

                    setOnTouchListener { v, _ ->
                        if (isInteractive) {
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        }
                        false
                    }

                    loadDataWithBaseURL(
                        "https://tile.openstreetmap.org",
                        html,
                        "text/html",
                        "UTF-8",
                        null
                    )
                    webViewInstance = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
