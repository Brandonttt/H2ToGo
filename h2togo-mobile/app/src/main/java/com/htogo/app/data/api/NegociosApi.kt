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
        @Query("cerca") cerca: String, // "lat,lon"
        @Query("limite") limite: Int = 10
    ): Response<List<NegocioCercanoResponse>>

    @GET("negocios/{id}/perfil")
    suspend fun obtenerPerfilNegocio(@Path("id") id: Int): Response<PerfilNegocioResponse>
}
