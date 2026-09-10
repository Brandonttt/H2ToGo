package com.htogo.app.data.dto

import com.google.gson.annotations.SerializedName

data class PedidoCreateRequest(
    @SerializedName("tipoSolicitud") val tipoSolicitud: String, // "directa" o "abierta"
    @SerializedName("idNegocio") val idNegocio: Int? = null,
    @SerializedName("precioMaximoGarrafon") val precioMaximoGarrafon: Double? = null,
    @SerializedName("idDireccionEntrega") val idDireccionEntrega: Int,
    @SerializedName("indicaciones") val indicaciones: String? = null,
    @SerializedName("programado") val programado: ProgramadoDto? = null,
    @SerializedName("detalles") val detalles: List<DetallePedidoRequest>
)

data class ProgramadoDto(
    @SerializedName("fechaProgramada") val fechaProgramada: String
)

data class DetallePedidoRequest(
    @SerializedName("idMarca") val idMarca: Int,
    @SerializedName("cantidad") val cantidad: Int,
    @SerializedName("tieneEnvase") val tieneEnvase: Boolean
)

data class PedidoResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("estado") val estado: String,
    @SerializedName("tipoSolicitud") val tipoSolicitud: String,
    @SerializedName("idNegocioSolicitado") val idNegocioSolicitado: Int?,
    @SerializedName("idRepartidor") val idRepartidor: Int?,
    @SerializedName("idDireccionEntrega") val idDireccionEntrega: Int?,
    @SerializedName("precioMaximoGarrafon") val precioMaximoGarrafon: Double?,
    @SerializedName("totalPagar") val totalPagar: Double?,
    @SerializedName("garrafonesTotales") val garrafonesTotales: Int?,
    @SerializedName("indicaciones") val indicaciones: String?,
    @SerializedName("esProgramado") val esProgramado: Boolean,
    @SerializedName("fechaProgramada") val fechaProgramada: String?,
    @SerializedName("fechaCreacion") val fechaCreacion: String?,
    @SerializedName("detalles") val detalles: List<DetalleResponse>?,
    @SerializedName("historial") val historial: List<HistorialResponse>?
)

data class DetalleResponse(
    @SerializedName("idMarca") val idMarca: Int,
    @SerializedName("nombreMarca") val nombreMarca: String?,
    @SerializedName("cantidad") val cantidad: Int,
    @SerializedName("tieneEnvase") val tieneEnvase: Boolean,
    @SerializedName("subtotal") val subtotal: Double?
)

data class HistorialResponse(
    @SerializedName("estado") val estado: String,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("motivo") val motivo: String?
)

data class CancelacionRequest(
    @SerializedName("motivo") val motivo: String
)

data class PedidoDisponibleResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("garrafonesTotales") val garrafonesTotales: Int,
    @SerializedName("distanciaKm") val distanciaKm: Double?,
    @SerializedName("tiempoEstimadoMinutos") val tiempoEstimadoMinutos: Int?,
    @SerializedName("totalEstimado") val totalEstimado: Double?,
    @SerializedName("direccionResumen") val direccionResumen: String?
)

data class PedidoResumenDto(
    @SerializedName("id") val id: Int,
    @SerializedName("estado") val estado: String,
    @SerializedName("tipoSolicitud") val tipoSolicitud: String,
    @SerializedName("totalPagar") val totalPagar: Double?,
    @SerializedName("garrafonesTotales") val garrafonesTotales: Int?,
    @SerializedName("esProgramado") val esProgramado: Boolean,
    @SerializedName("fechaCreacion") val fechaCreacion: String?
)

data class PagedResponseDto<T>(
    @SerializedName("contenido") val contenido: List<T>? = null,
    @SerializedName("items") val items: List<T>? = null,
    @SerializedName("totalElementos") val totalElementos: Long = 0L,
    @SerializedName("pagina") val pagina: Int = 0,
    @SerializedName("tamano") val tamano: Int = 0,
    @SerializedName("totalPaginas") val totalPaginas: Int = 0
) {
    val elementos: List<T>
        get() = contenido ?: items ?: emptyList()
}
