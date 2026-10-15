package com.htogo.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.htogo.app.data.dto.SesionResponse
import com.htogo.app.ui.AuthUiState
import com.htogo.app.ui.AuthViewModel
import com.htogo.app.ui.theme.HToGoTheme

/**
 * Login en Android: la pantalla vive en el módulo compartido ([LoginContenido], la misma que en
 * iOS) y aquí solo se conecta al AuthViewModel actual (Retrofit, FCM, nombre del negocio).
 */
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel { AuthViewModel() },
    onLoginSuccess: (SesionResponse) -> Unit = {},
    onForgot: () -> Unit = {},
    onRegister: () -> Unit = {},
    onBack: (() -> Unit)? = null
) {
    val uiState by authViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { authViewModel.resetState() }
    LaunchedEffect(uiState) {
        val exito = uiState as? AuthUiState.Success ?: return@LaunchedEffect
        authViewModel.resetState()
        onLoginSuccess(exito.sesion)
    }

    val estado = when (val s = uiState) {
        AuthUiState.Loading -> LoginEstado.Cargando
        is AuthUiState.Error -> if (s.message.contains("SESION_ACTIVA"))
            LoginEstado.Error("Ya hay una sesión activa en otro dispositivo.", sesionActiva = true)
        else LoginEstado.Error(s.message)
        else -> LoginEstado.Inactivo
    }
    LoginContenido(
        estado = estado,
        onLogin = { correo, password, forzar -> authViewModel.login(correo, password, forzar) },
        onLimpiarError = { if (uiState is AuthUiState.Error) authViewModel.resetState() },
        onForgot = onForgot,
        onRegister = onRegister,
        onBack = onBack
    )
}
