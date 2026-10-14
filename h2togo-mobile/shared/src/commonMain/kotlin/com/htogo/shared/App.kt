package com.htogo.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.htogo.app.HToGoRaiz
import com.htogo.app.data.api.ApiClient
import com.htogo.app.plataforma.UiPlataforma
import com.htogo.shared.data.API_AZURE

/** Entrada de la app completa para iOS: fija el backend y arranca la raíz común. */
@Composable
fun App(ui: UiPlataforma, baseUrl: String = API_AZURE) {
    remember(baseUrl) { ApiClient.getInstance(baseUrl = baseUrl) } // primera inicialización
    HToGoRaiz(ui)
}
