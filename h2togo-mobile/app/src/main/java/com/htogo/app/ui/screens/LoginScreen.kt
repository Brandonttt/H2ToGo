package com.htogo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.htogo.app.data.dto.SesionResponse
import com.htogo.app.ui.AuthUiState
import com.htogo.app.ui.AuthViewModel
import com.htogo.app.ui.components.HToGoButton
import com.htogo.app.ui.components.HToGoTextButton
import com.htogo.app.ui.components.HToGoTextField
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel(),
    onLoginSuccess: (SesionResponse) -> Unit = {},
    onForgot: () -> Unit = {},
    onRegister: () -> Unit = {}
) {
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }

    val uiState by authViewModel.uiState.collectAsState()
    val isLoading = uiState is AuthUiState.Loading
    val canSubmit = correo.contains("@") && password.length >= 6 && !isLoading

    // Escuchar cambios en uiState para navegar si fue exitoso
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onLoginSuccess((uiState as AuthUiState.Success).sesion)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(HToGoColors.PrimarySoft, HToGoColors.Background))
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(HToGoColors.Primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.WaterDrop,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Bienvenido de vuelta",
                style = MaterialTheme.typography.headlineLarge,
                color = HToGoColors.TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Inicia sesión para continuar",
                style = MaterialTheme.typography.bodyLarge,
                color = HToGoColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            // Cartel de error en caso de fallo
            if (uiState is AuthUiState.Error) {
                val errorMsg = (uiState as AuthUiState.Error).message
                val isConflictoSesion = errorMsg.contains("SESION_ACTIVA")

                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isConflictoSesion)
                                    "Ya hay una sesión activa en otro dispositivo."
                                else
                                    errorMsg,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (isConflictoSesion) {
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    authViewModel.login(correo, password, forzar = true)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Cerrar otra sesión e iniciar aquí")
                            }
                        }
                    }
                }
            }

            HToGoTextField(
                value = correo,
                onValueChange = {
                    correo = it
                    if (uiState is AuthUiState.Error) authViewModel.resetState()
                },
                label = "Correo electrónico",
                leadingIcon = Icons.Filled.AlternateEmail,
                keyboardType = KeyboardType.Email
            )
            Spacer(Modifier.height(12.dp))
            HToGoTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (uiState is AuthUiState.Error) authViewModel.resetState()
                },
                label = "Contraseña",
                leadingIcon = Icons.Filled.Lock,
                isPassword = true
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(checkedColor = HToGoColors.Primary)
                )
                Text(
                    "Recordarme",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HToGoColors.TextSecondary
                )
                Spacer(Modifier.weight(1f))
                HToGoTextButton(text = "¿Olvidaste tu contraseña?", onClick = onForgot)
            }

            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    color = HToGoColors.Primary,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                HToGoButton(
                    text = "Iniciar sesión",
                    onClick = {
                        authViewModel.login(correo, password, forzar = false)
                    },
                    enabled = canSubmit
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "¿Aún no tienes cuenta?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HToGoColors.TextSecondary
                )
                HToGoTextButton(text = "Regístrate", onClick = onRegister)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun LoginScreenPreview() {
    HToGoTheme { LoginScreen() }
}
