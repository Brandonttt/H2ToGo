package com.htogo.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import com.htogo.app.util.fechaDeSelector
import kotlinx.datetime.LocalDate

/**
 * Selector de hora común a Android e iOS (reemplaza TimePickerDialog de Android).
 * [alElegir] recibe hora y minuto (24 h).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorHora(
    hora: Int,
    minuto: Int,
    onDismiss: () -> Unit,
    alElegir: (hora: Int, minuto: Int) -> Unit
) {
    val estado = rememberTimePickerState(initialHour = hora, initialMinute = minuto, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = estado) },
        confirmButton = {
            TextButton(onClick = { alElegir(estado.hour, estado.minute); onDismiss() }) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/**
 * Selector de fecha común (reemplaza DatePickerDialog de Android). [desde] limita la primera
 * fecha elegible (p. ej. hoy); [alElegir] recibe la fecha elegida.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorFecha(
    inicial: LocalDate? = null,
    desde: LocalDate? = null,
    onDismiss: () -> Unit,
    alElegir: (LocalDate) -> Unit
) {
    val estado = rememberDatePickerState(
        initialSelectedDateMillis = inicial?.let { it.toEpochDays().toLong() * 86_400_000L },
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                desde == null || fechaDeSelector(utcTimeMillis) >= desde
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                estado.selectedDateMillis?.let { alElegir(fechaDeSelector(it)) }
                onDismiss()
            }) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    ) { DatePicker(state = estado) }
}
