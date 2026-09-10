package com.htogo.app.data.api

import com.htogo.app.data.dto.PedidoDisponibleResponse
import com.htogo.app.data.dto.PedidoResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.PUT

interface RepartidorApi {

    @GET("repartidores/me/pedidos-disponibles")
    suspend fun obtenerPedidosDisponibles(): Response<List<PedidoDisponibleResponse>>

    @POST("pedidos/{id}/aceptacion")
    suspend fun aceptarPedido(@Path("id") id: Int): Response<PedidoResponse>

    @POST("pedidos/{id}/en-camino")
    suspend fun marcarEnCamino(
        @Path("id") id: Int,
        @Body ubicacion: Map<String, Double>
    ): Response<PedidoResponse>

    @POST("pedidos/{id}/resultado")
    suspend fun finalizarEntrega(
        @Path("id") id: Int,
        @Body resultado: Map<String, Any>
    ): Response<PedidoResponse>

    @PUT("repartidores/me/ubicacion")
    suspend fun reportarUbicacion(@Body ubicacion: Map<String, Double>): Response<Unit>
}
