package com.htogo.app.data.api

import com.htogo.shared.data.jsonApi
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.serializer

/**
 * Resultado de una llamada al backend. Imita la API de `retrofit2.Response` (isSuccessful,
 * body(), code(), errorBody()?.string()) para que los ViewModels no cambien al pasar a Ktor.
 */
class Respuesta<T>(private val codigo: Int, private val cuerpo: T?, private val textoError: String?) {
    val isSuccessful: Boolean get() = codigo in 200..299
    fun body(): T? = cuerpo
    fun code(): Int = codigo
    fun errorBody(): CuerpoError? = textoError?.let(::CuerpoError)
}

class CuerpoError(private val texto: String) {
    fun string(): String = texto
}

/** Convierte la respuesta HTTP: cuerpo tipado si fue exitosa, texto del error si no. */
@PublishedApi
internal suspend inline fun <reified T> HttpResponse.aRespuesta(): Respuesta<T> {
    val codigo = status.value
    if (codigo !in 200..299) return Respuesta(codigo, null, bodyAsText())
    if (T::class == Unit::class) {
        @Suppress("UNCHECKED_CAST")
        return Respuesta(codigo, Unit as T, null)
    }
    val texto = bodyAsText()
    if (texto.isBlank()) return Respuesta(codigo, null, null) // p. ej. 204 sin contenido
    return Respuesta(codigo, jsonApi.decodeFromString(serializer<T>(), texto), null)
}
