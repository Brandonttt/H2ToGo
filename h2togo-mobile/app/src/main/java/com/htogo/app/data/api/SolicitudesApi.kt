package com.htogo.app.data.api

import com.htogo.app.data.dto.SolicitudRequest
import com.htogo.app.data.dto.SolicitudResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** Solicitudes de cambio del negocio (CU-020). Rol REPARTIDOR; crear y cancelar exige ser dueño. */
interface SolicitudesApi {

    @POST("solicitudes")
    suspend fun crear(@Body request: SolicitudRequest): Response<SolicitudResponse>

    @GET("solicitudes")
    suspend fun misSolicitudes(): Response<List<SolicitudResponse>>

    /** 204 si se retiró; 409 si el admin ya la resolvió. */
    @DELETE("solicitudes/{id}")
    suspend fun cancelar(@Path("id") id: Int): Response<Unit>
}
