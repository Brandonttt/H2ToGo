package com.htogo.app.plataforma

import com.htogo.app.data.api.ApiClient
import com.htogo.app.data.location.Posicion
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.darwin.NSObject

/**
 * GPS del repartidor en iOS (CoreLocation), equivalente a EntregaEnCursoService de Android:
 * publica cada posición en [posicion] y la reporta al backend cada 5 s, que la reenvía al
 * cliente por WebSocket y evalúa el aviso de 500 m. Sigue con la app en segundo plano gracias a
 * UIBackgroundModes = location (Info.plist) y la flecha azul de ubicación de iOS.
 */
/** Delegado de CoreLocation (una clase: Kotlin/Native no permite objetos que hereden de NSObject). */
private class DelegadoUbicacion(
    private val alActualizar: (CLLocation) -> Unit,
    private val alCambiarPermiso: (CLLocationManager) -> Unit
) : NSObject(), CLLocationManagerDelegateProtocol {
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        (didUpdateLocations.lastOrNull() as? CLLocation)?.let(alActualizar)
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        // Sin señal momentánea: CoreLocation sigue intentando.
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) = alCambiarPermiso(manager)
}

@OptIn(ExperimentalForeignApi::class)
internal object UbicacionIos {

    private const val REPORTE_MS = 5_000L

    val posicion = MutableStateFlow<Posicion?>(null)
    private val delegado = DelegadoUbicacion(::publicar, ::alCambiarPermiso)
    private val manager = CLLocationManager().apply {
        delegate = delegado
        desiredAccuracy = kCLLocationAccuracyBest
        distanceFilter = 10.0 // metros, como Android
    }
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var siguiendo = false
    private var ultimoReporte = 0L
    private val esperandoPermiso = mutableListOf<(Boolean) -> Unit>()

    fun tienePermiso(): Boolean = manager.authorizationStatus.let {
        it == kCLAuthorizationStatusAuthorizedWhenInUse || it == kCLAuthorizationStatusAuthorizedAlways
    }

    /** Pide el permiso "mientras se usa la app"; responde al decidir el usuario (o de inmediato). */
    fun solicitarPermiso(alResponder: (Boolean) -> Unit) {
        if (manager.authorizationStatus != kCLAuthorizationStatusNotDetermined) {
            alResponder(tienePermiso())
            return
        }
        esperandoPermiso += alResponder
        manager.requestWhenInUseAuthorization()
    }

    fun iniciar() {
        if (!tienePermiso()) return
        siguiendo = true
        manager.allowsBackgroundLocationUpdates = true
        manager.showsBackgroundLocationIndicator = true
        manager.pausesLocationUpdatesAutomatically = false
        manager.location?.let { publicar(it) } // la última conocida pinta el mapa de inmediato
        manager.startUpdatingLocation()
    }

    fun detener() {
        siguiendo = false
        manager.stopUpdatingLocation()
        manager.allowsBackgroundLocationUpdates = false
        posicion.value = null
    }

    private fun alCambiarPermiso(manager: CLLocationManager) {
        if (manager.authorizationStatus == kCLAuthorizationStatusNotDetermined) return
        val concedido = tienePermiso()
        esperandoPermiso.toList().forEach { it(concedido) }
        esperandoPermiso.clear()
        if (concedido && siguiendo) manager.startUpdatingLocation()
    }

    private fun publicar(l: CLLocation) {
        val p = l.coordinate.useContents { Posicion(latitude, longitude, l.horizontalAccuracy.toFloat()) }
        posicion.value = p
        if (!siguiendo) return
        val ahora = ahoraMs()
        if (ahora - ultimoReporte < REPORTE_MS) return
        ultimoReporte = ahora
        alcance.launch {
            try {
                ApiClient.getInstance().repartidorApi.reportarUbicacion(mapOf("lat" to p.lat, "lon" to p.lon))
            } catch (_: Exception) {
                // Sin red: el siguiente fix lo reintenta.
            }
        }
    }
}
