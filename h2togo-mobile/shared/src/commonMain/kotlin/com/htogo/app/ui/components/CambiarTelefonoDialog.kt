package com.htogo.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htogo.app.plataforma.mostrarAviso
import com.htogo.app.ui.theme.HToGoColors

private const val LONGITUD_TELEFONO = 10
private const val LONGITUD_CODIGO = 6

/**
 * Cambiar el teléfono con la sesión iniciada, en dos pasos: número nuevo -> código por SMS a ese
 * número. [onEnviarCodigo] y [onConfirmar] llaman al backend y responden null si salió bien o el
 * mensaje de error. [onActualizado] recibe el número ya guardado.
 *
 * Campos de texto normales (sin reemplazar su contenido al enfocarse) porque en iOS eso falla.
 */
@Composable
fun CambiarTelefonoDialog(
    onDismiss: () -> Unit,
    onEnviarCodigo: (telefono: String, onResult: (String?) -> Unit) -> Unit,
    onConfirmar: (telefono: String, codigo: String, onResult: (String?) -> Unit) -> Unit,
    onActualizado: (telefono: String) -> Unit
) {
    var telefono by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var codigoEnviado by remember { mutableStateOf(false) }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val telefonoValido = telefono.length == LONGITUD_TELEFONO
    val codigoValido = codigo.length == LONGITUD_CODIGO

    fun enviarCodigo() {
        enviando = true
        error = null
        onEnviarCodigo(telefono) { err ->
            enviando = false
            if (err == null) {
                codigoEnviado = true
                codigo = ""
            } else {
                error = err
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!enviando) onDismiss() },
        icon = { Icon(Icons.Filled.Phone, null, tint = HToGoColors.Primary) },
        title = { Text("Cambiar teléfono", fontWeight = FontWeight.Bold, color = HToGoColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!codigoEnviado) {
                    Text(
                        "Te enviaremos un código por SMS al número nuevo para confirmarlo.",
                        fontSize = 13.sp, color = HToGoColors.TextSecondary
                    )
                    HToGoTextField(
                        telefono,
                        { nuevo -> telefono = nuevo.filter { it.isDigit() }.take(LONGITUD_TELEFONO); error = null },
                        "Teléfono nuevo (10 dígitos)",
                        keyboardType = KeyboardType.Number
                    )
                } else {
                    Text(
                        "Escribe el código de 6 dígitos que enviamos al $telefono.",
                        fontSize = 13.sp, color = HToGoColors.TextSecondary
                    )
                    HToGoTextField(
                        codigo,
                        { nuevo -> codigo = nuevo.filter { it.isDigit() }.take(LONGITUD_CODIGO); error = null },
                        "Código de verificación",
                        keyboardType = KeyboardType.Number
                    )
                    TextButton(onClick = { codigoEnviado = false; codigo = ""; error = null }, enabled = !enviando) {
                        Text("Cambiar número", color = HToGoColors.Primary, fontSize = 13.sp)
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            Button(
                enabled = !enviando && (if (codigoEnviado) codigoValido else telefonoValido),
                onClick = {
                    if (!codigoEnviado) {
                        enviarCodigo()
                    } else {
                        enviando = true
                        error = null
                        val numero = telefono
                        onConfirmar(numero, codigo) { err ->
                            enviando = false
                            if (err == null) {
                                mostrarAviso("Teléfono actualizado")
                                onActualizado(numero)
                                onDismiss()
                            } else {
                                error = err
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                if (enviando) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else Text(if (codigoEnviado) "Confirmar" else "Enviar código", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !enviando) {
                Text("Cancelar", color = HToGoColors.TextSecondary)
            }
        }
    )
}
