package com.htogo.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.htogo.app.data.dto.SesionResponse
import com.htogo.app.plataforma.mostrarAviso
import com.htogo.app.ui.AuthUiState
import com.htogo.app.ui.AuthViewModel

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
    onIrAVerificarTelefono: () -> Unit = {},
    onBack: (() -> Unit)? = null
) {
    val uiState by authViewModel.uiState.collectAsState()
    var enviandoVerificacion by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { authViewModel.resetState() }
    LaunchedEffect(uiState) {
        val exito = uiState as? AuthUiState.Success ?: return@LaunchedEffect
        authViewModel.resetState()
        onLoginSuccess(exito.sesion)
    }

    val estado = when (val s = uiState) {
        AuthUiState.Loading -> LoginEstado.Cargando
        is AuthUiState.Error -> when {
            s.sesionActiva || s.message.contains("SESION_ACTIVA") ->
                LoginEstado.Error("Ya hay una sesión activa en otro dispositivo.", sesionActiva = true)
            s.telefonoNoVerificado ->
                LoginEstado.Error(s.message, telefonoNoVerificado = true)
            else -> LoginEstado.Error(s.message)
        }
        else -> LoginEstado.Inactivo
    }
    LoginContenido(
        estado = estado,
        onLogin = { correo, password, forzar -> authViewModel.login(correo, password, forzar) },
        onLimpiarError = { if (uiState is AuthUiState.Error) authViewModel.resetState() },
        onForgot = onForgot,
        onRegister = onRegister,
        enviandoVerificacion = enviandoVerificacion,
        onVerificarTelefono = { correo, password ->
            if (!enviandoVerificacion) {
                enviandoVerificacion = true
                authViewModel.iniciarVerificacionDesdeLogin(correo, password) { yaVerificado, error ->
                    enviandoVerificacion = false
                    if (error != null) {
                        mostrarAviso(error, largo = true)
                    } else if (!yaVerificado) {
                        mostrarAviso("Te enviamos un código de verificación por SMS")
                        onIrAVerificarTelefono()
                    }
                }
            }
        },
        onBack = onBack
    )
}
