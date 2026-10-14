package com.htogo.shared.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** URL del backend en Azure (la misma que usa la app Android). */
const val API_AZURE = "https://h2togo-api.orangecliff-485fd1d3.eastus.azurecontainerapps.io/api/v1/"

val jsonApi = Json {
    ignoreUnknownKeys = true // el backend puede agregar campos sin romper la app
    explicitNulls = false
}

/**
 * Cliente HTTP común (Ktor). Sin [engine] usa el de cada plataforma: OkHttp en Android y
 * NSURLSession en iOS. [token] se consulta en cada petición para mandar la sesión vigente.
 */
fun crearClienteHttp(
    baseUrl: String = API_AZURE,
    token: () -> String? = { null },
    engine: HttpClientEngine? = null
): HttpClient {
    val config: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
        expectSuccess = false
        install(ContentNegotiation) { json(jsonApi) }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 30_000
        }
        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
            token()?.let { headers.append(HttpHeaders.Authorization, "Bearer $it") }
        }
    }
    return if (engine != null) HttpClient(engine, config) else HttpClient(config)
}
