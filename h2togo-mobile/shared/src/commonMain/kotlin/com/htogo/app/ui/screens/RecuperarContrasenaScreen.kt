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
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.htogo.app.plataforma.mostrarAviso
import com.htogo.app.ui.AuthViewModel
import com.htogo.app.ui.components.HToGoButton
import com.htogo.app.ui.components.HToGoTextButton
import com.htogo.app.ui.components.HToGoTextField
import com.htogo.app.ui.components.LONGITUD_MINIMA_CONTRASENA
import com.htogo.app.ui.theme.HToGoColors

/**
 * "¿Olvidaste tu contraseña?": 1) correo de la cuenta → el backend manda un código por SMS al
 * teléfono registrado; 2) código + contraseña nueva. Al terminar regresa al login.
 */
@Composable
fun RecuperarContrasenaScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onListo: () -> Unit
) {
    var correo by remember { mutableStateOf("") }
    var codigoEnviado by remember { mutableStateOf(false) }
    var codigo by remember { mutableStateOf("") }
    var nueva by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun enviarCodigo() {
        cargando = true
        error = null
        authViewModel.solicitarRecuperacion(correo) { err ->
            cargando = false
            if (err == null) {
                codigoEnviado = true
                mostrarAviso("Si el correo está registrado, te enviamos un código por SMS")
            } else {
                error = err
            }
        }
    }

    val nuevaCorta = nueva.isNotEmpty() && nueva.length < LONGITUD_MINIMA_CONTRASENA
    val noCoinciden = confirmar.isNotEmpty() && confirmar != nueva
    val puedeEnviar = correo.contains("@") && !cargando
    val puedeConfirmar = codigo.length >= 4 && nueva.length >= LONGITUD_MINIMA_CONTRASENA && confirmar == nueva && !cargando

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(HToGoColors.PrimarySoft, HToGoColors.Background)))
    ) {
        IconButton(
            // En el paso 2 la flecha regresa a corregir el correo.
            onClick = { if (codigoEnviado && !cargando) { codigoEnviado = false; error = null } else onBack() },
            modifier = Modifier.align(Alignment.TopStart).safeDrawingPadding().padding(start = 8.dp, top = 4.dp)
                .zIndex(1f)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = HToGoColors.TextPrimary)
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
                Icon(Icons.Filled.LockReset, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Recuperar contraseña",
                style = MaterialTheme.typography.headlineLarge,
                color = HToGoColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (codigoEnviado) "Escribe el código que llegó por SMS al teléfono de tu cuenta y tu contraseña nueva."
                else "Escribe el correo de tu cuenta. Te mandaremos un código por SMS al teléfono registrado.",
                style = MaterialTheme.typography.bodyLarge,
                color = HToGoColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            error?.let {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (!codigoEnviado) {
                HToGoTextField(
                    value = correo,
                    onValueChange = { correo = it; error = null },
                    label = "Correo electrónico",
                    leadingIcon = Icons.Filled.AlternateEmail,
                    keyboardType = KeyboardType.Email
                )
                Spacer(Modifier.height(24.dp))
                if (cargando) {
                    CircularProgressIndicator(color = HToGoColors.Primary, modifier = Modifier.padding(16.dp))
                } else {
                    HToGoButton(text = "Enviar código", onClick = { enviarCodigo() }, enabled = puedeEnviar)
                }
            } else {
                Text(correo.trim(), style = MaterialTheme.typography.bodyMedium, color = HToGoColors.TextSecondary)
                Spacer(Modifier.height(12.dp))
                HToGoTextField(
                    value = codigo,
                    onValueChange = { v -> codigo = v.filter { it.isDigit() }.take(6); error = null },
                    label = "Código de verificación",
                    leadingIcon = Icons.Filled.Sms,
                    keyboardType = KeyboardType.Number
                )
                Spacer(Modifier.height(12.dp))
                HToGoTextField(
                    value = nueva,
                    onValueChange = { nueva = it; error = null },
                    label = "Contraseña nueva",
                    leadingIcon = Icons.Filled.Lock,
                    isPassword = true,
                    isError = nuevaCorta,
                    supportingText = if (nuevaCorta) "Mínimo $LONGITUD_MINIMA_CONTRASENA caracteres" else null
                )
                Spacer(Modifier.height(12.dp))
                HToGoTextField(
                    value = confirmar,
                    onValueChange = { confirmar = it; error = null },
                    label = "Confirmar contraseña nueva",
                    leadingIcon = Icons.Filled.Lock,
                    isPassword = true,
                    isError = noCoinciden,
                    supportingText = if (noCoinciden) "Las contraseñas no coinciden" else null
                )
                Spacer(Modifier.height(24.dp))
                if (cargando) {
                    CircularProgressIndicator(color = HToGoColors.Primary, modifier = Modifier.padding(16.dp))
                } else {
                    HToGoButton(
                        text = "Cambiar contraseña",
                        enabled = puedeConfirmar,
                        onClick = {
                            cargando = true
                            error = null
                            authViewModel.confirmarRecuperacion(correo, codigo, nueva) { err ->
                                cargando = false
                                if (err == null) {
                                    mostrarAviso("Contraseña actualizada. Inicia sesión con la nueva.")
                                    onListo()
                                } else {
                                    error = err
                                }
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    HToGoTextButton(text = "Reenviar código", onClick = { enviarCodigo() })
                }
            }
        }
    }
}
