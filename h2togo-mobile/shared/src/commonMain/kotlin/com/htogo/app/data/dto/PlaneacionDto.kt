package com.htogo.app.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Programado de hoy en la agenda del repartidor (GET /repartidores/me/agenda). */
@Serializable
data class AgendaPedidoDto(
    @SerialName("idPedido") val idPedido: Int = 0,
    @SerialName("fechaProgramada") val fechaProgramada: String? = null,
    @SerialName("estado") val estado: String = "",
    @SerialName("reservadoPorMi") val reservadoPorMi: Boolean = false,
    @SerialName("garrafonesTotales") val garrafonesTotales: Int = 0,
    @SerialName("totalEstimado") val totalEstimado: Double? = null,
    @SerialName("tipoSolicitud") val tipoSolicitud: String? = null,
    @SerialName("nombreCliente") val nombreCliente: String? = null,
    @SerialName("telefonoCliente") val telefonoCliente: String? = null,
    @SerialName("direccion") val direccion: String? = null,
    @SerialName("colonia") val colonia: String? = null,
    @SerialName("latEntrega") val latEntrega: Double? = null,
    @SerialName("lonEntrega") val lonEntrega: Double? = null,
    @SerialName("distanciaM") val distanciaM: Double? = null
)

/** Ruta del día optimizada con OR-Tools (GET /repartidores/me/ruta-optima). */
@Serializable
data class RutaOptimaDto(
    @SerialName("calculadaEn") val calculadaEn: String? = null,
    @SerialName("algoritmo") val algoritmo: String = "",
    @SerialName("origenLat") val origenLat: Double = 0.0,
    @SerialName("origenLon") val origenLon: Double = 0.0,
    @SerialName("distanciaTotalKm") val distanciaTotalKm: Double = 0.0,
    @SerialName("duracionMin") val duracionMin: Long = 0,
    @SerialName("capacidadVehiculo") val capacidadVehiculo: Int = 0,
    @SerialName("garrafonesEnRuta") val garrafonesEnRuta: Int = 0,
    @SerialName("paradas") val paradas: List<ParadaDto> = emptyList(),
    @SerialName("omitidos") val omitidos: List<OmitidoDto> = emptyList(),
    @SerialName("coordenadas") val coordenadas: List<CoordenadaDto> = emptyList()
)

@Serializable
data class ParadaDto(
    @SerialName("orden") val orden: Int = 0,
    @SerialName("idPedido") val idPedido: Int = 0,
    @SerialName("estado") val estado: String = "",
    @SerialName("nombreCliente") val nombreCliente: String? = null,
    @SerialName("telefonoCliente") val telefonoCliente: String? = null,
    @SerialName("direccion") val direccion: String? = null,
    @SerialName("lat") val lat: Double = 0.0,
    @SerialName("lon") val lon: Double = 0.0,
    @SerialName("garrafones") val garrafones: Int = 0,
    @SerialName("fechaProgramada") val fechaProgramada: String? = null,
    @SerialName("llegadaEstimada") val llegadaEstimada: String? = null,
    @SerialName("distanciaDesdeAnteriorKm") val distanciaDesdeAnteriorKm: Double = 0.0,
    @SerialName("fueraDeHorario") val fueraDeHorario: Boolean = false
)

@Serializable
data class OmitidoDto(
    @SerialName("idPedido") val idPedido: Int = 0,
    @SerialName("nombreCliente") val nombreCliente: String? = null,
    @SerialName("direccion") val direccion: String? = null,
    @SerialName("motivo") val motivo: String = ""
)
