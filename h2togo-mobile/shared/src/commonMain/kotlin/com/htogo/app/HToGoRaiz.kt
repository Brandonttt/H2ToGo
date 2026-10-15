package com.htogo.app

import androidx.compose.foundation.layout.Box
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import com.htogo.app.data.realtime.StompCliente
import com.htogo.app.plataforma.Aviso
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.htogo.app.navigation.HToGoNavHost
import com.htogo.app.plataforma.AvisosEnPantalla
import com.htogo.app.plataforma.LocalUiPlataforma
import com.htogo.app.plataforma.UiPlataforma
import com.htogo.app.ui.components.SinConexionBanner
import com.htogo.app.ui.components.rememberHayConexion
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme
import kotlinx.coroutines.delay

/**
 * Raíz de la app, común a Android e iOS: tema, navegación, franja de "sin conexión" y avisos.
 * Cada plataforma pasa su [ui] (mapas, red y permisos).
 */
@Composable
fun HToGoRaiz(ui: UiPlataforma) {
    CompositionLocalProvider(LocalUiPlataforma provides ui) {
        HToGoTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = HToGoColors.Background) {
                ReconectarAlVolver()
                val navController = rememberNavController()
                val hayConexion by rememberHayConexion()
                Box(Modifier.fillMaxSize()) {
                    HToGoNavHost(navController = navController)
                    SinConexionBanner(visible = !hayConexion, modifier = Modifier.align(Alignment.TopCenter))
                    AvisoFlotante(Modifier.align(Alignment.TopCenter))
                }
            }
        }
    }
}

/**
 * Al volver la app al frente, reconecta los WebSockets: mientras estuvo en segundo plano (iOS la
 * suspende) la conexión pudo morir sin que nadie lo notara y ya no llegarían los avisos.
 */
@Composable
private fun ReconectarAlVolver() {
    val dueno = LocalLifecycleOwner.current
    DisposableEffect(dueno) {
        var enSegundoPlano = false
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_STOP -> enSegundoPlano = true
                Lifecycle.Event.ON_START -> if (enSegundoPlano) {
                    enSegundoPlano = false
                    StompCliente.reconectarTodo()
                }
                else -> Unit
            }
        }
        dueno.lifecycle.addObserver(observador)
        onDispose { dueno.lifecycle.removeObserver(observador) }
    }
}

/**
 * Avisos de [mostrarAviso] (Android e iOS): tarjeta grande arriba de la pantalla que entra
 * deslizándose; se cierra sola (3 s, o 5 s si es larga) o al tocarla.
 */
@Composable
private fun AvisoFlotante(modifier: Modifier) {
    val aviso by AvisosEnPantalla.actual.collectAsState()
    // El último aviso sigue pintándose mientras sale con la animación.
    var ultimo by remember { mutableStateOf<Aviso?>(null) }
    SideEffect { aviso?.let { ultimo = it } }
    LaunchedEffect(aviso) {
        val a = aviso ?: return@LaunchedEffect
        delay(if (a.largo) 5_000 else 3_000)
        AvisosEnPantalla.limpiar(a)
    }
    AnimatedVisibility(
        visible = aviso != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        val a = aviso ?: ultimo ?: return@AnimatedVisibility
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF0F172A),
            shadowElevation = 10.dp,
            modifier = Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
                .clickable { AvisosEnPantalla.limpiar(a) }
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(HToGoColors.PrimaryLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Notifications, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(14.dp))
                Text(a.texto, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp)
            }
        }
    }
}
