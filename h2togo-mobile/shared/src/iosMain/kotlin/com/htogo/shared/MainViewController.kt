package com.htogo.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.htogo.app.data.notificaciones.CanalNotificaciones
import com.htogo.app.plataforma.NotificacionesIos
import com.htogo.app.plataforma.Plataforma
import com.htogo.app.plataforma.ServiciosIos
import com.htogo.app.plataforma.UiPlataformaIos
import com.htogo.shared.data.API_AZURE

/**
 * Punto de entrada que usa iosApp/ContenedorCompose.swift. [barraEstadoClara] recibe true cuando
 * la pantalla pide íconos claros en la barra de estado (encabezado oscuro).
 */
fun MainViewController(baseUrl: String = API_AZURE, barraEstadoClara: (Boolean) -> Unit = {}) = ComposeUIViewController {
    Plataforma.servicios = ServiciosIos
    Plataforma.barraEstadoClara = barraEstadoClara
    App(UiPlataformaIos, baseUrl) { alcance ->
        // Avisos del backend por WebSocket mientras la app está abierta (sin APNs).
        NotificacionesIos.preparar()
        CanalNotificaciones.iniciar(alcance, NotificacionesIos::mostrar)
    }
}
