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
    @SerializedName("distanciaKm") private val _distanciaKm: Double? = null,
    @SerializedName("distanciaM") private val _distanciaM: Double? = null,
    @SerializedName("calificacionPromedio") val calificacionPromedio: Double? = null,
    @SerializedName("abierto") val abierto: Boolean = true,
    @SerializedName("tiempoEntregaMinutos") val tiempoEntregaMinutos: Int? = null,
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lon") val lon: Double? = null,
    @SerializedName("direccion") val direccion: String? = null,
    @SerializedName("repartidores") val repartidores: Int? = null
) {
    val distanciaKm: Double
        get() = _distanciaKm ?: ((_distanciaM ?: 0.0) / 1000.0)
}

data class DireccionNegocioDto(
    @SerializedName("calle") val calle: String? = null,
    @SerializedName("numeroExterior") val numeroExterior: String? = null,
    @SerializedName("numeroInterior") val numeroInterior: String? = null,
    @SerializedName("colonia") val colonia: String? = null,
    @SerializedName("codigoPostal") val codigoPostal: String? = null,
    @SerializedName("referencias") val referencias: String? = null,
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lon") val lon: Double? = null
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

data class HorarioNegocioDto(
    @SerializedName("diaSemana") val diaSemana: Int,
    @SerializedName("horaApertura") val horaApertura: String?,
    @SerializedName("horaCierre") val horaCierre: String?,
    @SerializedName("cerrado") val cerrado: Boolean
)

data class PerfilNegocioResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("nombreComercial") val nombreComercial: String,
    @SerializedName("activo") val activo: Boolean = true,
    @SerializedName("direccion") val direccionObj: DireccionNegocioDto? = null,
    @SerializedName("abiertoAhora") val abiertoAhora: Boolean = true,
    @SerializedName("horarios") val horarios: List<HorarioNegocioDto>? = null,
    @SerializedName("productos") val productos: List<ProductoNegocioDto>? = null,
    @SerializedName("vehiculos") val vehiculos: List<VehiculoDto>? = null,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("calificacionPromedio") val calificacionPromedio: Double? = null
) {
    val direccion: String
        get() = direccionObj?.formatDireccion() ?: "Dirección no registrada"

    val vehiculoPrincipal: VehiculoDto?
        get() = vehiculos?.firstOrNull()
}

data class VehiculoDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("tipoVehiculo") val tipoVehiculo: String? = null,
    @SerializedName("marca") val marca: String? = null,
    @SerializedName("modelo") val modelo: String? = null,
    @SerializedName("color") val color: String? = null,
    @SerializedName("placas") val placas: String? = null,
    @SerializedName("capacidadGarrafones") val capacidadGarrafones: Int? = null,
    @SerializedName("activo") val activo: Boolean = true
)

data class ActualizarVehiculoRequest(
    @SerializedName("tipoVehiculo") val tipoVehiculo: String? = null,
    @SerializedName("marca") val marca: String? = null,
    @SerializedName("modelo") val modelo: String? = null,
    @SerializedName("color") val color: String? = null,
    @SerializedName("placas") val placas: String? = null,
    @SerializedName("capacidadGarrafones") val capacidadGarrafones: Int? = null
)

data class ProductoNegocioDto(
    @SerializedName("idProductoNegocio") val idProductoNegocio: Int? = null,
    @SerializedName("idMarca") val idMarca: Int = 0,
    @SerializedName("marca") private val _marca: String? = null,
    @SerializedName("nombreMarca") private val _nombreMarca: String? = null,
    @SerializedName("precio") private val _precio: Double? = null,
    @SerializedName("precioLiquido") private val _precioLiquido: Double? = null,
    @SerializedName("precioEnvase") val precioEnvase: Double = 80.0,
    @SerializedName("capacidadMaxima") val capacidadMaxima: Int = 50,
    @SerializedName("activo") val activo: Boolean = true,
    @SerializedName("stockDisponible") val stockDisponible: Long = 0
) {
    val marca: String get() = _marca ?: _nombreMarca ?: "Agua 20L"
    val nombreMarca: String get() = _marca ?: _nombreMarca ?: "Agua 20L"
    val precio: Double get() = _precio ?: _precioLiquido ?: 45.0
    val precioLiquido: Double get() = _precio ?: _precioLiquido ?: 45.0
}

data class PrecioRequest(
    @SerializedName("precio") val precio: Double,
    @SerializedName("precioEnvase") val precioEnvase: Double = 80.0
)
