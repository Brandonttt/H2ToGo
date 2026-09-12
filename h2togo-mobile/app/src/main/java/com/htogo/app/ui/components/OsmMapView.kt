package com.htogo.app.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

class OsmBridge(private val onLocationChange: (Double, Double) -> Unit) {
    @JavascriptInterface
    fun onLocationChanged(lat: Double, lon: Double) {
        onLocationChange(lat, lon)
    }
}

object LeafletAssets {
    private var cachedCss: String? = null
    private var cachedJs: String? = null

    fun getCss(context: Context): String {
        return cachedCss ?: run {
            try {
                context.assets.open("leaflet/leaflet.css").bufferedReader().use { it.readText() }
                    .also { cachedCss = it }
            } catch (e: Exception) {
                Log.e("OsmMapView", "Error reading leaflet.css: ${e.message}")
                ""
            }
        }
    }

    fun getJs(context: Context): String {
        return cachedJs ?: run {
            try {
                context.assets.open("leaflet/leaflet.js").bufferedReader().use { it.readText() }
                    .also { cachedJs = it }
            } catch (e: Exception) {
                Log.e("OsmMapView", "Error reading leaflet.js: ${e.message}")
                ""
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
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
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }

    val updatedOnLocationChange by rememberUpdatedState(onLocationChange)

    val css = remember { LeafletAssets.getCss(context) }
    val js = remember { LeafletAssets.getJs(context) }

    val html = remember(latitude, longitude, zoom, isInteractive, isDraggablePin, heightDp) {
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
              background-color: #f1f5f9;
            }
            #map {
              width: 100%;
              height: ${heightDp}px;
              min-height: ${heightDp}px;
              background-color: #f1f5f9;
            }
            .custom-pin {
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
            var marker = null;
            var initialLat = $latitude;
            var initialLon = $longitude;
            var initialZoom = $zoom;
            var isInteractive = $isInteractive;
            var isDraggable = $isDraggablePin;
            var fallbackHeight = $heightDp;

            function fixMapHeight() {
              var mapDiv = document.getElementById('map');
              if (!mapDiv) return;
              var h = window.innerHeight;
              if (h && h > 50) {
                mapDiv.style.height = h + 'px';
              } else {
                mapDiv.style.height = fallbackHeight + 'px';
              }
            }

            var pinSvg = '<svg width="38" height="38" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
              '<path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z" fill="#0284C7" stroke="#FFFFFF" stroke-width="1.8"/>' +
              '<circle cx="12" cy="9" r="3.2" fill="#FFFFFF"/>' +
            '</svg>';

            var customIcon = L.divIcon({
              className: 'custom-pin',
              html: pinSvg,
              iconSize: [38, 38],
              iconAnchor: [19, 38]
            });

            function initMap() {
              try {
                if (map) return;
                fixMapHeight();
                var mapDiv = document.getElementById('map');
                if (!mapDiv) return;

                map = L.map('map', {
                  center: [initialLat, initialLon],
                  zoom: initialZoom,
                  zoomControl: isInteractive,
                  dragging: isInteractive,
                  touchZoom: isInteractive,
                  doubleClickZoom: isInteractive,
                  scrollWheelZoom: isInteractive,
                  attributionControl: false
                });

                var tileLayer = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                  maxZoom: 19,
                  attribution: '&copy; OpenStreetMap'
                });

                tileLayer.on('tileload', function(e) {
                  console.log("OSM TILE LOADED: " + e.coords.z + "/" + e.coords.x + "/" + e.coords.y);
                });

                tileLayer.on('tileerror', function(e) {
                  console.error("OSM TILE ERROR: " + (e.tile ? e.tile.src : "unknown"));
                });

                tileLayer.addTo(map);

                marker = L.marker([initialLat, initialLon], {
                  icon: customIcon,
                  draggable: isDraggable
                }).addTo(map);

                if (isDraggable) {
                  marker.on('dragend', function() {
                    var pos = marker.getLatLng();
                    if (window.AndroidBridge && window.AndroidBridge.onLocationChanged) {
                      window.AndroidBridge.onLocationChanged(pos.lat, pos.lng);
                    }
                  });
                }

                if (isInteractive && isDraggable) {
                  map.on('click', function(e) {
                    marker.setLatLng(e.latlng);
                    if (window.AndroidBridge && window.AndroidBridge.onLocationChanged) {
                      window.AndroidBridge.onLocationChanged(e.latlng.lat, e.latlng.lng);
                    }
                  });
                }

                if (window.ResizeObserver) {
                  var ro = new ResizeObserver(function() {
                    fixMapHeight();
                    if (map) {
                      map.invalidateSize(true);
                      console.log("ResizeObserver: map size is " + map.getSize().x + "x" + map.getSize().y);
                    }
                  });
                  ro.observe(mapDiv);
                }

                setTimeout(function() { fixMapHeight(); if (map) map.invalidateSize(true); }, 100);
                setTimeout(function() { fixMapHeight(); if (map) map.invalidateSize(true); }, 350);
                setTimeout(function() { fixMapHeight(); if (map) map.invalidateSize(true); }, 800);
                console.log("OsmMapView Leaflet initialized at [" + initialLat + ", " + initialLon + "] size=" + map.getSize().x + "x" + map.getSize().y);
              } catch(e) {
                console.error("Error initMap: " + e.message);
              }
            }

