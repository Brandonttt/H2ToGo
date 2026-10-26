package com.htogo.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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

/** Mínimo que acepta el backend para una contraseña (RegistroRequest, CambioContrasenaRequest). */
const val LONGITUD_MINIMA_CONTRASENA = 8

/**
 * Cambiar contraseña con la sesión iniciada (perfiles de cliente y repartidor). [onGuardar] llama al
 * backend y responde null si se guardó o el mensaje de error.
 */
@Composable
fun CambiarContrasenaDialog(
    onDismiss: () -> Unit,
    onGuardar: (actual: String, nueva: String, onResult: (String?) -> Unit) -> Unit
) {
    var actual by remember { mutableStateOf("") }
    var nueva by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val nuevaCorta = nueva.isNotEmpty() && nueva.length < LONGITUD_MINIMA_CONTRASENA
    val noCoinciden = confirmar.isNotEmpty() && confirmar != nueva
    val valido = actual.isNotBlank() && nueva.length >= LONGITUD_MINIMA_CONTRASENA && confirmar == nueva

    AlertDialog(
        onDismissRequest = { if (!guardando) onDismiss() },
        icon = { Icon(Icons.Outlined.Lock, null, tint = HToGoColors.Primary) },
        title = { Text("Cambiar contraseña", fontWeight = FontWeight.Bold, color = HToGoColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HToGoTextField(actual, { actual = it; error = null }, "Contraseña actual", isPassword = true)
                HToGoTextField(
                    nueva, { nueva = it; error = null }, "Contraseña nueva", isPassword = true,
                    isError = nuevaCorta,
                    supportingText = if (nuevaCorta) "Mínimo $LONGITUD_MINIMA_CONTRASENA caracteres" else null
                )
                HToGoTextField(
                    confirmar, { confirmar = it; error = null }, "Confirmar contraseña nueva", isPassword = true,
                    isError = noCoinciden,
                    supportingText = if (noCoinciden) "Las contraseñas no coinciden" else null
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            Button(
                enabled = valido && !guardando,
                onClick = {
                    guardando = true
                    onGuardar(actual, nueva) { err ->
                        guardando = false
                        if (err == null) {
                            mostrarAviso("Contraseña actualizada")
                            onDismiss()
                        } else {
                            error = err
                        }
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                if (guardando) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Guardar", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !guardando) {
                Text("Cancelar", color = HToGoColors.TextSecondary)
            }
        }
    )
}
