package com.htogo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.htogo.ui.components.HToGoButton
import com.htogo.ui.components.HToGoTextButton
import com.htogo.ui.components.HToGoTextField
import com.htogo.ui.theme.HToGoColors
import com.htogo.ui.theme.HToGoTheme

enum class TipoUsuario { CLIENTE, REPARTIDOR }

@Composable
fun RegistroScreen(
    onBack: () -> Unit = {},
    onRegistered: (TipoUsuario) -> Unit = {},
    onLogin: () -> Unit = {}
) {
    var tipo by remember { mutableStateOf(TipoUsuario.CLIENTE) }
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var aceptaTerminos by remember { mutableStateOf(false) }

    val passwordsMatch = password.isNotEmpty() && password == confirm
    val canSubmit = nombre.isNotBlank() && correo.contains("@") &&
            telefono.length >= 10 && password.length >= 8 &&
            passwordsMatch && aceptaTerminos

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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HToGoColors.Background
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(HToGoColors.Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.WaterDrop, null, tint = Color.White,
                        modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Crea tu cuenta",
                        style = MaterialTheme.typography.headlineLarge,
                        color = HToGoColors.TextPrimary)
                    Text("Tarda menos de un minuto",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HToGoColors.TextSecondary)
                }
            }

            Spacer(Modifier.height(8.dp))

            HToGoTextField(
                value = nombre, onValueChange = { nombre = it },
                label = "Nombre completo", leadingIcon = Icons.Filled.Person
            )
            HToGoTextField(
                value = correo, onValueChange = { correo = it },
                label = "Correo electrónico",
                leadingIcon = Icons.Filled.AlternateEmail,
                keyboardType = KeyboardType.Email
            )
            HToGoTextField(
                value = telefono, onValueChange = { telefono = it.filter(Char::isDigit).take(10) },
                label = "Teléfono (10 dígitos)",
                leadingIcon = Icons.Filled.Phone,
                keyboardType = KeyboardType.Phone
            )
            HToGoTextField(
                value = password, onValueChange = { password = it },
                label = "Contraseña",
                leadingIcon = Icons.Filled.Lock,
                isPassword = true,
                supportingText = "Mínimo 8 caracteres"
            )
            HToGoTextField(
                value = confirm, onValueChange = { confirm = it },
                label = "Confirmar contraseña",
                leadingIcon = Icons.Filled.Lock,
                isPassword = true,
                isError = confirm.isNotEmpty() && !passwordsMatch,
                supportingText = if (confirm.isNotEmpty() && !passwordsMatch) "Las contraseñas no coinciden" else null
            )

            // Términos
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = aceptaTerminos,
                    onCheckedChange = { aceptaTerminos = it },
                    colors = CheckboxDefaults.colors(checkedColor = HToGoColors.Primary)
                )
                Text(
                    buildAnnotatedString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = HToGoColors.TextSecondary
                )
            }

            Spacer(Modifier.height(8.dp))

            HToGoButton(
                text = "Crear cuenta",
                onClick = { onRegistered(tipo) },
                enabled = canSubmit
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("¿Ya tienes cuenta?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HToGoColors.TextSecondary)
                HToGoTextButton(text = "Iniciar sesión", onClick = onLogin)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RoleCard(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(96.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) HToGoColors.PrimarySoft else HToGoColors.Surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) HToGoColors.Primary else HToGoColors.OutlineSoft
        )
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, null,
                tint = if (selected) HToGoColors.Primary else HToGoColors.TextSecondary,
                modifier = Modifier.size(24.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    color = if (selected) HToGoColors.PrimaryDark else HToGoColors.TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                    color = HToGoColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun buildAnnotatedString() = "Acepto los Términos y el Aviso de Privacidad"

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun RegistroScreenPreview() {
    HToGoTheme { RegistroScreen() }
}
