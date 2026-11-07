package com.htogo.app.data.location

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Posición GPS (lat/lon en grados, precisión en metros). */
data class Posicion(val lat: Double, val lon: Double, val precisionM: Float)

/** Indica si una coordenada cae dentro del área cubierta por el grafo OSM de Benito Juárez. */
fun enZonaGrafoOsm(lat: Double, lon: Double): Boolean =
    lat in 19.3360..19.4235 && lon in -99.2065..-99.1135

/** Distancia en metros entre dos puntos (haversine). */
fun distanciaM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6_371_000.0
    val rad = kotlin.math.PI / 180
    val dLat = (lat2 - lat1) * rad
    val dLon = (lon2 - lon1) * rad
    val a = sin(dLat / 2) * sin(dLat / 2) + cos(lat1 * rad) * cos(lat2 * rad) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * r * asin(sqrt(a))
}

/**
 * Genera una polilínea urbana realista (con giros tipo cuadrícula vial en Benito Juárez)
 * como respaldo demostrativo cuando aún no hay ruta A* del servidor.
 */
fun generarRutaDemoCalles(
    destLat: Double,
    destLon: Double,
    origenLat: Double? = null,
    origenLon: Double? = null
): List<Pair<Double, Double>> {
    val oLat = origenLat ?: (destLat + 0.0075).coerceIn(19.355, 19.402)
    val oLon = origenLon ?: (destLon - 0.0055).coerceIn(-99.190, -99.140)
    val dist = distanciaM(oLat, oLon, destLat, destLon)
    if (dist < 45.0) {
        return listOf(Pair(oLat, oLon), Pair(destLat, destLon))
    }
    val dLat = destLat - oLat
    val dLon = destLon - oLon
    // Cuando el trayecto es casi puramente N-S o E-O, introduce un ligero desvío perpendicular de cuadra
    val perpLat = if (kotlin.math.abs(dLat) < 0.0008) 0.00065 else 0.0
    val perpLon = if (kotlin.math.abs(dLon) < 0.0008) 0.00065 else 0.0
    return listOf(
        Pair(oLat, oLon),
        Pair(oLat + dLat * 0.35 + perpLat, oLon + dLon * 0.04),
        Pair(oLat + dLat * 0.38 + perpLat, oLon + dLon * 0.55 + perpLon),
        Pair(oLat + dLat * 0.78, oLon + dLon * 0.58 + perpLon),
        Pair(oLat + dLat * 0.82, oLon + dLon * 0.94),
        Pair(destLat, destLon)
    )
}

/**
 * Subdivide una polilínea de ruta en pasos uniformes (~45 m) para animar el recorrido
 * de FakeGPS calle por calle hasta quedar a ~14 m del domicilio de entrega.
 */
fun interpolarPasosRuta(
    ruta: List<Pair<Double, Double>>,
    destLat: Double,
    destLon: Double,
    pasoMetros: Double = 45.0
): List<Pair<Double, Double>> {
    val base = if (ruta.size >= 2) ruta else generarRutaDemoCalles(destLat, destLon)
    val resultado = mutableListOf<Pair<Double, Double>>()
    for (i in 0 until base.lastIndex) {
        val a = base[i]
        val b = base[i + 1]
        val dist = distanciaM(a.first, a.second, b.first, b.second)
        val nPasos = (dist / pasoMetros).toInt().coerceIn(1, 25)
        for (k in 0 until nPasos) {
            val t = k.toDouble() / nPasos
            resultado.add(
                Pair(
                    a.first + (b.first - a.first) * t,
                    a.second + (b.second - a.second) * t
                )
            )
        }
    }
    // Último punto a ~14 m del destino (dentro de la geocerca de 30 m)
    resultado.add(Pair(destLat + 0.00010, destLon + 0.00007))
    return resultado
}

/**
 * Calcula los kilómetros restantes sobre la polilínea de la ruta desde la posición actual
 * del repartidor (lat, lon) hasta el destino final, proyectando la posición sobre el segmento
 * más cercano para que disminuya de forma continua conforme avanza (tanto con GPS real como en FakeGPS).
 */
