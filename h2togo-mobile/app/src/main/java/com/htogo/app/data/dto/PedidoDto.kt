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
    @SerializedName("historial") val historial: List<HistorialResponse>?,
    @SerializedName("nombreCliente") val nombreCliente: String? = null,
    @SerializedName("telefonoCliente") val telefonoCliente: String? = null,
    @SerializedName("direccionTexto") val direccionTexto: String? = null,
    @SerializedName("latEntrega") val latEntrega: Double? = null,
    @SerializedName("lonEntrega") val lonEntrega: Double? = null
)

data class RouteResponse(
    @SerializedName("encontrada") val encontrada: Boolean = false,
    @SerializedName("distanciaTotalKm") val distanciaTotalKm: Double = 0.0,
    @SerializedName("coordenadas") val coordenadas: List<CoordenadaDto> = emptyList()
)

data class CoordenadaDto(
    @SerializedName("lat") val lat: Double = 0.0,
    @SerializedName("lon") val lon: Double = 0.0
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
    @SerializedName("idPedido") val idPedido: Int? = null,
    @SerializedName("id") private val _id: Int? = null,
    @SerializedName("garrafonesTotales") val garrafonesTotales: Int = 1,
    @SerializedName("distanciaM") val distanciaM: Double? = null,
    @SerializedName("distanciaKm") private val _distanciaKm: Double? = null,
    @SerializedName("tiempoEstimadoMinutos") val tiempoEstimadoMinutos: Int? = null,
    @SerializedName("totalEstimado") val totalEstimado: Double? = null,
    @SerializedName("nombreCliente") val nombreCliente: String? = null,
    @SerializedName("direccion") val direccion: String? = null,
    @SerializedName("direccionResumen") private val _direccionResumen: String? = null,
    @SerializedName("colonia") val colonia: String? = null,
    @SerializedName("tipoSolicitud") val tipoSolicitud: String? = null,
    @SerializedName("detalles") val detalles: List<DetalleResponse>? = null,
    @SerializedName("telefonoCliente") val telefonoCliente: String? = null,
    @SerializedName("latEntrega") val latEntrega: Double? = null,
    @SerializedName("lonEntrega") val lonEntrega: Double? = null
) {
    val id: Int
        get() = idPedido ?: _id ?: 0

    val distanciaKm: Double
        get() = _distanciaKm ?: ((distanciaM ?: 1500.0) / 1000.0)

    val direccionResumen: String
        get() = direccion ?: _direccionResumen ?: colonia ?: "Zona Cobertura"
}

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
