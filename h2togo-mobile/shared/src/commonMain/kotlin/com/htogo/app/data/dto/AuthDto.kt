@file:OptIn(ExperimentalSerializationApi::class)

package com.htogo.app.data.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class LoginRequest(
    @SerialName("correo") val correo: String,
    @SerialName("password") val password: String,
    @SerialName("tokenFcm") val tokenFcm: String? = null,
    @SerialName("forzar") val forzar: Boolean = false
)

@Serializable
data class SesionResponse(
    @SerialName("token") val token: String = "",
    @SerialName("expiraEn") val expiraEn: String? = null,
    @SerialName("rol") val rol: String = "",
    @SerialName("suspendidoHasta") val suspendidoHasta: String? = null,
    @SerialName("perfil") val perfil: PerfilResponse
)

@Serializable
data class PerfilResponse(
    // El backend manda "id" (PerfilResponse); se acepta "idUsuario" por compatibilidad.
    @SerialName("id") @JsonNames("idUsuario") val idUsuario: Int = 0,
    @SerialName("correo") val correo: String = "",
    @SerialName("nombre") val nombre: String = "",
    @SerialName("apellidos") val apellidos: String = "",
    @SerialName("telefono") val telefono: String = "",
    @SerialName("rol") val rol: String = ""
)

@Serializable
data class RegistroRequest(
    @SerialName("nombre") val nombre: String,
    @SerialName("apellidos") val apellidos: String,
    @SerialName("correo") val correo: String,
    @SerialName("password") val password: String,
    @SerialName("telefono") val telefono: String,
    @SerialName("rol") val rol: String = "cliente",
    @SerialName("negocio") val negocio: RegistroNegocioRequest? = null
)

@Serializable
data class RegistroNegocioRequest(
    @SerialName("idExistente") val idExistente: Int? = null,
    @SerialName("nombreComercial") val nombreComercial: String? = null,
    @SerialName("calle") val calle: String? = null,
    @SerialName("numeroExterior") val numeroExterior: String? = null,
    @SerialName("colonia") val colonia: String? = null,
    @SerialName("codigoPostal") val codigoPostal: String? = null,
    @SerialName("referencias") val referencias: String? = null,
    @SerialName("lat") val lat: Double? = null,
    @SerialName("lon") val lon: Double? = null,
    @SerialName("horarioApertura") val horarioApertura: String? = null,
    @SerialName("horarioCierre") val horarioCierre: String? = null
)

@Serializable
data class RegistroResponse(
    @SerialName("idUsuario") val idUsuario: Int = 0,
    @SerialName("correo") val correo: String = "",
    @SerialName("telefono") val telefono: String = "",
    @SerialName("telefonoVerificado") val telefonoVerificado: Boolean = false,
    @SerialName("idNegocio") val idNegocio: Int? = null,
    @SerialName("rol") val rol: String? = null,
    @SerialName("mensaje") val mensaje: String? = null
)

@Serializable
data class OtpRequest(
    @SerialName("correo") val correo: String,
    @SerialName("codigo") val codigo: String
)

@Serializable
data class ReenviarOtpRequest(
    @SerialName("correo") val correo: String
)

/** GET /usuarios/me: solo los campos que usa la app. */
@Serializable
data class PerfilMeDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("rol") val rol: String = "",
    @SerialName("repartidor") val repartidor: RepartidorInfoDto? = null
)

@Serializable
data class RepartidorInfoDto(
    @SerialName("idNegocio") val idNegocio: Int? = null,
    @SerialName("estadoOperativo") val estadoOperativo: Boolean = false,
    @SerialName("idVehiculoActual") val idVehiculoActual: Int? = null,
    @SerialName("esDueno") val esDueno: Boolean = false,
    // false: se unió a una purificadora existente y espera la aprobación del dueño.
    @SerialName("aprobado") val aprobado: Boolean = true
)

/** Recuperar contraseña (paso 1): se manda un código por SMS al teléfono de la cuenta. */
@Serializable
data class RecuperacionRequest(
    @SerialName("correo") val correo: String
)

/** Recuperar contraseña (paso 2): código del SMS y contraseña nueva. */
@Serializable
data class RecuperacionConfirmacionRequest(
    @SerialName("correo") val correo: String,
    @SerialName("codigo") val codigo: String,
    @SerialName("nuevaContrasena") val nuevaContrasena: String
)

/** Cambiar contraseña con la sesión iniciada. */
@Serializable
data class CambioContrasenaRequest(
    @SerialName("actual") val actual: String,
    @SerialName("nueva") val nueva: String
)

/** Repartidor de la purificadora visto por su dueño (GET negocios/me/repartidores). */
@Serializable
data class MiembroNegocioDto(
    @SerialName("idUsuario") val idUsuario: Int = 0,
    @SerialName("nombre") val nombre: String = "",
    @SerialName("apellidos") val apellidos: String = "",
    @SerialName("correo") val correo: String = "",
    @SerialName("telefono") val telefono: String = "",
    @SerialName("esDueno") val esDueno: Boolean = false,
    @SerialName("aprobado") val aprobado: Boolean = true,
    @SerialName("enLinea") val enLinea: Boolean = false
)
