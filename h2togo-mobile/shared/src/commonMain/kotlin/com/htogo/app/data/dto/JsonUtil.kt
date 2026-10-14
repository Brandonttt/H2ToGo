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
