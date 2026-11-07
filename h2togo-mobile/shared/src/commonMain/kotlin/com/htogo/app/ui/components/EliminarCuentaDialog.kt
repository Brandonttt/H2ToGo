package com.htogo.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htogo.app.plataforma.mostrarAviso
import com.htogo.app.ui.theme.HToGoColors

/**
 * Diálogo de confirmación para eliminar la cuenta propia (perfiles de cliente y repartidor).
 * Solicita la contraseña actual para evitar eliminaciones accidentales o no autorizadas.
 */
@Composable
fun EliminarCuentaDialog(
    onDismiss: () -> Unit,
    onConfirmar: (password: String, onResult: (String?) -> Unit) -> Unit,
    onEliminada: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var eliminando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!eliminando) onDismiss() },
        icon = { Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = HToGoColors.AccentRose) },
        title = {
            Text(
                "¿Eliminar cuenta?",
                fontWeight = FontWeight.Bold,
                color = HToGoColors.TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Esta acción dará de baja tu cuenta de forma permanente, cerrará tu sesión y desactivará tus direcciones guardadas. Ingresa tu contraseña actual para confirmar.",
                    color = HToGoColors.TextSecondary,
                    fontSize = 14.sp
                )
                HToGoTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        error = null
                    },
                    label = "Contraseña actual",
                    isPassword = true
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = password.isNotBlank() && !eliminando,
                onClick = {
                    eliminando = true
                    error = null
                    onConfirmar(password) { err ->
                        eliminando = false
                        if (err == null) {
                            mostrarAviso("Tu cuenta ha sido eliminada")
                            onDismiss()
                            onEliminada()
                        } else {
                            error = err
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.AccentRose),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (eliminando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Eliminar cuenta", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !eliminando
            ) {
                Text("Cancelar", color = HToGoColors.TextSecondary)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}
