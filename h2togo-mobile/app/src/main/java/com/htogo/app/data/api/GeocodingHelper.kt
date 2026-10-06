package com.htogo.app.data.api

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Geocodificación de direcciones (texto ↔ coordenadas).
 *
 * Primero usa el [Geocoder] de Android, que en teléfonos con Google Play Services consulta los
 * datos de Google: sí tiene la numeración de las calles de la CDMX, es gratis y no necesita API
 * key. Nominatim (OpenStreetMap) queda de respaldo: en la CDMX casi no tiene números exteriores
 * y, cuando no encuentra el número, devuelve un punto genérico de la calle.
 *
 * [GeoResult.exacto] indica si el resultado corresponde al número exterior y no solo a la calle,
 * para que la pantalla pida al usuario ajustar el pin cuando no lo es.
 */
object GeocodingHelper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private val localeMx = Locale("es", "MX")

    // Recuadro alrededor de Benito Juárez (con margen): sesga los resultados a la zona de cobertura.
    private const val SUR = 19.33
    private const val OESTE = -99.22
    private const val NORTE = 19.43
    private const val ESTE = -99.11

    private var appContext: Context? = null

    /** Se llama una vez en Application.onCreate; sin esto solo se usa Nominatim. */
    fun inicializar(context: Context) {
        appContext = context.applicationContext
    }

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

    suspend fun buscarCoordenadas(direccionTexto: String): GeoResult? = withContext(Dispatchers.IO) {
        if (direccionTexto.isBlank()) return@withContext null
        val query = if (direccionTexto.contains("Benito Juárez", ignoreCase = true)) {
            direccionTexto
        } else {
            "$direccionTexto, Benito Juárez, Ciudad de México"
        }

        val google = geocoderAndroid { g, listo -> buscarEnGeocoder(g, query, listo) }
        if (google?.exacto == true) return@withContext google
        val osm = nominatimBuscar(query)
        when {
            osm?.exacto == true -> osm
            google != null -> google // calle de Google: suele quedar más cerca que la de OSM
            else -> osm
        }
    }

    suspend fun obtenerDireccionDeCoordenadas(lat: Double, lon: Double): GeoResult? = withContext(Dispatchers.IO) {
        val google = geocoderAndroid { g, listo -> inversaEnGeocoder(g, lat, lon, listo) }
        if (google != null && !google.road.isNullOrBlank()) {
            // Se conservan las coordenadas del pin: el usuario lo puso donde quiere.
            return@withContext google.copy(lat = lat, lon = lon)
        }
        nominatimInversa(lat, lon) ?: google?.copy(lat = lat, lon = lon)
    }

    // ---------------------------------------------------------------- Geocoder de Android

    /**
     * Ejecuta una consulta del Geocoder con tope de 6 s. Devuelve null si no hay Geocoder en el
     * dispositivo (p. ej. sin Play Services), si falla o si no hay resultados.
     */
    private suspend fun geocoderAndroid(
        consulta: (Geocoder, (List<Address>?) -> Unit) -> Unit
    ): GeoResult? {
        val ctx = appContext ?: return null
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(ctx, localeMx)
        val direcciones = withTimeoutOrNull(6_000) {
            suspendCancellableCoroutine { cont ->
                try {
                    consulta(geocoder) { if (cont.isActive) cont.resume(it) }
                } catch (e: Exception) {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
        return direcciones?.firstOrNull()?.let(::desdeAddress)
    }

    @Suppress("DEPRECATION")
    private fun buscarEnGeocoder(g: Geocoder, query: String, listo: (List<Address>?) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            g.getFromLocationName(query, 1, SUR, OESTE, NORTE, ESTE, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) = listo(addresses)
                override fun onError(errorMessage: String?) = listo(null)
            })
        } else {
            listo(g.getFromLocationName(query, 1, SUR, OESTE, NORTE, ESTE)) // API < 33: síncrono (ya en IO)
        }
    }

    @Suppress("DEPRECATION")
    private fun inversaEnGeocoder(g: Geocoder, lat: Double, lon: Double, listo: (List<Address>?) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            g.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) = listo(addresses)
                override fun onError(errorMessage: String?) = listo(null)
            })
        } else {
            listo(g.getFromLocation(lat, lon, 1))
        }
    }

    private fun desdeAddress(a: Address): GeoResult {
        val numero = a.subThoroughfare?.takeIf { it.isNotBlank() }
        return GeoResult(
            lat = a.latitude,
            lon = a.longitude,
            displayName = a.getAddressLine(0) ?: listOfNotNull(a.thoroughfare, numero).joinToString(" "),
            road = a.thoroughfare,
            houseNumber = numero,
            neighbourhood = a.subLocality ?: a.locality,
            postcode = a.postalCode,
            exacto = numero != null
        )
    }

    // ---------------------------------------------------------------- Nominatim (respaldo)

    private fun nominatimBuscar(query: String): GeoResult? = try {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val body = get("https://nominatim.openstreetmap.org/search?format=json&addressdetails=1&limit=1&countrycodes=mx&q=$encoded")
        val array = body?.let(::JSONArray)
        if (array != null && array.length() > 0) desdeNominatim(array.getJSONObject(0)) else null
    } catch (e: Exception) {
        null
    }

    private fun nominatimInversa(lat: Double, lon: Double): GeoResult? = try {
        get("https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon&addressdetails=1")
            ?.let { desdeNominatim(JSONObject(it)).copy(lat = lat, lon = lon) }
    } catch (e: Exception) {
        null
    }

    private fun get(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "HToGo-App/1.0 (Android; info@h2togo.mx)")
            .build()
        return client.newCall(request).execute().use { it.body?.string() }
    }

    private fun desdeNominatim(obj: JSONObject): GeoResult {
        val address = obj.optJSONObject("address")
        // optString devuelve "" (no null) cuando falta la clave.
        fun campo(vararg claves: String) = claves.firstNotNullOfOrNull { k ->
            address?.optString(k)?.takeIf { it.isNotBlank() }
        }
        val numero = campo("house_number")
        return GeoResult(
            lat = obj.getDouble("lat"),
            lon = obj.getDouble("lon"),
            displayName = obj.optString("display_name", ""),
            road = campo("road"),
            houseNumber = numero,
            neighbourhood = campo("neighbourhood", "suburb", "quarter"),
            postcode = campo("postcode"),
            exacto = numero != null
        )
    }
}
