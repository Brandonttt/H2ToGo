package com.htogo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.htogo.app.ui.components.HToGoButton
import com.htogo.app.ui.components.HToGoTextButton
import com.htogo.app.ui.components.HToGoTextField
import com.htogo.app.ui.theme.HToGoColors

/** Estado de la pantalla de login, independiente de cómo se hace la petición en cada plataforma. */
sealed interface LoginEstado {
    data object Inactivo : LoginEstado
    data object Cargando : LoginEstado
    /** [sesionActiva]: RF-016, se ofrece cerrar la otra sesión y entrar. */
    data class Error(val mensaje: String, val sesionActiva: Boolean = false) : LoginEstado
}

/**
 * Pantalla de inicio de sesión (CU-002), común a Android e iOS. No conoce la red: recibe el
 * [estado] y avisa con [onLogin]; cada plataforma la conecta a su ViewModel.
 */
@Composable
fun LoginContenido(
    estado: LoginEstado,
    onLogin: (correo: String, password: String, forzar: Boolean) -> Unit,
    onLimpiarError: () -> Unit = {},
    onForgot: () -> Unit = {},
    onRegister: () -> Unit = {},
    /** Regresar al onboarding; null si no hay pantalla previa (p. ej. tras cerrar sesión). */
    onBack: (() -> Unit)? = null
) {
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    val isLoading = estado is LoginEstado.Cargando
    val canSubmit = correo.contains("@") && password.length >= 6 && !isLoading

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(HToGoColors.PrimarySoft, HToGoColors.Background)))
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart).safeDrawingPadding().padding(start = 8.dp, top = 4.dp)
                    .zIndex(1f)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = HToGoColors.TextPrimary)
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(HToGoColors.Primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.WaterDrop, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Bienvenido de vuelta", style = MaterialTheme.typography.headlineLarge, color = HToGoColors.TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Inicia sesión para continuar",
                style = MaterialTheme.typography.bodyLarge,
                color = HToGoColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            if (estado is LoginEstado.Error) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                estado.mensaje,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (estado.sesionActiva) {
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { onLogin(correo, password, true) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Cerrar otra sesión e iniciar aquí") }
                        }
                    }
                }
            }

            HToGoTextField(
                value = correo,
                onValueChange = { correo = it; onLimpiarError() },
                label = "Correo electrónico",
                leadingIcon = Icons.Filled.AlternateEmail,
                keyboardType = KeyboardType.Email
            )
            Spacer(Modifier.height(12.dp))
            HToGoTextField(
                value = password,
                onValueChange = { password = it; onLimpiarError() },
                label = "Contraseña",
                leadingIcon = Icons.Filled.Lock,
                isPassword = true
            )

            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(checkedColor = HToGoColors.Primary)
                )
                Text("Recordarme", style = MaterialTheme.typography.bodyMedium, color = HToGoColors.TextSecondary)
                Spacer(Modifier.weight(1f))
                HToGoTextButton(text = "¿Olvidaste tu contraseña?", onClick = onForgot)
            }

            Spacer(Modifier.height(16.dp))
            if (isLoading) {
                CircularProgressIndicator(color = HToGoColors.Primary, modifier = Modifier.padding(16.dp))
            } else {
                HToGoButton(text = "Iniciar sesión", onClick = { onLogin(correo, password, false) }, enabled = canSubmit)
            }

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("¿Aún no tienes cuenta?", style = MaterialTheme.typography.bodyMedium, color = HToGoColors.TextSecondary)
                HToGoTextButton(text = "Regístrate", onClick = onRegister)
            }
        }
    }
}
