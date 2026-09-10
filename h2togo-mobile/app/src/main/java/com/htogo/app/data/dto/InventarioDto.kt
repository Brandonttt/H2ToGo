package com.htogo.app.data.dto

import com.google.gson.annotations.SerializedName

data class InventarioItemDto(
    @SerializedName("idLote") val idLote: Int,
    @SerializedName("idMarca") val idMarca: Int,
    @SerializedName("marca") val marca: String,
    @SerializedName("fechaCaducidad") val fechaCaducidad: String,
    @SerializedName("cantidadActual") val cantidadActual: Int,
    @SerializedName("cantidadApartada") val cantidadApartada: Int? = 0
)

data class InventarioBaseResponse(
    @SerializedName("lotes") val lotes: List<InventarioItemDto>
)

data class InventarioVehiculoResponse(
    @SerializedName("idVehiculo") val idVehiculo: Int?,
    @SerializedName("capacidadGarrafones") val capacidadGarrafones: Int,
    @SerializedName("ocupado") val ocupado: Int,
    @SerializedName("libre") val libre: Int,
    @SerializedName("lotes") val lotes: List<InventarioItemDto>
)

data class CargaItemDto(
    @SerializedName("idMarca") val idMarca: Int,
    @SerializedName("cantidad") val cantidad: Int
)

data class CargaVehiculoRequest(
    @SerializedName("cargas") val cargas: List<CargaItemDto>
)

data class LoteEntradaRequest(
    @SerializedName("idMarca") val idMarca: Int,
    @SerializedName("cantidad") val cantidad: Int,
    @SerializedName("fechaCaducidad") val fechaCaducidad: String,
    @SerializedName("costoUnitario") val costoUnitario: Double? = null,
    @SerializedName("proveedor") val proveedor: String? = null,
    @SerializedName("notas") val notas: String? = null
)

data class LoteResponse(
    @SerializedName("idLote") val idLote: Int,
    @SerializedName("idMarca") val idMarca: Int,
    @SerializedName("marca") val marca: String,
    @SerializedName("fechaCaducidad") val fechaCaducidad: String,
    @SerializedName("cantidadActual") val cantidadActual: Int
)
