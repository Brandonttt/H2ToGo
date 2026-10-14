package com.htogo.shared.data

import kotlinx.serialization.Serializable

/** Mismo contrato que POST /auth/login del backend (ver AuthDto.kt de la app Android). */
@Serializable
data class LoginRequestDto(
    val correo: String,
    val password: String,
    val forzar: Boolean = false,
    val tokenFcm: String? = null
)

@Serializable
data class SesionDto(
    val token: String,
    val expiraEn: String? = null,
    val rol: String,
    val suspendidoHasta: String? = null,
    val perfil: PerfilDto
)

@Serializable
data class PerfilDto(
    val idUsuario: Int,
    val correo: String,
    val nombre: String,
    val apellidos: String = "",
    val telefono: String = "",
    val rol: String
)

/** Cuerpo de error del backend: {"codigo": "...", "mensaje": "..."}. */
@Serializable
data class ErrorDto(val codigo: String? = null, val mensaje: String? = null)
