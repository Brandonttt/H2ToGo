package com.htogo.app.data.dto

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("correo") val correo: String,
    @SerializedName("password") val password: String,
    @SerializedName("tokenFcm") val tokenFcm: String? = null,
    @SerializedName("forzar") val forzar: Boolean = false
)

data class SesionResponse(
    @SerializedName("token") val token: String,
    @SerializedName("expiraEn") val expiraEn: String?,
    @SerializedName("rol") val rol: String,
    @SerializedName("suspendidoHasta") val suspendidoHasta: String?,
    @SerializedName("perfil") val perfil: PerfilResponse
)

data class PerfilResponse(
    @SerializedName("idUsuario") val idUsuario: Int,
    @SerializedName("correo") val correo: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("apellidos") val apellidos: String,
    @SerializedName("telefono") val telefono: String,
    @SerializedName("rol") val rol: String
)

data class RegistroRequest(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("apellidos") val apellidos: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("password") val password: String,
    @SerializedName("telefono") val telefono: String,
    @SerializedName("rol") val rol: String = "cliente",
    @SerializedName("negocio") val negocio: RegistroNegocioRequest? = null
)

data class RegistroNegocioRequest(
    @SerializedName("idExistente") val idExistente: Int? = null,
    @SerializedName("nombreComercial") val nombreComercial: String? = null
)

data class RegistroResponse(
    @SerializedName("idUsuario") val idUsuario: Int,
    @SerializedName("correo") val correo: String,
    @SerializedName("telefono") val telefono: String,
    @SerializedName("telefonoVerificado") val telefonoVerificado: Boolean = false,
    @SerializedName("idNegocio") val idNegocio: Int? = null,
    @SerializedName("rol") val rol: String? = null,
    @SerializedName("mensaje") val mensaje: String? = null
)

data class OtpRequest(
    @SerializedName("correo") val correo: String,
    @SerializedName("codigo") val codigo: String
)

data class ReenviarOtpRequest(
    @SerializedName("correo") val correo: String
)
