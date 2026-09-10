package com.htogo.app.data.dto

import com.google.gson.annotations.SerializedName

data class MarcaResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String?
)

data class NegocioCercanoResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("nombreComercial") val nombreComercial: String,
    @SerializedName("distanciaKm") val distanciaKm: Double,
    @SerializedName("calificacionPromedio") val calificacionPromedio: Double?,
    @SerializedName("abierto") val abierto: Boolean,
    @SerializedName("tiempoEntregaMinutos") val tiempoEntregaMinutos: Int?
)

data class PerfilNegocioResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("nombreComercial") val nombreComercial: String,
    @SerializedName("telefono") val telefono: String?,
    @SerializedName("direccion") val direccion: String?,
    @SerializedName("calificacionPromedio") val calificacionPromedio: Double?,
    @SerializedName("productos") val productos: List<ProductoNegocioDto>?
)

data class ProductoNegocioDto(
    @SerializedName("idMarca") val idMarca: Int,
    @SerializedName("nombreMarca") val nombreMarca: String,
    @SerializedName("precioLiquido") val precioLiquido: Double,
    @SerializedName("precioEnvase") val precioEnvase: Double
)
