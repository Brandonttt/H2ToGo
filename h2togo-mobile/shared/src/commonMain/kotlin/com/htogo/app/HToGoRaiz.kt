package com.htogo.app

import androidx.compose.foundation.layout.Box
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
                val focusManager = LocalFocusManager.current
                Box(Modifier.fillMaxSize().quitarFocoAlTocarFuera(focusManager)) {
                    HToGoNavHost(navController = navController)
                    SinConexionBanner(visible = !hayConexion, modifier = Modifier.align(Alignment.TopCenter))
                    AvisoFlotante(Modifier.align(Alignment.BottomCenter))
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
 * Avisos de [mostrarAviso] (Android e iOS): tarjeta compacta abajo, por encima de la barra de
 * pestañas y del teclado, para no tapar el encabezado; se cierra sola (3 s, o 5 s si es larga)
 * o al tocarla.
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
        enter = slideInVertically { it / 2 } + fadeIn(),
        exit = slideOutVertically { it / 2 } + fadeOut(),
        modifier = modifier
    ) {
        val a = aviso ?: ultimo ?: return@AnimatedVisibility
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = HToGoColors.Surface,
            border = BorderStroke(1.dp, HToGoColors.OutlineSoft),
            shadowElevation = 6.dp,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(start = 16.dp, end = 16.dp, bottom = 80.dp)
                .fillMaxWidth()
                .clickable { AvisosEnPantalla.limpiar(a) }
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, null, tint = HToGoColors.Primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    a.texto,
                    color = HToGoColors.TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Toque fuera de un campo de texto: quita el foco y con eso baja el teclado (en iOS los teclados
 * numéricos no tienen botón para cerrarse). Solo observa en el pase Final, sin consumir nada,
 * así que no afecta clics ni scroll. Los campos de texto y los botones consumen su toque en el
 * pase Main, por lo que aquí solo cuenta un toque que nadie consumió (fondo, tarjetas, textos):
 * tocar un campo sigue enfocándolo. Un arrastre (scroll) también se consume y no quita el foco.
 */
private fun Modifier.quitarFocoAlTocarFuera(focusManager: FocusManager): Modifier =
    pointerInput(focusManager) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            // Devuelve null si el toque se canceló o algún hijo lo consumió (o lo convirtió en arrastre).
            val up = waitForUpOrCancellation(pass = PointerEventPass.Final)
            if (up != null && !up.isConsumed) focusManager.clearFocus()
        }
    }