fun distanciaRestanteRutaKm(
    lat: Double,
    lon: Double,
    ruta: List<Pair<Double, Double>>,
    destLat: Double,
    destLon: Double,
    distanciaTotalOriginalKm: Double? = null
): Double {
    val distDirectaM = distanciaM(lat, lon, destLat, destLon)
    if (ruta.size < 2) {
        return (distDirectaM / 1000.0).coerceAtLeast(0.0)
    }

    val cosLat = cos(lat * kotlin.math.PI / 180.0).coerceAtLeast(0.1)
    var bestIdx = 0
    var bestQLat = ruta[0].first
    var bestQLon = ruta[0].second
    var minDistM = Double.MAX_VALUE

    var longitudTotalRutaM = 0.0
    val longitudesSegmento = DoubleArray(ruta.size - 1)

    for (i in 0 until ruta.lastIndex) {
        val a = ruta[i]
        val b = ruta[i + 1]
        val segM = distanciaM(a.first, a.second, b.first, b.second)
        longitudesSegmento[i] = segM
        longitudTotalRutaM += segM

        val ax = a.second * cosLat
        val ay = a.first
        val bx = b.second * cosLat
        val by = b.first
        val px = lon * cosLat
        val py = lat
        val dx = bx - ax
        val dy = by - ay
        val lenSq = dx * dx + dy * dy
        val t = if (lenSq > 1e-12) {
            (((px - ax) * dx + (py - ay) * dy) / lenSq).coerceIn(0.0, 1.0)
        } else {
            0.0
        }
        val qLat = a.first + (b.first - a.first) * t
        val qLon = a.second + (b.second - a.second) * t
        val d = distanciaM(lat, lon, qLat, qLon)
        if (d < minDistM) {
            minDistM = d
            bestIdx = i
            bestQLat = qLat
            bestQLon = qLon
        }
    }

    var restantePolilineaM = distanciaM(bestQLat, bestQLon, ruta[bestIdx + 1].first, ruta[bestIdx + 1].second)
    for (k in (bestIdx + 1) until ruta.lastIndex) {
        restantePolilineaM += longitudesSegmento[k]
    }

    val restanteKm = if (distanciaTotalOriginalKm != null && distanciaTotalOriginalKm > 0.0 && longitudTotalRutaM > 10.0) {
        val proporcion = (restantePolilineaM / longitudTotalRutaM).coerceIn(0.0, 1.0)
        distanciaTotalOriginalKm * proporcion + (if (minDistM > 45.0) minDistM / 1000.0 else 0.0)
    } else {
        (restantePolilineaM + (if (minDistM > 45.0) minDistM else 0.0)) / 1000.0
    }

    return if (distDirectaM < 30.0) {
        minOf(restanteKm, distDirectaM / 1000.0).coerceAtLeast(0.0)
    } else {
        restanteKm.coerceAtLeast(0.0)
    }
}

/**
 * Estima los segundos restantes de traslado urbano a [velocidadKmh] (20 km/h en Benito Juárez).
 */
fun estimarSegundosRuta(kmRestantes: Double, velocidadKmh: Double = 20.0): Int {
    val limpio = kmRestantes.coerceAtLeast(0.0)
    val vel = velocidadKmh.coerceAtLeast(1.0)
    return kotlin.math.round((limpio / vel) * 3600.0).toInt().coerceAtLeast(5)
}

/**
 * Estima los minutos restantes de traslado urbano a [velocidadKmh] (20 km/h en Benito Juárez).
 * Devuelve 0 cuando ya está a ≤ 80 m ("llegando / < 1 min").
 */
fun estimarMinutosRuta(kmRestantes: Double, velocidadKmh: Double = 20.0): Int {
    if (kmRestantes <= 0.08) return 0
    return kotlin.math.ceil((kmRestantes / velocidadKmh) * 60.0).toInt().coerceAtLeast(1)
}

/**
 * Formatea una duración en segundos de forma natural y legible según su magnitud:
 * - `< 60 s`: `"15 s"`, `"45 s"` (redondeado a múltiplos de 5 s para evitar parpadeo)
 * - `1 min .. 1 min 59 s`: `"1 min"` o `"1 min 25 s"`
 * - `2 min .. 59 min`: `"8 min"`, `"45 min"`
 * - `>= 60 min`: `"1 h"`, `"2 h 15 min"`, `"5 h"` (p. ej. 300 min -> `"5 h"`)
 */
