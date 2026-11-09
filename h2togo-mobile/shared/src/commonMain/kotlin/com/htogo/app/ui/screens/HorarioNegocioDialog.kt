package com.htogo.app.ui.screens

import com.htogo.app.util.format

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htogo.app.data.dto.HorarioNegocioDto
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.components.SelectorHora

private val DIAS = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

/** Un día en edición; las horas van como "HH:mm". */
private data class DiaHorario(val dia: Int, val cerrado: Boolean, val apertura: String, val cierre: String)

/** Resumen corto del horario para mostrarlo en una fila ("Lun–Vie 08:00–18:00 · cerrado: domingo"). */
fun resumenHorario(horarios: List<HorarioNegocioDto>?): String {
    if (horarios.isNullOrEmpty()) return "Sin horario configurado"
    val abiertos = horarios.filter { !it.cerrado && it.horaApertura != null && it.horaCierre != null }
    if (abiertos.isEmpty()) return "Cerrado todos los días"
    val grupos = abiertos.groupBy { "${it.horaApertura!!.take(5)}–${it.horaCierre!!.take(5)}" }
    val texto = grupos.entries.joinToString(" · ") { (horas, dias) ->
        dias.sortedBy { it.diaSemana }.joinToString(",") { DIAS[(it.diaSemana - 1).coerceIn(0, 6)].take(3) } + " $horas"
    }
    val cerrados = horarios.filter { it.cerrado }.sortedBy { it.diaSemana }
    return if (cerrados.isEmpty()) texto
    else "$texto · cerrado: " + cerrados.joinToString(", ") { DIAS[(it.diaSemana - 1).coerceIn(0, 6)].lowercase() }
}

/**
 * RF-031: el dueño define el horario de cada día de la semana, con días cerrados. Se aplica
 * al instante (no requiere aprobación del admin). [onGuardar] recibe siempre los 7 días.
 */
