package com.htogo.app.data.api

import com.htogo.app.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor de OkHttp que inyecta automáticamente el header
 * Authorization: Bearer <token> en todas las peticiones que lo requieran.
 */
class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = sessionManager.obtenerToken()

        val requestBuilder = originalRequest.newBuilder()

        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val response = chain.proceed(requestBuilder.build())

        // Si el backend responde 401 Unauthorized y la app contaba con token activo
        if (response.code == 401 && !token.isNullOrBlank()) {
            val path = originalRequest.url.encodedPath
            // Evitar interceptar el login o registro (donde 401 son credenciales incorrectas)
            if (!path.contains("/auth/login") && !path.contains("/auth/registro")) {
                sessionManager.notificarSesionExpirada()
            }
        }

        return response
    }
}
