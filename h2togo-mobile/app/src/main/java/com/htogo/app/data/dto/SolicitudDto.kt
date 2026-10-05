package com.htogo.app.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Solicitud de cambio de perfil del negocio (CU-020). Solo el dueño puede crearlas; el admin
 * las aprueba o rechaza desde el panel web (RN-020).
 */
data class SolicitudRequest(
    @SerializedName("codigoCambio") val codigoCambio: String,
    @SerializedName("idVehiculo") val idVehiculo: Int? = null,
    @SerializedName("idProductoNegocio") val idProductoNegocio: Int? = null,
    @SerializedName("valorNuevo") val valorNuevo: Map<String, Any>
)

data class SolicitudResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("codigoCambio") val codigoCambio: String,
    @SerializedName("idNegocio") val idNegocio: Int,
    @SerializedName("idVehiculo") val idVehiculo: Int?,
    @SerializedName("idProductoNegocio") val idProductoNegocio: Int?,
    @SerializedName("valorAnterior") val valorAnterior: String?,
    /** JSON con los datos propuestos; su forma depende de [codigoCambio]. */
    @SerializedName("valorNuevo") val valorNuevo: String,
    /** pendiente | aprobado | rechazado */
    @SerializedName("estado") val estado: String,
    @SerializedName("comentarioAdmin") val comentarioAdmin: String?,
    @SerializedName("fechaSolicitud") val fechaSolicitud: String,
    @SerializedName("fechaResolucion") val fechaResolucion: String?
)
