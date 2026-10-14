package com.htogo.shared

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.htogo.app.ui.components.HToGoButton
import com.htogo.app.ui.screens.LoginContenido
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme
import com.htogo.shared.auth.AuthRepositorio
import com.htogo.shared.auth.LoginViewModel
import com.htogo.app.data.api.ApiClient
import com.htogo.app.data.local.SessionManager
import com.htogo.shared.data.API_AZURE

/**
 * Raíz de la app en iOS (prueba inicial): login real contra el backend y, al entrar, una
 * pantalla de confirmación. Las demás pantallas se irán moviendo aquí en las siguientes fases.
 */
@Composable
fun App(baseUrl: String = API_AZURE) {
    val sesion = remember { SessionManager.getInstance() }
    val repositorio = remember(baseUrl) { AuthRepositorio(ApiClient.getInstance(baseUrl = baseUrl).authApi) }
    var nombre by remember { mutableStateOf(sesion.obtenerToken()?.let { sesion.obtenerNombre() }) }
    var rol by remember { mutableStateOf(sesion.obtenerRol()) }

    HToGoTheme {
        val actual = nombre
        if (actual == null) {
            val vm = viewModel {
                LoginViewModel(repositorio, sesion) { s ->
                    nombre = "${s.perfil.nombre} ${s.perfil.apellidos}".trim()
                    rol = s.rol
                }
            }
            val estado by vm.estado.collectAsState()
            LoginContenido(estado = estado, onLogin = vm::login, onLimpiarError = vm::limpiarError)
        } else {
            Column(
                Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Sesión iniciada", style = MaterialTheme.typography.headlineLarge, color = HToGoColors.TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text("$actual · ${rol ?: ""}", color = HToGoColors.TextSecondary)
                Spacer(Modifier.height(4.dp))
                Text("H2ToGo en $plataforma", color = HToGoColors.TextTertiary)
                Spacer(Modifier.height(24.dp))
                HToGoButton(text = "Cerrar sesión", onClick = { sesion.cerrarSesion(); nombre = null; rol = null })
            }
        }
    }
}
