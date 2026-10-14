package com.htogo.shared.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** URL del backend en Azure (la misma que usa la app Android). */
const val API_AZURE = "https://h2togo-api.orangecliff-485fd1d3.eastus.azurecontainerapps.io/api/v1/"

val jsonApi = Json {
    ignoreUnknownKeys = true  // el backend puede agregar campos sin romper la app
    explicitNulls = false     // los null no se envían
    encodeDefaults = true     // los valores por defecto sí (p. ej. rol = "cliente")
    coerceInputValues = true  // un null del backend en un campo no nulo toma su valor por defecto
}

/**
 * Cliente HTTP común (Ktor). Sin [engine] usa el de cada plataforma: OkHttp en Android y
 * NSURLSession en iOS. [token] se consulta en cada petición para mandar la sesión vigente;
 * [alNoAutorizado] recibe la ruta de cada respuesta 401 enviada con token.
 */
fun crearClienteHttp(
    baseUrl: String = API_AZURE,
    token: () -> String? = { null },
    alNoAutorizado: (ruta: String) -> Unit = {},
    engine: HttpClientEngine? = null
): HttpClient {
    val config: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
        expectSuccess = false
        install(ContentNegotiation) { json(jsonApi) }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 30_000
        }
        install(createClientPlugin("SesionExpirada") {
            onResponse { resp ->
                if (resp.status == HttpStatusCode.Unauthorized &&
                    resp.call.request.headers[HttpHeaders.Authorization] != null
                ) alNoAutorizado(resp.call.request.url.encodedPath)
            }
        })
        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
            token()?.let { headers.append(HttpHeaders.Authorization, "Bearer $it") }
        }
    }
    return if (engine != null) HttpClient(engine, config) else HttpClient(config)
}
