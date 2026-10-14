package com.htogo.app.data.api

import com.htogo.app.plataforma.Plataforma

/**
 * Geocodificación de direcciones (texto ↔ coordenadas). Fachada común: la búsqueda la hace cada
 * plataforma (Android: Geocoder con respaldo en Nominatim).
 *
 * [GeoResult.exacto] indica si el resultado corresponde al número exterior y no solo a la calle,
 * para que la pantalla pida al usuario ajustar el pin cuando no lo es.
 */
object GeocodingHelper {

    data class GeoResult(
        val lat: Double,
        val lon: Double,
        val displayName: String,
        val road: String? = null,
        val houseNumber: String? = null,
        val neighbourhood: String? = null,
        val postcode: String? = null,
        /** true si se ubicó el número exterior; false si solo se encontró la calle. */
        val exacto: Boolean = false
    )

    /** Mensaje para el usuario según la precisión del resultado. */
    fun mensajeUbicacion(result: GeoResult): String =
        if (result.exacto) "✓ Ubicación colocada en el mapa"
        else "Ubicamos la calle, pero no el número. Arrastra el pin hasta tu puerta."

    suspend fun buscarCoordenadas(direccionTexto: String): GeoResult? =
        Plataforma.servicios.buscarCoordenadas(direccionTexto)

    suspend fun obtenerDireccionDeCoordenadas(lat: Double, lon: Double): GeoResult? =
        Plataforma.servicios.direccionDeCoordenadas(lat, lon)
}
