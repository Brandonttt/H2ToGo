package com.htogo.app.data.api

import com.htogo.app.data.dto.LoginRequest
import com.htogo.app.data.dto.OtpRequest
import com.htogo.app.data.dto.ReenviarOtpRequest
import com.htogo.app.data.dto.RegistroRequest
import com.htogo.app.data.dto.RegistroResponse
import com.htogo.app.data.dto.SesionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/registro")
    suspend fun registrar(@Body request: RegistroRequest): Response<RegistroResponse>

    @POST("auth/verificacion-telefono")
    suspend fun verificarTelefono(@Body request: OtpRequest): Response<Unit>

    @POST("auth/verificacion-telefono/reenviar")
    suspend fun reenviarOtp(@Body request: ReenviarOtpRequest): Response<Unit>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<SesionResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>
}
