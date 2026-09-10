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

        return chain.proceed(requestBuilder.build())
    }
}
