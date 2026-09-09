package com.htogo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htogo.ui.components.HToGoButton
import com.htogo.ui.components.HToGoTextButton
import com.htogo.ui.theme.HToGoColors
import com.htogo.ui.theme.HToGoTheme
import kotlinx.coroutines.delay

@Composable
fun VerificacionTelefonoScreen(
    telefono: String = "55 1234 5678",
    onBack: () -> Unit = {},
    onVerified: () -> Unit = {},
    onResend: () -> Unit = {},
    onChangePhone: () -> Unit = {}
) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var secondsLeft by remember { mutableStateOf(45) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        while (secondsLeft > 0) { delay(1000); secondsLeft -= 1 }
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
                Modifier.size(72.dp).clip(CircleShape).background(HToGoColors.PrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Sms, null, tint = HToGoColors.Primary,
                    modifier = Modifier.size(36.dp))
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
                "+52 $telefono",
                style = MaterialTheme.typography.titleMedium,
                color = HToGoColors.TextPrimary
            )
            TextButton(onClick = onChangePhone) {
                Text("Cambiar número", color = HToGoColors.Primary)
            }
            Spacer(Modifier.height(24.dp))

            // OTP boxes
            BasicTextField(
                value = code,
                onValueChange = {
                    val filtered = it.filter(Char::isDigit).take(6)
                    code = filtered
                    error = null
                    if (filtered.length == 6) {
                        if (filtered == "123456") onVerified()
                        else error = "Código incorrecto, inténtalo de nuevo"
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
                            Box(
                                Modifier
                                    .size(width = 46.dp, height = 56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (char.isNotEmpty()) HToGoColors.PrimarySoft
                                        else HToGoColors.Surface
                                    )
                                    .border(
                                        width = if (focused || error != null) 2.dp else 1.5.dp,
                                        color = when {
                                            error != null -> HToGoColors.StatusCancelado
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

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error!!, color = HToGoColors.StatusCancelado,
                    style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(24.dp))

            // Reenviar
            if (secondsLeft > 0) {
                Text(
                    "Reenviar código en ${secondsLeft}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HToGoColors.TextSecondary
                )
            } else {
                HToGoTextButton(
                    text = "Reenviar código",
                    onClick = { secondsLeft = 45; onResend() }
                )
            }

            Spacer(Modifier.weight(1f))

            HToGoButton(
                text = "Verificar",
                onClick = {
                    if (code == "123456") onVerified()
                    else error = "Código incorrecto, inténtalo de nuevo"
                },
                enabled = code.length == 6
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun VerificacionTelefonoPreview() {
    HToGoTheme { VerificacionTelefonoScreen() }
}
