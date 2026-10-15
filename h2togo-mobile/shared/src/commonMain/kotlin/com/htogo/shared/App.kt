package com.htogo.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import com.htogo.app.HToGoRaiz
import com.htogo.app.data.api.ApiClient
import com.htogo.app.plataforma.UiPlataforma
import com.htogo.shared.data.API_AZURE

/** Entrada de la app completa para iOS: fija el backend y arranca la raíz común. */
@Composable
fun App(ui: UiPlataforma, baseUrl: String = API_AZURE, alIniciar: (CoroutineScope) -> Unit = {}) {
    remember(baseUrl) { ApiClient.getInstance(baseUrl = baseUrl) } // primera inicialización
    val alcance = rememberCoroutineScope()
    LaunchedEffect(Unit) { alIniciar(alcance) }
    HToGoRaiz(ui)
}
