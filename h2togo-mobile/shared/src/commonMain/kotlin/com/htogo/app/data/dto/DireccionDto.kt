@file:OptIn(ExperimentalSerializationApi::class)

package com.htogo.app.data.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class DireccionRequest(
    @SerialName("alias") val alias: String,
    @SerialName("calle") val calle: String,
    @SerialName("numeroExterior") val numeroExterior: String,
    @SerialName("numeroInterior") val numeroInterior: String? = null,
    @SerialName("colonia") val colonia: String,
    @SerialName("codigoPostal") val codigoPostal: String,
    @SerialName("referencias") val referencias: String,
    @SerialName("lat") val lat: Double,
    @SerialName("lon") val lon: Double
)

@Serializable
data class DireccionResponse(
    @SerialName("id") val id: Int = 0,
    @SerialName("alias") val alias: String = "",
    @SerialName("calle") val calle: String = "",
    @SerialName("numeroExterior") val numeroExterior: String = "",
    @SerialName("numeroInterior") val numeroInterior: String? = null,
    @SerialName("colonia") val colonia: String = "",
    @SerialName("codigoPostal") val codigoPostal: String = "",
    @SerialName("referencias") val referencias: String? = null,
    @SerialName("lat") val lat: Double = 0.0,
    @SerialName("lon") val lon: Double = 0.0,
    @SerialName("enZonaCobertura") val enZonaCobertura: Boolean = false
)
