package com.htogo.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.htogo.app.plataforma.LocalUiPlataforma
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** true mientras el teléfono tiene una red con internet (RN-029); lo resuelve cada plataforma. */
@Composable
fun rememberHayConexion(): State<Boolean> = LocalUiPlataforma.current.rememberHayConexion()

/**
 * Franja superior de "Sin conexión" para toda la app (RN-029): los mapas y datos quedan con lo
 * último cargado y las acciones fallan hasta recuperar la red.
 */
@Composable
fun SinConexionBanner(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = visible, modifier = modifier) {
        Surface(color = Color(0xFF334155), modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.CloudOff, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Sin conexión · se muestra la última información conocida",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/** Aviso flotante sobre un mapa (GPS sin señal, ubicación desactualizada…). */
@Composable
fun AvisoMapa(texto: String, modifier: Modifier = Modifier, icono: ImageVector = Icons.Filled.GpsNotFixed) {
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = Color(0xFFFFFBEB),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.55f)),
        shadowElevation = 4.dp,
        modifier = modifier
    ) {
        Row(
            Modifier.padding(start = 6.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = Color(0xFFFEF3C7),
                modifier = Modifier.size(20.dp)
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Icon(icono, null, tint = Color(0xFFD97706), modifier = Modifier.size(13.dp))
                }
            }
            Spacer(Modifier.width(7.dp))
            Text(
                texto,
                fontSize = 11.sp,
                color = Color(0xFF92400E),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
