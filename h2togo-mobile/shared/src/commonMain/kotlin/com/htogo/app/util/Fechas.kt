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
    iso?.trim()?.takeIf { it.isNotEmpty() }?.let { raw ->
        runCatching { Instant.parse(raw).toLocalDateTime(ZONA_CDMX) }
            .recoverCatching { LocalDateTime.parse(raw.replace(' ', 'T').take(19)) }
            .getOrNull()
    }

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

/** "13 oct, 05:24 p.m." en hora de la CDMX. */
fun LocalDateTime.textoCorto(): String =
    "$dayOfMonth ${MESES[monthNumber - 1].take(3)}, ${horaAmPm()}"

/** "13 de octubre 2026, 05:24 p.m." en hora de la CDMX. */
fun LocalDateTime.textoCompleto(): String =
    "$dayOfMonth de ${MESES[monthNumber - 1]} $year, ${horaAmPm()}"

/** "10:00 a.m." / "05:24 p.m.". */
fun LocalDateTime.horaAmPm(): String {
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    return "${h12.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')} ${if (hour < 12) "a.m." else "p.m."}"
}

/** "21/10/2026 05:24 p.m." en hora de la CDMX. */
fun LocalDateTime.fechaHoraLegible(): String {
    val dd = dayOfMonth.toString().padStart(2, '0')
    val mm = monthNumber.toString().padStart(2, '0')
    return "$dd/$mm/$year ${horaAmPm()}"
}

/** "21/10 05:24 p.m." en hora de la CDMX. */
fun LocalDateTime.diaMesHoraLegible(): String {
    val dd = dayOfMonth.toString().padStart(2, '0')
    val mm = monthNumber.toString().padStart(2, '0')
    return "$dd/$mm ${horaAmPm()}"
}

/** Fecha y hora de la CDMX → instante ISO-8601 en UTC para el backend ("2026-10-15T16:00:00Z"). */
fun LocalDateTime.aIsoUtc(): String = toInstant(ZONA_CDMX).toString()
