package com.htogo.app.data.api

import com.htogo.app.data.dto.*
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody

// APIs REST del backend con Ktor, comunes a Android e iOS. Mismos nombres de métodos que las
// interfaces de Retrofit que reemplazan.

class AuthApi(private val http: HttpClient) {
    suspend fun registrar(request: RegistroRequest): Respuesta<RegistroResponse> =
        http.post("auth/registro") { setBody(request) }.aRespuesta()
    suspend fun verificarTelefono(request: OtpRequest): Respuesta<Unit> =
        http.post("auth/verificacion-telefono") { setBody(request) }.aRespuesta()
    suspend fun reenviarOtp(request: ReenviarOtpRequest): Respuesta<Unit> =
        http.post("auth/verificacion-telefono/reenviar") { setBody(request) }.aRespuesta()
    suspend fun login(request: LoginRequest): Respuesta<SesionResponse> =
        http.post("auth/login") { setBody(request) }.aRespuesta()
    suspend fun logout(): Respuesta<Unit> = http.post("auth/logout").aRespuesta()
    suspend fun verificarSesion(): Respuesta<Unit> = http.get("usuarios/me").aRespuesta()
    suspend fun perfil(): Respuesta<PerfilMeDto> = http.get("usuarios/me").aRespuesta()
    /** Token FCM/APNs del dispositivo para recibir notificaciones con la app cerrada. */
    suspend fun registrarDispositivo(body: Map<String, String>): Respuesta<Unit> =
        http.put("usuarios/me/dispositivo") { setBody(body) }.aRespuesta()
}

class PedidosApi(private val http: HttpClient) {
    suspend fun crearPedido(request: PedidoCreateRequest): Respuesta<PedidoResponse> =
        http.post("pedidos") { setBody(request) }.aRespuesta()
    suspend fun obtenerDetallePedido(id: Int): Respuesta<PedidoResponse> = http.get("pedidos/$id").aRespuesta()
    /** Última posición del repartidor (CU-006). 204 si el pedido no está en ruta o aún no hay posición. */
    suspend fun ubicacionRepartidor(id: Int): Respuesta<UbicacionRepartidorDto> =
        http.get("pedidos/$id/ubicacion-repartidor").aRespuesta()
    /** Ruta por calles del repartidor al domicilio (CU-006); 422 si va fuera de Benito Juárez. */
    suspend fun rutaSeguimiento(id: Int): Respuesta<RouteResponse> =
        http.get("pedidos/$id/ruta-seguimiento").aRespuesta()
    suspend fun cancelarPedido(id: Int, request: CancelacionRequest? = null): Respuesta<PedidoResponse> =
        http.post("pedidos/$id/cancelacion") { request?.let { setBody(it) } }.aRespuesta()
    suspend fun obtenerHistorialCliente(page: Int = 0, size: Int = 20): Respuesta<PagedResponseDto<PedidoResumenDto>> =
        http.get("clientes/me/pedidos") { parameter("page", page); parameter("size", size) }.aRespuesta()
}

class DireccionesApi(private val http: HttpClient) {
    suspend fun listarDirecciones(): Respuesta<List<DireccionResponse>> = http.get("clientes/me/direcciones").aRespuesta()
    suspend fun crearDireccion(request: DireccionRequest): Respuesta<DireccionResponse> =
        http.post("clientes/me/direcciones") { setBody(request) }.aRespuesta()
    suspend fun actualizarDireccion(id: Int, request: DireccionRequest): Respuesta<DireccionResponse> =
        http.put("clientes/me/direcciones/$id") { setBody(request) }.aRespuesta()
    suspend fun eliminarDireccion(id: Int): Respuesta<Unit> = http.delete("clientes/me/direcciones/$id").aRespuesta()
}

