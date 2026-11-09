package com.htogo.shared.data

import kotlinx.serialization.Serializable

/** Cuerpo de error del backend: {"codigo": "...", "mensaje": "...", "detalles": ["campo: motivo"]}. */
@Serializable
data class ErrorDto(
    val codigo: String? = null,
    val mensaje: String? = null,
    val detalles: List<String> = emptyList(),
)

/**
 * Mensaje legible de un cuerpo de error del backend, o null si no trae uno. Los errores de
 * validación ("Datos de la petición inválidos.") se traducen al campo que falló.
 */
fun mensajeDeError(cuerpo: String?): String? {
    val error = cuerpo?.let { runCatching { jsonApi.decodeFromString(ErrorDto.serializer(), it) }.getOrNull() }
        ?: return null
    if (error.codigo == "VALIDACION") {
        val campo = error.detalles.firstOrNull()?.substringBefore(":")?.trim()?.lowercase().orEmpty()
        return when {
            "correo" in campo || "email" in campo -> "Escribe un correo válido."
            "password" in campo || "contrasena" in campo -> "Revisa tu contraseña."
            "telefono" in campo -> "Revisa tu número de teléfono."
            "codigo" in campo -> "Revisa el código."
            else -> "Revisa los datos e intenta de nuevo."
        }
    }
    return error.mensaje
}

/** Código estructurado de un cuerpo de error del backend (ej. "TELEFONO_NO_VERIFICADO"), o null. */
fun codigoDeError(cuerpo: String?): String? =
    cuerpo?.let { runCatching { jsonApi.decodeFromString(ErrorDto.serializer(), it).codigo }.getOrNull() }
