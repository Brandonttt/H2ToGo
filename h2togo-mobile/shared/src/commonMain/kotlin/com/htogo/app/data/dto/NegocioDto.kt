@file:OptIn(ExperimentalSerializationApi::class)

package com.htogo.app.data.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class MarcaResponse(
    @SerialName("id") val id: Int = 0,
    @SerialName("nombre") val nombre: String = "",
    @SerialName("descripcion") val descripcion: String? = null
)

@Serializable
data class NegocioCercanoResponse(
    @SerialName("id") val id: Int = 0,
    @SerialName("nombreComercial") val nombreComercial: String = "",
    @SerialName("distanciaKm") private val _distanciaKm: Double? = null,
    @SerialName("distanciaM") private val _distanciaM: Double? = null,
    // Sin valor por defecto "true": antes, sin el dato, todas las purificadoras salían abiertas.
    @SerialName("abiertoAhora") @JsonNames("abierto") val abiertoAhora: Boolean = false,
    @SerialName("lat") val lat: Double? = null,
    @SerialName("lon") val lon: Double? = null,
    @SerialName("direccion") val direccion: String? = null,
    @SerialName("repartidores") val repartidores: Int? = null,
    /** Precio más bajo de su catálogo; null si no tiene productos activos. */
    @SerialName("precioDesde") val precioDesde: Double? = null,
    @SerialName("marcas") val marcas: List<String>? = null
) {
    val distanciaKm: Double
        get() = _distanciaKm ?: ((_distanciaM ?: 0.0) / 1000.0)
}

@Serializable
data class DireccionNegocioDto(
    @SerialName("calle") val calle: String? = null,
    @SerialName("numeroExterior") val numeroExterior: String? = null,
    @SerialName("numeroInterior") val numeroInterior: String? = null,
    @SerialName("colonia") val colonia: String? = null,
    @SerialName("codigoPostal") val codigoPostal: String? = null,
    @SerialName("referencias") val referencias: String? = null,
    @SerialName("lat") val lat: Double? = null,
    @SerialName("lon") val lon: Double? = null
) {
    fun formatDireccion(): String {
        val calleNum = listOfNotNull(calle, numeroExterior).joinToString(" ").trim()
        val parts = listOfNotNull(
            calleNum.takeIf { it.isNotBlank() },
            colonia?.takeIf { it.isNotBlank() },
            codigoPostal?.takeIf { it.isNotBlank() }?.let { "CP $it" }
        )
        return if (parts.isNotEmpty()) parts.joinToString(", ") else "Dirección no registrada"
    }
}

@Serializable
data class HorarioNegocioDto(
    @SerialName("diaSemana") val diaSemana: Int = 0,
    @SerialName("horaApertura") val horaApertura: String? = null,
    @SerialName("horaCierre") val horaCierre: String? = null,
    @SerialName("cerrado") val cerrado: Boolean = false
)

@Serializable
data class PerfilNegocioResponse(
    @SerialName("id") val id: Int = 0,
    @SerialName("nombreComercial") val nombreComercial: String = "",
    @SerialName("activo") val activo: Boolean = true,
    @SerialName("direccion") val direccionObj: DireccionNegocioDto? = null,
    @SerialName("abiertoAhora") val abiertoAhora: Boolean = true,
    @SerialName("horarios") val horarios: List<HorarioNegocioDto>? = null,
    @SerialName("productos") val productos: List<ProductoNegocioDto>? = null,
    @SerialName("vehiculos") val vehiculos: List<VehiculoDto>? = null,
    @SerialName("telefono") val telefono: String? = null,
    @SerialName("calificacionPromedio") val calificacionPromedio: Double? = null
) {
    val direccion: String
        get() = direccionObj?.formatDireccion() ?: "Dirección no registrada"

    val vehiculoPrincipal: VehiculoDto?
        get() = vehiculos?.firstOrNull()
}

@Serializable
data class VehiculoDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("tipoVehiculo") val tipoVehiculo: String? = null,
    @SerialName("marca") val marca: String? = null,
    @SerialName("modelo") val modelo: String? = null,
    @SerialName("color") val color: String? = null,
    @SerialName("placas") val placas: String? = null,
    @SerialName("capacidadGarrafones") val capacidadGarrafones: Int? = null,
    @SerialName("activo") val activo: Boolean = true
)

@Serializable
data class ActualizarVehiculoRequest(
    @SerialName("tipoVehiculo") val tipoVehiculo: String? = null,
    @SerialName("marca") val marca: String? = null,
    @SerialName("modelo") val modelo: String? = null,
    @SerialName("color") val color: String? = null,
    @SerialName("placas") val placas: String? = null,
    @SerialName("capacidadGarrafones") val capacidadGarrafones: Int? = null
)

@Serializable
data class ProductoNegocioDto(
    @SerialName("idProductoNegocio") val idProductoNegocio: Int? = null,
    @SerialName("idMarca") val idMarca: Int = 0,
    @SerialName("marca") private val _marca: String? = null,
    @SerialName("nombreMarca") private val _nombreMarca: String? = null,
    @SerialName("precio") private val _precio: Double? = null,
    @SerialName("precioLiquido") private val _precioLiquido: Double? = null,
    @SerialName("precioEnvase") val precioEnvase: Double = 80.0,
    @SerialName("capacidadMaxima") val capacidadMaxima: Int = 50,
    @SerialName("activo") val activo: Boolean = true,
    @SerialName("stockDisponible") val stockDisponible: Long = 0
) {
    val marca: String get() = _marca ?: _nombreMarca ?: "Agua 20L"
    val nombreMarca: String get() = _marca ?: _nombreMarca ?: "Agua 20L"
    val precio: Double get() = _precio ?: _precioLiquido ?: 45.0
    val precioLiquido: Double get() = _precio ?: _precioLiquido ?: 45.0
}

@Serializable
data class PrecioRequest(
    @SerialName("precio") val precio: Double,
    @SerialName("precioEnvase") val precioEnvase: Double = 80.0
)
