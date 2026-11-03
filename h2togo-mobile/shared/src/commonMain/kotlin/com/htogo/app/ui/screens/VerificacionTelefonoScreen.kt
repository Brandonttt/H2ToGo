package com.htogo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.htogo.app.plataforma.mostrarAviso
import com.htogo.app.ui.AuthUiState
import com.htogo.app.ui.AuthViewModel
import com.htogo.app.ui.OtpUiState
import com.htogo.app.ui.components.HToGoButton
import com.htogo.app.ui.components.HToGoTextButton
import com.htogo.app.ui.theme.HToGoColors
import kotlinx.coroutines.delay

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun VerificacionTelefonoScreen(
    authViewModel: AuthViewModel = viewModel { AuthViewModel() },
    onBack: () -> Unit = {},
    onSuccess: () -> Unit = {}
) {
    val telefonoRaw by authViewModel.telefonoPendiente.collectAsState()
    val telefonoMostrado = when {
        telefonoRaw.isBlank() -> "tu número registrado"
        telefonoRaw.startsWith("+") -> telefonoRaw
        else -> "+52 $telefonoRaw"
    }

    var code by remember { mutableStateOf("") }
    var secondsLeft by remember { mutableStateOf(45) }
    var cicloTimer by remember { mutableStateOf(0) }
    var reenviando by remember { mutableStateOf(false) }

    var mostrarDialogoNumero by remember { mutableStateOf(false) }
    var nuevoTelefono by remember { mutableStateOf("") }
    var passwordConfirmacion by remember { mutableStateOf("") }
    var errorCambioNumero by remember { mutableStateOf<String?>(null) }
    var guardandoNumero by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    val otpState by authViewModel.otpState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val isLoading = otpState is OtpUiState.Loading || authState is AuthUiState.Loading

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(cicloTimer) {
        secondsLeft = 45
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
    }

    LaunchedEffect(otpState) {
        if (otpState is OtpUiState.Success) {
            onSuccess()
        }
    }

    if (mostrarDialogoNumero) {
        val requierePass = !authViewModel.tienePasswordTemporal()
        val puedeGuardar = nuevoTelefono.length == 10 &&
            (!requierePass || passwordConfirmacion.isNotBlank()) &&
            !guardandoNumero

        AlertDialog(
            onDismissRequest = {
                if (!guardandoNumero) {
                    mostrarDialogoNumero = false
                }
            },
            title = { Text("Cambiar número de teléfono") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Ingresa el nuevo número de 10 dígitos al que enviaremos tu código de verificación por SMS.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HToGoColors.TextSecondary
                    )
                    OutlinedTextField(
                        value = nuevoTelefono,
                        onValueChange = {
                            nuevoTelefono = it.filter(Char::isDigit).take(10)
                            errorCambioNumero = null
                        },
                        label = { Text("Nuevo teléfono (10 dígitos)") },
                        placeholder = { Text("Ej. 5512345678") },
                        prefix = { Text("+52 ") },
                        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                        trailingIcon = {
                            if (nuevoTelefono.isNotEmpty() && !guardandoNumero) {
                                IconButton(onClick = { nuevoTelefono = ""; errorCambioNumero = null }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Limpiar")
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (requierePass) {
                        OutlinedTextField(
                            value = passwordConfirmacion,
                            onValueChange = {
                                passwordConfirmacion = it
                                errorCambioNumero = null
                            },
                            label = { Text("Contraseña de tu cuenta") },
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (errorCambioNumero != null) {
                        Text(
                            text = errorCambioNumero!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        guardandoNumero = true
                        errorCambioNumero = null
                        authViewModel.cambiarTelefonoOtp(
                            nuevoTelefono = nuevoTelefono,
                            passwordManual = if (requierePass) passwordConfirmacion else null
                        ) { err ->
                            guardandoNumero = false
                            if (err == null) {
                                mostrarDialogoNumero = false
                                code = ""
                                cicloTimer += 1
                                mostrarAviso("Número actualizado. Te enviamos un nuevo código por SMS")
                            } else {
                                errorCambioNumero = err
                            }
                        }
                    },
                    enabled = puedeGuardar,
                    colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary)
                ) {
                    if (guardandoNumero) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Guardar y enviar SMS")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarDialogoNumero = false },
                    enabled = !guardandoNumero
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        containerColor = HToGoColors.Background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = HToGoColors.Background)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(HToGoColors.PrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Sms,
                    null,
                    tint = HToGoColors.Primary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Verifica tu teléfono",
                style = MaterialTheme.typography.headlineLarge,
                color = HToGoColors.TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Te enviamos un código de 6 dígitos por SMS al",
                style = MaterialTheme.typography.bodyLarge,
                color = HToGoColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Text(
                telefonoMostrado,
                style = MaterialTheme.typography.titleMedium,
                color = HToGoColors.TextPrimary
            )
            TextButton(
                onClick = {
                    nuevoTelefono = telefonoRaw.filter(Char::isDigit).takeLast(10)
                    passwordConfirmacion = ""
                    errorCambioNumero = null
                    mostrarDialogoNumero = true
                },
                enabled = !isLoading
            ) {
                Text("Cambiar número", color = HToGoColors.Primary)
            }
            Spacer(Modifier.height(24.dp))

            // Cartel de error si el OTP falla
            if (otpState is OtpUiState.Error) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = (otpState as OtpUiState.Error).message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // OTP boxes (6 dígitos)
            BasicTextField(
                value = code,
                onValueChange = {
                    val filtered = it.filter(Char::isDigit).take(6)
                    code = filtered
                    if (otpState is OtpUiState.Error && filtered.length < 6) {
                        authViewModel.limpiarErrorOtp()
                    }
                    if (filtered.length == 6 && !isLoading) {
                        authViewModel.verificarOtp(filtered)
                    }
                },
                modifier = Modifier.focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(color = Color.Transparent),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.Transparent),
                decorationBox = { _ ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(6) { i ->
                            val char = code.getOrNull(i)?.toString() ?: ""
                            val focused = code.length == i
                            val isError = otpState is OtpUiState.Error
                            Box(
                                Modifier
                                    .size(width = 46.dp, height = 56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (char.isNotEmpty()) HToGoColors.PrimarySoft
                                        else HToGoColors.Surface
                                    )
                                    .border(
                                        width = if (focused || isError) 2.dp else 1.5.dp,
                                        color = when {
                                            isError -> HToGoColors.StatusCancelado
                                            focused -> HToGoColors.Primary
                                            char.isNotEmpty() -> HToGoColors.Primary
                                            else -> HToGoColors.OutlineSoft
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    char,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HToGoColors.TextPrimary
                                )
                            }
                        }
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            // Reenviar
            when {
                reenviando -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = HToGoColors.Primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Reenviando código...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HToGoColors.TextSecondary
                        )
                    }
                }
                secondsLeft > 0 -> {
                    Text(
                        "Reenviar código en ${secondsLeft}s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HToGoColors.TextSecondary
                    )
                }
                else -> {
                    HToGoTextButton(
                        text = "Reenviar código",
                        onClick = {
                            if (!reenviando) {
                                reenviando = true
                                authViewModel.reenviarOtp { err ->
                                    reenviando = false
                                    if (err == null) {
                                        code = ""
                                        cicloTimer += 1
                                        mostrarAviso("Te enviamos un nuevo código por SMS")
                                    } else {
                                        mostrarAviso(err, largo = true)
                                    }
                                }
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            if (isLoading) {
                CircularProgressIndicator(
                    color = HToGoColors.Primary,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                HToGoButton(
                    text = "Verificar",
                    onClick = {
                        if (code.length == 6) {
                            authViewModel.verificarOtp(code)
                        }
                    },
                    enabled = code.length == 6
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