fun formatearDuracionSegundos(segundosTotales: Int): String {
    val seg = segundosTotales.coerceAtLeast(1)
    if (seg < 60) {
        val segRedondeados = (((seg + 2) / 5) * 5).coerceIn(5, 55)
        return "$segRedondeados s"
    }
    if (seg < 120) {
        val resto = (((seg - 60 + 2) / 5) * 5)
        return when {
            resto <= 0 -> "1 min"
            resto >= 60 -> "2 min"
            else -> "1 min $resto s"
        }
    }
    val minRedondeados = (seg + 30) / 60
    if (minRedondeados < 60) {
        return "$minRedondeados min"
    }
    val horas = minRedondeados / 60
    val mins = minRedondeados % 60
    return if (mins == 0) "$horas h" else "$horas h $mins min"
}

/** Formatea una duración expresada en minutos (`300` -> `"5 h"`, `75` -> `"1 h 15 min"`, `12` -> `"12 min"`). */
fun formatearDuracionMinutos(minutos: Long): String {
    if (minutos <= 0L) return "< 1 min"
    if (minutos < 60L) return "$minutos min"
    val horas = minutos / 60L
    val mins = minutos % 60L
    return if (mins == 0L) "$horas h" else "$horas h $mins min"
}

/** Atajo para estimar y formatear el tiempo de traslado de [kmRestantes] (con [minutosExtra] opcionales de preparación). */
fun formatearDuracionRuta(
    kmRestantes: Double,
    minutosExtra: Int = 0,
    velocidadKmh: Double = 20.0
): String {
    val totalSeg = estimarSegundosRuta(kmRestantes, velocidadKmh) + (minutosExtra.coerceAtLeast(0) * 60)
    return formatearDuracionSegundos(totalSeg)
}

/** Formatea milisegundos transcurridos en segundos, minutos u horas (`"35 s"`, `"4 min"`, `"2 h 10 min"`). */
fun formatearTiempoTranscurrido(ms: Long): String {
    val seg = (ms.coerceAtLeast(1_000L) / 1_000L)
    if (seg < 60L) return "$seg s"
    val min = seg / 60L
    if (min < 60L) return "$min min"
    val horas = min / 60L
    val restoMin = min % 60L
    return if (restoMin == 0L) "$horas h" else "$horas h $restoMin min"
}

private fun formatearEnteroConMiles(valor: Int): String {
    val str = valor.toString()
    if (str.length <= 3) return str
    val sb = StringBuilder()
    for (i in str.indices) {
        if (i > 0 && (str.length - i) % 3 == 0) sb.append(',')
        sb.append(str[i])
    }
    return sb.toString()
}

/**
 * Formato legible de distancia en metros o kilómetros según convenga:
 * - `< 1 km`: `"25 m"`, `"280 m"`, `"950 m"`
 * - `1 km .. 99.9 km`: `"1 km"`, `"1.4 km"`, `"12.5 km"`
 * - `>= 100 km`: `"250 km"`, `"2,995 km"`
 */
fun formatearDistanciaCorta(km: Double): String {
    val limpio = km.coerceAtLeast(0.0)
    if (limpio < 1.0) {
        val metros = kotlin.math.round(limpio * 1000.0).toInt().coerceAtLeast(5)
        if (metros < 1000) return "$metros m"
    }
    if (limpio < 100.0) {
        val decimos = kotlin.math.round(limpio * 10.0).toInt()
        val enteros = decimos / 10
        val dec = decimos % 10
        if (enteros >= 100) return "${formatearEnteroConMiles(enteros)} km"
        return if (dec == 0) "$enteros km" else "$enteros.$dec km"
    }
    val enteros = kotlin.math.round(limpio).toInt()
    return "${formatearEnteroConMiles(enteros)} km"
}

/** Igual que [formatearDistanciaCorta], recibiendo directamente metros. */
fun formatearDistanciaMetros(metros: Double): String =
    formatearDistanciaCorta(metros.coerceAtLeast(0.0) / 1000.0)
