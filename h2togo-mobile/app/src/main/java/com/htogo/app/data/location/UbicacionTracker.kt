package com.htogo.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Posición GPS del dispositivo. */
data class Posicion(val lat: Double, val lon: Double, val precisionM: Float)

/**
 * Ubicación en vivo del repartidor vía FusedLocationProvider. El flujo emite mientras alguien lo
 * recolecta y libera el GPS al cancelarse, así que basta con atarlo al ciclo de vida de la pantalla.
 */
object UbicacionTracker {

    fun tienePermiso(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Emite cada ~5 s (o al moverse 10 m). Requiere [tienePermiso]; si no, no emite nada. */
    @SuppressLint("MissingPermission")
    fun posiciones(context: Context, intervaloMs: Long = 5_000): Flow<Posicion> = callbackFlow {
        if (!tienePermiso(context)) {
            close()
            return@callbackFlow
        }
        val cliente = LocationServices.getFusedLocationProviderClient(context)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervaloMs)
            .setMinUpdateDistanceMeters(10f)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(Posicion(it.latitude, it.longitude, it.accuracy)) }
            }
        }
        // La última conocida pinta el mapa de inmediato mientras llega el primer fix.
        cliente.lastLocation.addOnSuccessListener { it?.let { l -> trySend(Posicion(l.latitude, l.longitude, l.accuracy)) } }
        cliente.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { cliente.removeLocationUpdates(callback) }
    }

    /** Distancia en metros (Haversine), suficiente para umbrales de decenas de metros. */
    fun distanciaM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).let { it * it } +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2).let { it * it }
        return 2 * r * Math.asin(Math.sqrt(a))
    }
}
