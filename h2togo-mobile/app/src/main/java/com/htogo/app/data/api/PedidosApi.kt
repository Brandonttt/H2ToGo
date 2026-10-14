package com.htogo.app.data.api

import com.htogo.app.data.dto.CancelacionRequest
import com.htogo.app.data.dto.PagedResponseDto
import com.htogo.app.data.dto.PedidoCreateRequest
import com.htogo.app.data.dto.PedidoResponse
import com.htogo.app.data.dto.PedidoResumenDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

interface PedidosApi {

    @POST("pedidos")
    suspend fun crearPedido(@Body request: PedidoCreateRequest): Response<PedidoResponse>

    @GET("pedidos/{id}")
    suspend fun obtenerDetallePedido(@Path("id") id: Int): Response<PedidoResponse>

    /** Última posición del repartidor (CU-006). 204 si el pedido no está en ruta o aún no hay posición. */
    @GET("pedidos/{id}/ubicacion-repartidor")
    suspend fun ubicacionRepartidor(@Path("id") id: Int): Response<com.htogo.app.data.dto.UbicacionRepartidorDto>

    /** Ruta por calles del repartidor a mi domicilio (CU-006); 422 si va en camino fuera de Benito Juárez. */
    @GET("pedidos/{id}/ruta-seguimiento")
    suspend fun rutaSeguimiento(@Path("id") id: Int): Response<com.htogo.app.data.dto.RouteResponse>

    @POST("pedidos/{id}/cancelacion")
    suspend fun cancelarPedido(
        @Path("id") id: Int,
        @Body request: CancelacionRequest? = null
    ): Response<PedidoResponse>

    @GET("clientes/me/pedidos")
    suspend fun obtenerHistorialCliente(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PagedResponseDto<PedidoResumenDto>>
}
