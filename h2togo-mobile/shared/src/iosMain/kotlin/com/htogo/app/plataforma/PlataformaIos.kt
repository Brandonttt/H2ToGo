package com.htogo.app.plataforma

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Piezas de interfaz de iOS. Fase 3: los mapas son un marcador de posición; en la fase 4 se
 * cargan con WKWebView usando el mismo HTML de Leaflet que Android.
 */
object UiPlataformaIos : UiPlataforma {

    @Composable
    override fun MapaSelector(
        latitude: Double, longitude: Double, modifier: Modifier, heightDp: Int, zoom: Int,
        isInteractive: Boolean, isDraggablePin: Boolean, onLocationChange: ((Double, Double) -> Unit)?
    ) = MapaPendiente(modifier.fillMaxWidth().height(heightDp.dp))

    @Composable
    override fun MapaRuta(
        originLat: Double?, originLon: Double?, destLat: Double, destLon: Double, modifier: Modifier,
        routePoints: List<Pair<Double, Double>>, isInteractive: Boolean, repartidorLabel: String,
        seguirRepartidor: Boolean, avisoSinUbicacion: String?, estadoPedido: String?
    ) = MapaPendiente(modifier)

    @Composable
    private fun MapaPendiente(modifier: Modifier) {
        Box(modifier.background(Color(0xFFDDE7EE)), contentAlignment = Alignment.Center) {
            Text("Mapa en iOS: siguiente fase", color = Color(0xFF64748B), fontSize = 13.sp)
        }
    }
}

/** Servicios de iOS (GPS, geocodificador, navegación externa): se completan en la fase 4. */
object ServiciosIos : ServiciosPlataforma {
    override val posicionEntrega = kotlinx.coroutines.flow.MutableStateFlow<com.htogo.app.data.location.Posicion?>(null)
}
