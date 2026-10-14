package com.htogo.app.util

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** Fechas comunes a Android e iOS (kotlinx-datetime), en la hora de la CDMX. */
val ZONA_CDMX: TimeZone = TimeZone.of("America/Mexico_City")

/** Ahora, en hora de la CDMX. */
fun ahoraCdmx(): LocalDateTime = Clock.System.now().toLocalDateTime(ZONA_CDMX)

/** Fecha ISO del backend ("2026-10-04T12:37:00.123-06:00" o con Z) → hora de la CDMX. */
fun fechaCdmx(iso: String?): LocalDateTime? =
    iso?.let { runCatching { Instant.parse(it).toLocalDateTime(ZONA_CDMX) }.getOrNull() }

/** 0 = lunes … 6 = domingo. */
val LocalDate.indiceSemana: Int get() = dayOfWeek.ordinal

/** Lunes de la semana que contiene la fecha. */
fun LocalDate.lunesDeLaSemana(): LocalDate = minus(DatePeriod(days = indiceSemana))

private val DIAS = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
private val MESES = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto",
    "septiembre", "octubre", "noviembre", "diciembre")

/** "Lunes 13 de octubre". */
fun LocalDate.textoLargo(): String =
    "${DIAS[indiceSemana].replaceFirstChar { it.uppercase() }} $dayOfMonth de ${MESES[monthNumber - 1]}"

/** "13 de octubre, 2026". */
fun LocalDate.textoConAnio(): String = "$dayOfMonth de ${MESES[monthNumber - 1]}, $year"

/** Fecha de un DatePicker de Material (milisegundos UTC al inicio del día). */
fun fechaDeSelector(millisUtc: Long): LocalDate =
    Instant.fromEpochMilliseconds(millisUtc).toLocalDateTime(TimeZone.UTC).date

/** Minutos transcurridos desde una fecha ISO del backend; null si no se puede leer. */
fun minutosDesde(iso: String?): Long? =
    iso?.let { runCatching { (Clock.System.now() - Instant.parse(it)).inWholeMinutes }.getOrNull() }

/** "13 oct, 09:30". */
fun LocalDateTime.textoCorto(): String =
    "$dayOfMonth ${MESES[monthNumber - 1].take(3)}, ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

/** "13 de octubre 2026, 09:30:05". */
fun LocalDateTime.textoCompleto(): String =
    "$dayOfMonth de ${MESES[monthNumber - 1]} $year, " +
        listOf(hour, minute, second).joinToString(":") { it.toString().padStart(2, '0') }

/** "10:00 a.m." / "4:30 p.m.". */
fun LocalDateTime.horaAmPm(): String {
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    return "$h12:${minute.toString().padStart(2, '0')} ${if (hour < 12) "a.m." else "p.m."}"
}

/** Fecha y hora de la CDMX → instante ISO-8601 en UTC para el backend ("2026-10-15T16:00:00Z"). */
fun LocalDateTime.aIsoUtc(): String = toInstant(ZONA_CDMX).toString()
