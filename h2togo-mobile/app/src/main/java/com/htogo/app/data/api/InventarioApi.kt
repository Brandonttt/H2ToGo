package com.htogo.app.data.api

import com.htogo.app.data.dto.CargaVehiculoRequest
import com.htogo.app.data.dto.InventarioBaseResponse
import com.htogo.app.data.dto.InventarioVehiculoResponse
import com.htogo.app.data.dto.LoteEntradaRequest
import com.htogo.app.data.dto.LoteResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface InventarioApi {

    @GET("inventario/base")
    suspend fun obtenerInventarioBase(): Response<InventarioBaseResponse>

    @GET("inventario/vehiculo")
    suspend fun obtenerInventarioVehiculo(): Response<InventarioVehiculoResponse>

    @POST("inventario/carga-vehiculo")
    suspend fun cargarVehiculo(@Body request: CargaVehiculoRequest): Response<InventarioVehiculoResponse>

    @POST("inventario/lotes")
    suspend fun registrarLote(@Body request: LoteEntradaRequest): Response<LoteResponse>

    @POST("inventario/devolucion")
    suspend fun devolverABase(): Response<Unit>
}
