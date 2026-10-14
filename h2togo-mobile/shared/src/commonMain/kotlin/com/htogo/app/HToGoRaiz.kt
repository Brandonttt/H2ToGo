package com.htogo.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
                val navController = rememberNavController()
                val hayConexion by rememberHayConexion()
                Box(Modifier.fillMaxSize()) {
                    HToGoNavHost(navController = navController)
                    SinConexionBanner(visible = !hayConexion, modifier = Modifier.align(Alignment.TopCenter))
                    AvisoFlotante(Modifier.align(Alignment.BottomCenter))
                }
            }
        }
    }
}

/** Avisos breves en plataformas sin Toast (iOS); en Android nunca hay pendientes. */
@Composable
private fun AvisoFlotante(modifier: Modifier) {
    val aviso by AvisosEnPantalla.actual.collectAsState()
    val texto = aviso ?: return
    LaunchedEffect(texto) {
        delay(3_000)
        AvisosEnPantalla.limpiar()
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E293B),
        modifier = modifier.navigationBarsPadding().padding(16.dp).fillMaxWidth()
    ) {
        Text(texto, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
    }
}
