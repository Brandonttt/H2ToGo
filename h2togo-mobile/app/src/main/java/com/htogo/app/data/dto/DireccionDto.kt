package com.htogo.app.data.dto

import com.google.gson.annotations.SerializedName

data class DireccionRequest(
    @SerializedName("alias") val alias: String,
    @SerializedName("calle") val calle: String,
    @SerializedName("numeroExterior") val numeroExterior: String,
    @SerializedName("numeroInterior") val numeroInterior: String? = null,
    @SerializedName("colonia") val colonia: String,
    @SerializedName("codigoPostal") val codigoPostal: String,
    @SerializedName("referencias") val referencias: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double
)

data class DireccionResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("alias") val alias: String,
    @SerializedName("calle") val calle: String,
    @SerializedName("numeroExterior") val numeroExterior: String,
    @SerializedName("numeroInterior") val numeroInterior: String?,
    @SerializedName("colonia") val colonia: String,
    @SerializedName("codigoPostal") val codigoPostal: String,
    @SerializedName("referencias") val referencias: String?,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("enZonaCobertura") val enZonaCobertura: Boolean
)
