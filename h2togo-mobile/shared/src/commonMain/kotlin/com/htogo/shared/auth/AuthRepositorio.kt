package com.htogo.shared.auth

import com.htogo.app.data.api.AuthApi
import com.htogo.app.data.dto.LoginRequest
import com.htogo.app.data.dto.SesionResponse
import com.htogo.shared.data.mensajeDeError

sealed interface ResultadoLogin {
    data class Exito(val sesion: SesionResponse) : ResultadoLogin
    /** RF-016: ya hay una sesión abierta en otro dispositivo; se puede forzar. */
    data object SesionActiva : ResultadoLogin
    data class Error(val mensaje: String) : ResultadoLogin
}

/** Inicio de sesión (CU-002) contra el backend, compartido entre Android e iOS. */
class AuthRepositorio(private val api: AuthApi) {

    suspend fun login(correo: String, password: String, forzar: Boolean = false): ResultadoLogin = try {
        val resp = api.login(LoginRequest(correo.trim(), password, forzar = forzar))
        val sesion = resp.body()
        when {
            resp.isSuccessful && sesion != null -> ResultadoLogin.Exito(sesion)
            resp.code() == 409 -> ResultadoLogin.SesionActiva
            resp.code() == 401 -> ResultadoLogin.Error("Credenciales inválidas. Verifica tu correo y contraseña.")
            else -> ResultadoLogin.Error(mensajeDeError(resp.errorBody()?.string())
                ?: "Error al iniciar sesión (${resp.code()})")
        }
    } catch (e: Exception) {
        ResultadoLogin.Error("Error de conexión con el servidor")
    }
}
