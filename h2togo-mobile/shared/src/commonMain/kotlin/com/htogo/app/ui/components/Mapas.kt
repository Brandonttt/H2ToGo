package com.htogo.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.htogo.app.plataforma.LocalUiPlataforma

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
) = LocalUiPlataforma.current.MapaSelector(
    latitude, longitude, modifier, heightDp, zoom, isInteractive, isDraggablePin, onLocationChange
)

/**
 * Mapa con el repartidor, el destino y la ruta (seguimiento del cliente y navegación del
 * repartidor). La implementación es de cada plataforma (Leaflet en WebView / WKWebView).
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
    estadoPedido: String? = null
) = LocalUiPlataforma.current.MapaRuta(
    originLat, originLon, destLat, destLon, modifier, routePoints, isInteractive, repartidorLabel,
    seguirRepartidor, avisoSinUbicacion, estadoPedido
)
