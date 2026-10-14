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
