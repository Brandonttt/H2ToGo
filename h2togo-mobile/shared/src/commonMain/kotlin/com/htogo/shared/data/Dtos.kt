package com.htogo.shared.data

import kotlinx.serialization.Serializable

/** Cuerpo de error del backend: {"codigo": "...", "mensaje": "..."}. */
@Serializable
data class ErrorDto(val codigo: String? = null, val mensaje: String? = null)

/** Mensaje legible de un cuerpo de error del backend, o null si no trae uno. */
fun mensajeDeError(cuerpo: String?): String? =
    cuerpo?.let { runCatching { jsonApi.decodeFromString(ErrorDto.serializer(), it).mensaje }.getOrNull() }
