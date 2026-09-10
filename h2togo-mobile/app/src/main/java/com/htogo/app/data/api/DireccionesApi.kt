package com.htogo.app.data.api

import com.htogo.app.data.dto.DireccionRequest
import com.htogo.app.data.dto.DireccionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.PUT

interface DireccionesApi {

    @GET("clientes/me/direcciones")
    suspend fun listarDirecciones(): Response<List<DireccionResponse>>

    @POST("clientes/me/direcciones")
    suspend fun crearDireccion(@Body request: DireccionRequest): Response<DireccionResponse>

    @PUT("clientes/me/direcciones/{id}")
    suspend fun actualizarDireccion(
        @Path("id") id: Int,
        @Body request: DireccionRequest
    ): Response<DireccionResponse>

    @DELETE("clientes/me/direcciones/{id}")
    suspend fun eliminarDireccion(@Path("id") id: Int): Response<Unit>
}