            function updatePosition(lat, lon) {
              try {
                fixMapHeight();
                if (!map) {
                  initMap();
                }
                if (map && marker) {
                  map.setView([lat, lon], map.getZoom() || initialZoom);
                  marker.setLatLng([lat, lon]);
                  setTimeout(function() { fixMapHeight(); if (map) map.invalidateSize(true); }, 150);
                  console.log("OsmMapView position updated to [" + lat + ", " + lon + "] size=" + map.getSize().x + "x" + map.getSize().y);
                }
              } catch(e) {
                console.error("Error updatePosition: " + e.message);
              }
            }

            if (document.readyState === 'complete' || document.readyState === 'interactive') {
              initMap();
            } else {
              document.addEventListener('DOMContentLoaded', initMap);
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
            factory = { ctx: Context ->
                WebView(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = false
                    settings.useWideViewPort = false
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.userAgentString = "HToGo-App/1.0 (Android; info@h2togo.mx) AppleWebKit/537.36"

                    addJavascriptInterface(OsmBridge { lat, lon ->
                        updatedOnLocationChange?.invoke(lat, lon)
                    }, "AndroidBridge")

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            Log.d("OsmMapView", "[JS] ${consoleMessage?.message()} (line ${consoleMessage?.lineNumber()})")
                            return true
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isMapLoaded = true
                            view?.evaluateJavascript("fixMapHeight(); if (map) { map.invalidateSize(true); } updatePosition($latitude, $longitude);", null)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            Log.e("OsmMapView", "Resource error: ${error?.description} on ${request?.url}")
                        }
                    }

                    setOnTouchListener { v, event ->
                        if (isInteractive) {
                            when (event.actionMasked) {
                                MotionEvent.ACTION_DOWN,
                                MotionEvent.ACTION_MOVE,
                                MotionEvent.ACTION_POINTER_DOWN -> {
                                    v.parent?.requestDisallowInterceptTouchEvent(true)
                                }
                                MotionEvent.ACTION_UP,
                                MotionEvent.ACTION_CANCEL -> {
                                    v.parent?.requestDisallowInterceptTouchEvent(false)
                                }
                            }
                        }
                        false
                    }

                    loadDataWithBaseURL(
                        "https://tile.openstreetmap.org/",
                        html,
                        "text/html",
                        "UTF-8",
                        null
                    )
                    webViewInstance = this
                }
            },
            update = { webView ->
                if (isMapLoaded) {
                    webView.evaluateJavascript("updatePosition($latitude, $longitude);", null)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
