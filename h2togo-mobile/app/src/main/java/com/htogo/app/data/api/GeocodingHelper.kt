package com.htogo.app.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object GeocodingHelper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    data class GeoResult(
        val lat: Double,
        val lon: Double,
        val displayName: String,
        val road: String? = null,
        val houseNumber: String? = null,
        val neighbourhood: String? = null,
        val postcode: String? = null
    )

    suspend fun buscarCoordenadas(direccionTexto: String): GeoResult? = withContext(Dispatchers.IO) {
        if (direccionTexto.isBlank()) return@withContext null
        try {
            val query = if (direccionTexto.contains("Benito Juárez", ignoreCase = true)) {
                direccionTexto
            } else {
                "$direccionTexto, Benito Juárez, Ciudad de México"
            }
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?format=json&addressdetails=1&limit=1&countrycodes=mx&q=$encoded"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "HToGo-App/1.0 (Android; info@h2togo.mx)")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            val array = JSONArray(body)
            if (array.length() > 0) {
                val obj = array.getJSONObject(0)
                val lat = obj.getDouble("lat")
                val lon = obj.getDouble("lon")
                val displayName = obj.optString("display_name", "")
                val addressObj = obj.optJSONObject("address")
                GeoResult(
                    lat = lat,
                    lon = lon,
                    displayName = displayName,
                    road = addressObj?.optString("road"),
                    houseNumber = addressObj?.optString("house_number"),
                    neighbourhood = addressObj?.optString("neighbourhood")
                        ?: addressObj?.optString("suburb")
                        ?: addressObj?.optString("quarter"),
                    postcode = addressObj?.optString("postcode")
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun obtenerDireccionDeCoordenadas(lat: Double, lon: Double): GeoResult? = withContext(Dispatchers.IO) {
        try {
            val url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon&addressdetails=1"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "HToGo-App/1.0 (Android; info@h2togo.mx)")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            val obj = JSONObject(body)
            val displayName = obj.optString("display_name", "")
            val addressObj = obj.optJSONObject("address")
            GeoResult(
                lat = lat,
                lon = lon,
                displayName = displayName,
                road = addressObj?.optString("road"),
                houseNumber = addressObj?.optString("house_number"),
                neighbourhood = addressObj?.optString("neighbourhood")
                    ?: addressObj?.optString("suburb")
                    ?: addressObj?.optString("quarter"),
                postcode = addressObj?.optString("postcode")
            )
        } catch (e: Exception) {
            null
        }
    }
}