@Composable
fun HorarioNegocioDialog(
    actuales: List<HorarioNegocioDto>?,
    guardando: Boolean,
    onDismiss: () -> Unit,
    onGuardar: (List<HorarioNegocioDto>) -> Unit
) {
    var editandoHora by remember { mutableStateOf<Pair<String, (String) -> Unit>?>(null) }
    // Al tocar una hora primero se ofrece escribirla; desde ahí se puede abrir el reloj.
    var escribiendoHora by remember { mutableStateOf<Pair<String, (String) -> Unit>?>(null) }
    var dias by remember {
        mutableStateOf((1..7).map { d ->
            val h = actuales?.firstOrNull { it.diaSemana == d }
            DiaHorario(
                dia = d,
                cerrado = h?.cerrado ?: (d == 7),
                apertura = h?.horaApertura?.take(5) ?: "08:00",
                cierre = h?.horaCierre?.take(5) ?: "18:00"
            )
        })
    }
    val invalido = dias.firstOrNull { !it.cerrado && it.apertura >= it.cierre }

    fun elegirHora(actual: String, alElegir: (String) -> Unit) {
        escribiendoHora = actual to alElegir
    }

    escribiendoHora?.let { (actual, alElegir) ->
        EscribirHoraDialog(
            actual = actual,
            onGuardar = { alElegir(it); escribiendoHora = null },
            onUsarReloj = { escribiendoHora = null; editandoHora = actual to alElegir },
            onCancelar = { escribiendoHora = null }
        )
    }

    editandoHora?.let { (actual, alElegir) ->
        val (h, m) = actual.split(":").map { it.toIntOrNull() ?: 0 }
        SelectorHora(h, m, onDismiss = { editandoHora = null }) { hh, mm -> alElegir("%02d:%02d".format(hh, mm)) }
    }

    fun actualizar(i: Int, nuevo: DiaHorario) {
        dias = dias.toMutableList().also { it[i] = nuevo }
    }

    AlertDialog(
        onDismissRequest = { if (!guardando) onDismiss() },
        title = { Text("Horario de atención", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Los clientes solo pueden pedir dentro de este horario; fuera de él se ofrece programar.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = HToGoColors.TextSecondary
                )
                Text(
                    "Toca una hora para escribirla.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = HToGoColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.padding(top = 10.dp))
                dias.forEachIndexed { i, d ->
                    if (i > 0) HorizontalDivider(color = HToGoColors.OutlineSoft)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(DIAS[i], fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1,
                            modifier = Modifier.width(88.dp))
                        if (d.cerrado) {
                            Text("Cerrado", fontSize = 13.sp, color = HToGoColors.TextSecondary, modifier = Modifier.weight(1f))
                        } else {
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                HoraChip(d.apertura) { elegirHora(d.apertura) { actualizar(i, d.copy(apertura = it)) } }
                                Text("–", fontSize = 13.sp)
                                HoraChip(d.cierre) { elegirHora(d.cierre) { actualizar(i, d.copy(cierre = it)) } }
                            }
                        }
                        Switch(checked = !d.cerrado, onCheckedChange = { abierto -> actualizar(i, d.copy(cerrado = !abierto)) })
                    }
                }
                if (invalido != null) {
                    Text(
                        "${DIAS[invalido.dia - 1]}: la apertura debe ser antes del cierre.",
                        fontSize = 12.sp,
                        color = HToGoColors.AccentRose,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = invalido == null && !guardando,
                onClick = {
                    onGuardar(dias.map {
                        HorarioNegocioDto(
                            diaSemana = it.dia,
                            horaApertura = if (it.cerrado) null else "${it.apertura}:00",
                            horaCierre = if (it.cerrado) null else "${it.cierre}:00",
                            cerrado = it.cerrado
                        )
                    })
                }
            ) { Text(if (guardando) "Guardando…" else "Guardar") }
        },
        dismissButton = {
            TextButton(enabled = !guardando, onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun HoraChip(hora: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = HToGoColors.PrimarySoft,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            hora,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = HToGoColors.Primary,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

/** Convierte "8", "830", "0830" o "8:30" en "08:30"; null si no es una hora válida. */
private fun parsearHora(texto: String): String? {
    val t = texto.trim()
    val (h, m) = if (':' in t) {
        val partes = t.split(":")
        if (partes.size != 2) return null
        (partes[0].toIntOrNull() ?: return null) to (partes[1].ifEmpty { "0" }.toIntOrNull() ?: return null)
    } else when (t.length) {
        1, 2 -> (t.toIntOrNull() ?: return null) to 0
        3 -> (t.take(1).toIntOrNull() ?: return null) to (t.drop(1).toIntOrNull() ?: return null)
        4 -> (t.take(2).toIntOrNull() ?: return null) to (t.drop(2).toIntOrNull() ?: return null)
        else -> return null
    }
    if (h !in 0..23 || m !in 0..59) return null
    return "%02d:%02d".format(h, m)
}

/** Escribir la hora a mano (campo vacío para que funcione bien en iOS). */
@Composable
private fun EscribirHoraDialog(
    actual: String,
    onGuardar: (String) -> Unit,
    onUsarReloj: () -> Unit,
    onCancelar: () -> Unit
) {
    var texto by remember { mutableStateOf("") }
    val hora = parsearHora(texto)
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { foco.requestFocus() }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Escribir hora") },
        text = {
            Column {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { nuevo -> texto = nuevo.filter { it.isDigit() || it == ':' }.take(5) },
                    singleLine = true,
                    placeholder = { Text(actual) },
                    supportingText = {
                        Text(
                            if (texto.isNotEmpty() && hora == null) "Hora no válida (00:00 a 23:59)"
                            else "Formato 24 h, p. ej. 0830 o 1900",
                            color = if (texto.isNotEmpty() && hora == null) HToGoColors.AccentRose else HToGoColors.TextSecondary
                        )
                    },
                    isError = texto.isNotEmpty() && hora == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { hora?.let(onGuardar) }),
                    modifier = Modifier.focusRequester(foco)
                )
                TextButton(onClick = onUsarReloj) { Text("Elegir en el reloj") }
            }
        },
        confirmButton = {
            TextButton(onClick = { hora?.let(onGuardar) }, enabled = hora != null) { Text("Listo") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
