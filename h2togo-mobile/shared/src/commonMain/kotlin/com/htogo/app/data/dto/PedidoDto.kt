@file:OptIn(ExperimentalSerializationApi::class)

package com.htogo.app.data.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class PedidoCreateRequest(
    @SerialName("tipoSolicitud") val tipoSolicitud: String, // "directa" o "abierta"
    @SerialName("idNegocio") val idNegocio: Int? = null,
    @SerialName("precioMaximoGarrafon") val precioMaximoGarrafon: Double? = null,
    @SerialName("idDireccionEntrega") val idDireccionEntrega: Int,
    @SerialName("indicaciones") val indicaciones: String? = null,
    @SerialName("programado") val programado: ProgramadoDto? = null,
    @SerialName("detalles") val detalles: List<DetallePedidoRequest>
)

@Serializable
data class ProgramadoDto(
    @SerialName("fechaProgramada") val fechaProgramada: String
)

@Serializable
data class DetallePedidoRequest(
    @SerialName("idMarca") val idMarca: Int,
    @SerialName("cantidad") val cantidad: Int,
    @SerialName("tieneEnvase") val tieneEnvase: Boolean
)

@Serializable
data class PedidoResponse(
    @SerialName("id") val id: Int = 0,
    @SerialName("estado") val estado: String = "",
    @SerialName("tipoSolicitud") val tipoSolicitud: String = "",
    @SerialName("idNegocioSolicitado") val idNegocioSolicitado: Int? = null,
    @SerialName("idRepartidor") val idRepartidor: Int? = null,
    @SerialName("idDireccionEntrega") val idDireccionEntrega: Int? = null,
    @SerialName("precioMaximoGarrafon") val precioMaximoGarrafon: Double? = null,
    @SerialName("totalPagar") val totalPagar: Double? = null,
    @SerialName("garrafonesTotales") val garrafonesTotales: Int? = null,
    @SerialName("indicaciones") val indicaciones: String? = null,
    @SerialName("esProgramado") val esProgramado: Boolean = false,
    @SerialName("fechaProgramada") val fechaProgramada: String? = null,
    @SerialName("fechaCreacion") val fechaCreacion: String? = null,
    @SerialName("detalles") val detalles: List<DetalleResponse>? = null,
    @SerialName("historial") val historial: List<HistorialResponse>? = null,
    @SerialName("nombreCliente") val nombreCliente: String? = null,
    @SerialName("telefonoCliente") val telefonoCliente: String? = null,
    @SerialName("direccionTexto") val direccionTexto: String? = null,
    @SerialName("latEntrega") val latEntrega: Double? = null,
    @SerialName("lonEntrega") val lonEntrega: Double? = null,
    @SerialName("nombreNegocio") val nombreNegocio: String? = null,
    /** Repartidor asignado (seguimiento del cliente); null mientras nadie acepta. */
    @SerialName("repartidor") val repartidor: InfoRepartidorDto? = null
)

@Serializable
data class InfoRepartidorDto(
    @SerialName("nombre") val nombre: String? = null,
    /** Solo viene mientras el pedido está asignado o en camino. */
    @SerialName("telefono") val telefono: String? = null,
    @SerialName("negocio") val negocio: String? = null,
    @SerialName("vehiculo") val vehiculo: String? = null,
    @SerialName("placas") val placas: String? = null
)

@Serializable
data class UbicacionRepartidorDto(
    @SerialName("lat") val lat: Double = 0.0,
    @SerialName("lon") val lon: Double = 0.0,
    @SerialName("timestamp") val timestamp: String? = null
)

@Serializable
data class RouteResponse(
    @SerialName("encontrada") val encontrada: Boolean = false,
    @SerialName("distanciaTotalKm") val distanciaTotalKm: Double = 0.0,
    @SerialName("coordenadas") val coordenadas: List<CoordenadaDto> = emptyList()
)

@Serializable
data class CoordenadaDto(
    @SerialName("lat") val lat: Double = 0.0,
    @SerialName("lon") val lon: Double = 0.0
)

