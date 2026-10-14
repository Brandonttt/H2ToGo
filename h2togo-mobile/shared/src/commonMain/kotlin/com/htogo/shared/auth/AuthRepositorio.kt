package com.htogo.shared.auth

import com.htogo.shared.data.ErrorDto
import com.htogo.shared.data.LoginRequestDto
import com.htogo.shared.data.SesionDto
import com.htogo.shared.data.jsonApi
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

sealed interface ResultadoLogin {
    data class Exito(val sesion: SesionDto) : ResultadoLogin
    /** RF-016: ya hay una sesión abierta en otro dispositivo; se puede forzar. */
    data object SesionActiva : ResultadoLogin
    data class Error(val mensaje: String) : ResultadoLogin
}

/** Inicio de sesión (CU-002) contra el backend, compartido entre Android e iOS. */
class AuthRepositorio(private val http: HttpClient) {

    suspend fun login(correo: String, password: String, forzar: Boolean = false): ResultadoLogin = try {
        val resp = http.post("auth/login") { setBody(LoginRequestDto(correo.trim(), password, forzar)) }
        when {
            resp.status.isSuccess() -> ResultadoLogin.Exito(resp.body())
            resp.status == HttpStatusCode.Conflict -> ResultadoLogin.SesionActiva
            resp.status == HttpStatusCode.Unauthorized ->
                ResultadoLogin.Error("Credenciales inválidas. Verifica tu correo y contraseña.")
            else -> ResultadoLogin.Error(mensajeDeError(resp.bodyAsText())
                ?: "Error al iniciar sesión (${resp.status.value})")
        }
    } catch (e: Exception) {
        ResultadoLogin.Error("Error de conexión con el servidor")
    }

    private fun mensajeDeError(cuerpo: String): String? =
        runCatching { jsonApi.decodeFromString(ErrorDto.serializer(), cuerpo).mensaje }.getOrNull()
}
