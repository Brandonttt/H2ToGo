@file:OptIn(ExperimentalSerializationApi::class)

package com.htogo.app.data.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class InventarioItemDto(
    @SerialName("idLote") val idLote: Int = 0,
    @SerialName("idMarca") val idMarca: Int = 0,
    @SerialName("marca") val marca: String = "",
    @SerialName("fechaCaducidad") val fechaCaducidad: String = "",
    @SerialName("cantidadActual") val cantidadActual: Int = 0,
    @SerialName("cantidadApartada") val cantidadApartada: Int? = 0
)

@Serializable
data class InventarioBaseResponse(
    @SerialName("lotes") val lotes: List<InventarioItemDto> = emptyList()
)

@Serializable
data class InventarioVehiculoResponse(
    @SerialName("idVehiculo") val idVehiculo: Int? = null,
    @SerialName("capacidadGarrafones") val capacidadGarrafones: Int = 0,
    @SerialName("ocupado") val ocupado: Int = 0,
    @SerialName("libre") val libre: Int = 0,
    @SerialName("lotes") val lotes: List<InventarioItemDto> = emptyList()
)

@Serializable
data class CargaItemDto(
    @SerialName("idMarca") val idMarca: Int,
    @SerialName("cantidad") val cantidad: Int
)

@Serializable
data class CargaVehiculoRequest(
    @SerialName("cargas") val cargas: List<CargaItemDto>
)

@Serializable
data class LoteEntradaRequest(
    @SerialName("idMarca") val idMarca: Int,
    @SerialName("cantidad") val cantidad: Int,
    @SerialName("fechaCaducidad") val fechaCaducidad: String,
    @SerialName("costoUnitario") val costoUnitario: Double? = null,
    @SerialName("proveedor") val proveedor: String? = null,
    @SerialName("notas") val notas: String? = null
)

@Serializable
data class LoteResponse(
    @SerialName("idLote") val idLote: Int = 0,
    @SerialName("idMarca") val idMarca: Int = 0,
    @SerialName("marca") val marca: String = "",
    @SerialName("fechaCaducidad") val fechaCaducidad: String = "",
    @SerialName("cantidadActual") val cantidadActual: Int = 0
)

@Serializable
data class IniciarJornadaRequest(
    @SerialName("idVehiculo") val idVehiculo: Int,
    @SerialName("cargaInicial") val cargaInicial: List<CargaItemDto>? = null
)

@Serializable
data class JornadaResponse(
    @SerialName("idVehiculo") val idVehiculo: Int = 0,
    // El backend (JornadaResponse.java) manda "estadoOperativo" e "inventarioVehiculo".
    @SerialName("estadoOperativo") @JsonNames("disponible") val disponible: Boolean = false,
    @SerialName("inventarioVehiculo") @JsonNames("inventario") val inventario: InventarioVehiculoResponse? = null
)

@Serializable
data class SalidaManualRequest(
    @SerialName("idMarca") val idMarca: Int,
    @SerialName("cantidad") val cantidad: Int,
    @SerialName("motivo") val motivo: String
)

