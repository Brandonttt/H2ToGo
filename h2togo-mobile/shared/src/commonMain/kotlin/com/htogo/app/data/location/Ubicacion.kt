package com.htogo.app.data.location

import com.htogo.app.plataforma.Plataforma
import kotlinx.coroutines.flow.StateFlow

/** Fachada común del GPS para las pantallas. */
object UbicacionTracker {
    fun tienePermiso(): Boolean = Plataforma.servicios.tienePermisoUbicacion()
    fun distanciaM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double =
        com.htogo.app.data.location.distanciaM(lat1, lon1, lat2, lon2)
}

/** Seguimiento GPS de la entrega en curso (en Android sigue aunque se abra Maps/Waze). */
object EntregaEnCurso {
    val posicion: StateFlow<Posicion?> get() = Plataforma.servicios.posicionEntrega
    fun iniciar(idPedido: Int) = Plataforma.servicios.iniciarEntrega(idPedido)
    fun detener() = Plataforma.servicios.detenerEntrega()
}
