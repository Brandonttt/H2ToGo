package com.htogo.app.plataforma

import com.htogo.app.data.api.GeocodingHelper.GeoResult
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLCircularRegion
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.CoreLocation.CLPlacemark
import kotlin.coroutines.resume

/**
 * Geocodificación en iOS con CLGeocoder (datos de Apple Maps, que tienen la numeración de la
 * CDMX). Las búsquedas se sesgan a Benito Juárez. [GeoResult.exacto] indica si se ubicó el
 * número exterior.
 */
@OptIn(ExperimentalForeignApi::class)
internal object GeocodificadorIos {

    private val zonaBenitoJuarez = CLCircularRegion(
        center = CLLocationCoordinate2DMake(19.38, -99.165),
        radius = 6_000.0,
        identifier = "benito-juarez"
    )

    suspend fun buscar(texto: String): GeoResult? = conTiempo {
        suspendCancellableCoroutine { cont ->
            CLGeocoder().geocodeAddressString("$texto, Benito Juárez, Ciudad de México", zonaBenitoJuarez) { marcas, _ ->
                cont.resume((marcas?.firstOrNull() as? CLPlacemark)?.let(::aResultado))
            }
        }
    }

    suspend fun inversa(lat: Double, lon: Double): GeoResult? = conTiempo {
        suspendCancellableCoroutine { cont ->
            CLGeocoder().reverseGeocodeLocation(CLLocation(latitude = lat, longitude = lon)) { marcas, _ ->
                cont.resume((marcas?.firstOrNull() as? CLPlacemark)?.let(::aResultado))
            }
        }
    }

    private suspend fun conTiempo(bloque: suspend () -> GeoResult?): GeoResult? =
        withTimeoutOrNull(8_000) { bloque() }

    private fun aResultado(p: CLPlacemark): GeoResult? {
        val coord = p.location?.coordinate ?: return null
        val (lat, lon) = coord.useContents { latitude to longitude }
        val nombre = listOfNotNull(
            listOfNotNull(p.thoroughfare, p.subThoroughfare).joinToString(" ").ifBlank { null },
            p.subLocality, p.postalCode
        ).joinToString(", ")
        return GeoResult(
            lat = lat,
            lon = lon,
            displayName = nombre.ifBlank { p.name ?: "" },
            road = p.thoroughfare,
            houseNumber = p.subThoroughfare,
            neighbourhood = p.subLocality,
            postcode = p.postalCode,
            exacto = !p.subThoroughfare.isNullOrBlank()
        )
    }
}