class NegociosApi(private val http: HttpClient) {
    suspend fun listarMarcas(): Respuesta<List<MarcaResponse>> = http.get("marcas").aRespuesta()
    suspend fun buscarCercanos(cerca: String? = null, limite: Int = 50): Respuesta<List<NegocioCercanoResponse>> =
        http.get("negocios") { cerca?.let { parameter("cerca", it) }; parameter("limite", limite) }.aRespuesta()
    suspend fun obtenerPerfilNegocio(id: Int): Respuesta<PerfilNegocioResponse> = http.get("negocios/$id/perfil").aRespuesta()
    suspend fun obtenerMiNegocio(): Respuesta<PerfilNegocioResponse> = http.get("negocios/me").aRespuesta()
    suspend fun actualizarPrecio(id: Int, request: PrecioRequest): Respuesta<ProductoNegocioDto> =
        http.put("negocios/me/productos/$id/precio") { setBody(request) }.aRespuesta()
    /** RF-031: horario semanal (7 días); solo el dueño, se aplica al instante. */
    suspend fun actualizarHorarios(dias: List<HorarioNegocioDto>): Respuesta<List<HorarioNegocioDto>> =
        http.put("negocios/me/horarios") { setBody(dias) }.aRespuesta()
}

class RepartidorApi(private val http: HttpClient) {
    suspend fun obtenerPedidosDisponibles(): Respuesta<List<PedidoDisponibleResponse>> =
        http.get("repartidores/me/pedidos-disponibles").aRespuesta()
    suspend fun aceptarPedido(id: Int): Respuesta<PedidoResponse> = http.post("pedidos/$id/aceptacion").aRespuesta()
    suspend fun marcarEnCamino(id: Int, ubicacion: Map<String, Double>): Respuesta<PedidoResponse> =
        http.post("pedidos/$id/en-camino") { setBody(ubicacion) }.aRespuesta()
    suspend fun cargarApartadosBase(id: Int): Respuesta<PedidoResponse> =
        http.post("pedidos/$id/cargar-apartados-base").aRespuesta()
    suspend fun finalizarEntrega(id: Int, resultado: ResultadoEntregaRequestDto): Respuesta<PedidoResponse> =
        http.post("pedidos/$id/resultado") { setBody(resultado) }.aRespuesta()
    suspend fun reportarUbicacion(ubicacion: Map<String, Double>): Respuesta<Unit> =
        http.put("repartidores/me/ubicacion") { setBody(ubicacion) }.aRespuesta()
    suspend fun obtenerRuta(id: Int): Respuesta<RouteResponse> = http.get("pedidos/$id/ruta").aRespuesta()
    suspend fun obtenerMisEntregas(page: Int = 0, size: Int = 20): Respuesta<PagedResponseDto<PedidoResumenDto>> =
        http.get("repartidores/me/entregas") { parameter("page", page); parameter("size", size) }.aRespuesta()
    suspend fun iniciarJornada(request: IniciarJornadaRequest): Respuesta<JornadaResponse> =
        http.post("repartidores/me/jornada") { setBody(request) }.aRespuesta()
}

class InventarioApi(private val http: HttpClient) {
    suspend fun obtenerInventarioBase(): Respuesta<InventarioBaseResponse> = http.get("inventario/base").aRespuesta()
    suspend fun obtenerInventarioVehiculo(): Respuesta<InventarioVehiculoResponse> = http.get("inventario/vehiculo").aRespuesta()
    suspend fun cargarVehiculo(request: CargaVehiculoRequest): Respuesta<InventarioVehiculoResponse> =
        http.post("inventario/carga-vehiculo") { setBody(request) }.aRespuesta()
    suspend fun registrarLote(request: LoteEntradaRequest): Respuesta<LoteResponse> =
        http.post("inventario/lotes") { setBody(request) }.aRespuesta()
    suspend fun devolverABase(): Respuesta<Unit> = http.post("inventario/devolucion").aRespuesta()
    suspend fun registrarSalidaManual(request: SalidaManualRequest): Respuesta<Unit> =
        http.post("inventario/salida-manual") { setBody(request) }.aRespuesta()
}

/** Solicitudes de cambio del negocio (CU-020). Rol REPARTIDOR; crear y cancelar exige ser dueño. */
class SolicitudesApi(private val http: HttpClient) {
    suspend fun crear(request: SolicitudRequest): Respuesta<SolicitudResponse> =
        http.post("solicitudes") { setBody(request) }.aRespuesta()
    suspend fun misSolicitudes(): Respuesta<List<SolicitudResponse>> = http.get("solicitudes").aRespuesta()
    /** 204 si se retiró; 409 si el admin ya la resolvió. */
    suspend fun cancelar(id: Int): Respuesta<Unit> = http.delete("solicitudes/$id").aRespuesta()
}
