package com.htogo.app.ui.screens

import android.app.TimePickerDialog
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htogo.app.data.dto.HorarioNegocioDto
import com.htogo.app.ui.theme.HToGoColors

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
    val context = LocalContext.current
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
        val (h, m) = actual.split(":").map { it.toIntOrNull() ?: 0 }
        TimePickerDialog(context, { _, hh, mm -> alElegir("%02d:%02d".format(hh, mm)) }, h, m, true).show()
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
                    color = HToGoColors.TextSecondary
                )
                Spacer(Modifier.padding(top = 8.dp))
                dias.forEachIndexed { i, d ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(DIAS[i], fontSize = 14.sp, modifier = Modifier.width(84.dp))
                        if (d.cerrado) {
                            Text("Cerrado", fontSize = 13.sp, color = HToGoColors.TextSecondary, modifier = Modifier.weight(1f))
                        } else {
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp),
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
