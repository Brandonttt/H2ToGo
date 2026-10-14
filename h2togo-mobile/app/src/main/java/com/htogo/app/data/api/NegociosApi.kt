package com.htogo.app.data.api

import com.htogo.app.data.dto.MarcaResponse
import com.htogo.app.data.dto.NegocioCercanoResponse
import com.htogo.app.data.dto.PerfilNegocioResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface NegociosApi {

    @GET("marcas")
    suspend fun listarMarcas(): Response<List<MarcaResponse>>

    @GET("negocios")
    suspend fun buscarCercanos(
        @Query("cerca") cerca: String? = null,
        @Query("limite") limite: Int = 50
    ): Response<List<NegocioCercanoResponse>>

    @GET("negocios/{id}/perfil")
    suspend fun obtenerPerfilNegocio(@Path("id") id: Int): Response<PerfilNegocioResponse>

    @GET("negocios/me")
    suspend fun obtenerMiNegocio(): Response<PerfilNegocioResponse>

    @retrofit2.http.PUT("negocios/me/productos/{id}/precio")
    suspend fun actualizarPrecio(
        @Path("id") id: Int,
        @retrofit2.http.Body request: com.htogo.app.data.dto.PrecioRequest
    ): Response<com.htogo.app.data.dto.ProductoNegocioDto>
}
