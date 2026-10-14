package com.htogo.app.data.dto

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Convierte un mapa de valores simples a JSON (para cuerpos libres como valorNuevo). */
fun Map<String, Any?>.aJsonObject(): JsonObject = JsonObject(mapValues { (_, v) -> v.aJson() })

private fun Any?.aJson(): JsonElement = when (this) {
    null -> JsonNull
    is JsonElement -> this
    is String -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is Map<*, *> -> JsonObject(entries.associate { (k, v) -> k.toString() to v.aJson() })
    is Iterable<*> -> JsonArray(map { it.aJson() })
    else -> JsonPrimitive(toString())
}

/**
 * Texto JSON de un objeto → mapa de valores simples (como lo hacía Gson: los números son Double).
 * Devuelve un mapa vacío si el texto no es un objeto JSON.
 */
fun String.aMapaJson(): Map<String, Any?> = runCatching {
    (com.htogo.shared.data.jsonApi.parseToJsonElement(this) as? JsonObject)?.mapValues { (_, v) -> v.aValorSimple() }
}.getOrNull() ?: emptyMap()

private fun JsonElement.aValorSimple(): Any? = when (this) {
    is JsonNull -> null
    is JsonPrimitive -> if (isString) content else content.toBooleanStrictOrNull() ?: content.toDoubleOrNull() ?: content
    is JsonObject -> mapValues { (_, v) -> v.aValorSimple() }
    is JsonArray -> map { it.aValorSimple() }
}
