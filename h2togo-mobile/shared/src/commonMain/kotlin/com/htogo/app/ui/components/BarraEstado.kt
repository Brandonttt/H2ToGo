package com.htogo.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.htogo.app.plataforma.Plataforma

/** Pantallas en pantalla que pidieron íconos claros; un contador evita parpadeos al navegar. */
private var pantallasClaras = 0

/**
 * Íconos claros en la barra de estado (hora, batería) mientras esta pantalla esté visible: para
 * los encabezados oscuros que llegan hasta arriba. Al salir vuelven a ser oscuros.
 */
@Composable
fun BarraEstadoClara() {
    DisposableEffect(Unit) {
        pantallasClaras++
        Plataforma.barraEstadoClara(true)
        onDispose {
            pantallasClaras--
            Plataforma.barraEstadoClara(pantallasClaras > 0)
        }
    }
}
