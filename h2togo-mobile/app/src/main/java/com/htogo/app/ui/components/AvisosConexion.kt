package com.htogo.app.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** true mientras el teléfono tiene una red con internet (RN-029). */
@Composable
fun rememberHayConexion(): State<Boolean> {
    val context = LocalContext.current
    val cm = remember { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    val estado = remember {
        mutableStateOf(
            cm.getNetworkCapabilities(cm.activeNetwork)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        )
    }
    DisposableEffect(cm) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { estado.value = true }
            override fun onLost(network: Network) {
                estado.value = cm.activeNetwork != null && cm.activeNetwork != network
            }
        }
        cm.registerDefaultNetworkCallback(callback)
        onDispose { cm.unregisterNetworkCallback(callback) }
    }
    return estado
}

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
        color = Color(0xFFFEF3C7),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, null, tint = Color(0xFFB45309), modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(texto, fontSize = 12.sp, color = Color(0xFF92400E), fontWeight = FontWeight.Medium)
        }
    }
}
