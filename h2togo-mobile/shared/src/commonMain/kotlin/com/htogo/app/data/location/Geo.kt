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

    return if (distDirectaM < 40.0) {
        minOf(restanteKm, distDirectaM / 1000.0).coerceAtLeast(0.0)
    } else {
        restanteKm.coerceAtLeast(0.0)
    }
}
