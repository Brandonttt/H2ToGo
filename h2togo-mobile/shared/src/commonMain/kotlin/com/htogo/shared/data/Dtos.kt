package com.htogo.shared.data

import kotlinx.serialization.Serializable

/** Cuerpo de error del backend: {"codigo": "...", "mensaje": "..."}. */
@Serializable
data class ErrorDto(val codigo: String? = null, val mensaje: String? = null)

/** Mensaje legible de un cuerpo de error del backend, o null si no trae uno. */
fun mensajeDeError(cuerpo: String?): String? =
    cuerpo?.let { runCatching { jsonApi.decodeFromString(ErrorDto.serializer(), it).mensaje }.getOrNull() }

/** Código estructurado de un cuerpo de error del backend (ej. "TELEFONO_NO_VERIFICADO"), o null. */
fun codigoDeError(cuerpo: String?): String? =
    cuerpo?.let { runCatching { jsonApi.decodeFromString(ErrorDto.serializer(), it).codigo }.getOrNull() }