@Serializable
data class DetalleResponse(
    @SerialName("id") @JsonNames("idDetalle") val id: Int? = null,
    @SerialName("idMarca") val idMarca: Int = 0,
    @SerialName("nombreMarca") val nombreMarca: String? = null,
    // El backend lo manda como "cantidadSolicitada"; "cantidad" se acepta por compatibilidad.
    @SerialName("cantidad") @JsonNames("cantidadSolicitada") val cantidad: Int = 0,
    @SerialName("tieneEnvase") val tieneEnvase: Boolean = false,
    @SerialName("subtotal") val subtotal: Double? = null,
    @SerialName("precioUnitario") val precioUnitario: Double? = null,
    @SerialName("precioEnvaseUnitario") val precioEnvaseUnitario: Double? = null
)

@Serializable
data class HistorialResponse(
    @SerialName("estado") val estado: String = "",
    @SerialName("fechaCambio") @JsonNames("fecha") val fechaCambio: String? = null,
    @SerialName("notas") @JsonNames("motivo") val notas: String? = null
) {
    val fecha: String
        get() = fechaCambio ?: ""
}

@Serializable
data class CancelacionRequest(
    @SerialName("motivo") val motivo: String
)

@Serializable
data class PedidoDisponibleResponse(
    @SerialName("idPedido") val idPedido: Int? = null,
    @SerialName("id") private val _id: Int? = null,
    @SerialName("garrafonesTotales") val garrafonesTotales: Int = 1,
    @SerialName("distanciaM") val distanciaM: Double? = null,
    @SerialName("distanciaKm") private val _distanciaKm: Double? = null,
    @SerialName("tiempoEstimadoMinutos") val tiempoEstimadoMinutos: Int? = null,
    @SerialName("totalEstimado") val totalEstimado: Double? = null,
    @SerialName("nombreCliente") val nombreCliente: String? = null,
    @SerialName("direccion") val direccion: String? = null,
    @SerialName("direccionResumen") private val _direccionResumen: String? = null,
    @SerialName("colonia") val colonia: String? = null,
    @SerialName("tipoSolicitud") val tipoSolicitud: String? = null,
    @SerialName("detalles") val detalles: List<DetalleResponse>? = null,
    @SerialName("telefonoCliente") val telefonoCliente: String? = null,
    @SerialName("latEntrega") val latEntrega: Double? = null,
    @SerialName("lonEntrega") val lonEntrega: Double? = null
) {
    val id: Int
        get() = idPedido ?: _id ?: 0

    val distanciaKm: Double
        get() = _distanciaKm ?: ((distanciaM ?: 1500.0) / 1000.0)

    val direccionResumen: String
        get() = direccion ?: _direccionResumen ?: colonia ?: "Zona Cobertura"
}

@Serializable
data class PedidoResumenDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("estado") val estado: String = "",
    @SerialName("tipoSolicitud") val tipoSolicitud: String = "",
    @SerialName("totalPagar") val totalPagar: Double? = null,
    @SerialName("garrafonesTotales") val garrafonesTotales: Int? = null,
    @SerialName("esProgramado") val esProgramado: Boolean = false,
    @SerialName("fechaCreacion") val fechaCreacion: String? = null,
    @SerialName("nombreCliente") val nombreCliente: String? = null,
    @SerialName("direccionTexto") val direccionTexto: String? = null,
    @SerialName("nombreNegocio") val nombreNegocio: String? = null,
    @SerialName("nombreRepartidor") val nombreRepartidor: String? = null
)

@Serializable
data class PagedResponseDto<T>(
    @SerialName("contenido") val contenido: List<T>? = null,
    @SerialName("items") val items: List<T>? = null,
    @SerialName("totalElementos") val totalElementos: Long = 0L,
    @SerialName("pagina") val pagina: Int = 0,
    @SerialName("tamano") val tamano: Int = 0,
    @SerialName("totalPaginas") val totalPaginas: Int = 0
) {
    val elementos: List<T>
        get() = contenido ?: items ?: emptyList()
}

@Serializable
data class ResultadoEntregaRequestDto(
    @SerialName("resultado") val resultado: String, // "ENTREGADO" o "NO_ENTREGADO"
    @SerialName("lat") val lat: Double,
    @SerialName("lon") val lon: Double,
    @SerialName("motivoNoEntrega") val motivoNoEntrega: String? = null,
    @SerialName("lineas") val lineas: List<LineaResultadoDto>? = null
)

@Serializable
data class LineaResultadoDto(
    @SerialName("idDetalle") val idDetalle: Int? = null,
    @SerialName("idMarca") val idMarca: Int? = null,
    @SerialName("cantidadEntregada") val cantidadEntregada: Int,
    @SerialName("agregadaEnSitio") val agregadaEnSitio: Boolean = false
)
