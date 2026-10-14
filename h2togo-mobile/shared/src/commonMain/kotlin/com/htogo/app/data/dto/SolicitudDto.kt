@file:OptIn(ExperimentalSerializationApi::class)

package com.htogo.app.data.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonObject

/**
 * Solicitud de cambio de perfil del negocio (CU-020). Solo el dueño puede crearlas; el admin
 * las aprueba o rechaza desde el panel web (RN-020).
 */
@Serializable
data class SolicitudRequest(
    @SerialName("codigoCambio") val codigoCambio: String,
    @SerialName("idVehiculo") val idVehiculo: Int? = null,
    @SerialName("idProductoNegocio") val idProductoNegocio: Int? = null,
    @SerialName("valorNuevo") val valorNuevo: JsonObject
) {
    /** Construye la solicitud a partir de un mapa simple (texto, números, booleanos, listas). */
    constructor(
        codigoCambio: String,
        idVehiculo: Int? = null,
        idProductoNegocio: Int? = null,
        valorNuevo: Map<String, Any?>
    ) : this(codigoCambio, idVehiculo, idProductoNegocio, valorNuevo.aJsonObject())
}

@Serializable
data class SolicitudResponse(
    @SerialName("id") val id: Int = 0,
    @SerialName("codigoCambio") val codigoCambio: String = "",
    @SerialName("idNegocio") val idNegocio: Int = 0,
    @SerialName("idVehiculo") val idVehiculo: Int? = null,
    @SerialName("idProductoNegocio") val idProductoNegocio: Int? = null,
    @SerialName("valorAnterior") val valorAnterior: String? = null,
    /** JSON con los datos propuestos; su forma depende de [codigoCambio]. */
    @SerialName("valorNuevo") val valorNuevo: String = "",
    /** pendiente | aprobado | rechazado */
    @SerialName("estado") val estado: String = "",
    @SerialName("comentarioAdmin") val comentarioAdmin: String? = null,
    @SerialName("fechaSolicitud") val fechaSolicitud: String = "",
    @SerialName("fechaResolucion") val fechaResolucion: String? = null
)
