package com.htogo.app.data.location

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Posición GPS (lat/lon en grados, precisión en metros). */
data class Posicion(val lat: Double, val lon: Double, val precisionM: Float)

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
    val dLat = destLat - oLat
    val dLon = destLon - oLon
    return listOf(
        Pair(oLat, oLon),
        Pair(oLat + dLat * 0.35, oLon + dLon * 0.05),
        Pair(oLat + dLat * 0.38, oLon + dLon * 0.55),
        Pair(oLat + dLat * 0.78, oLon + dLon * 0.58),
        Pair(oLat + dLat * 0.82, oLon + dLon * 0.92),
        Pair(destLat, destLon)
    )
}

/**
 * Subdivide una polilínea de ruta en pasos uniformes (~45 m) para animar el recorrido
 * de FakeGPS calle por calle hasta quedar a ~18 m del domicilio de entrega.
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
    // Último punto a ~18 m del destino (dentro de la geocerca de 50 m)
    resultado.add(Pair(destLat + 0.00014, destLon + 0.00008))
    return resultado
}
