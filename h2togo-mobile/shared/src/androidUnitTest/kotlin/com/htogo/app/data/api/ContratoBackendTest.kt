package com.htogo.app.data.api

import com.htogo.app.data.dto.LoginRequest
import com.htogo.app.data.realtime.EventoStomp
import com.htogo.app.data.realtime.StompCliente
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import com.htogo.shared.data.crearClienteHttp
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Contrato con el backend real: cada endpoint de lectura que usa la app debe decodificarse con
 * los DTOs compartidos. Corre solo si se define H2TOGO_API (p. ej. el backend local de Docker):
 *   H2TOGO_API=http://localhost:8088/api/v1/ ./gradlew :shared:testDebugUnitTest
 * Usa los usuarios de data-demo.sql y fuerza la sesión (cierra la que tengan abierta).
 */
class ContratoBackendTest {

    private val base: String? = System.getenv("H2TOGO_API")

    private fun apis(token: () -> String?) = crearClienteHttp(base!!, token = token)

    private suspend fun login(correo: String): String {
        val r = AuthApi(apis { null }).login(LoginRequest(correo, "Demo1234", forzar = true))
        return r.body()?.token ?: fail("login $correo: ${r.code()} ${r.errorBody()?.string()}")
    }

    /** Ejecuta la llamada; falla si el JSON no se pudo decodificar (lo demás se reporta). */
    private suspend fun revisar(nombre: String, errores: MutableList<String>, llamada: suspend () -> Respuesta<*>) {
        try {
            val r = llamada()
            println("[contrato] $nombre -> ${r.code()}" + (r.errorBody()?.let { " ${it.string().take(120)}" } ?: ""))
        } catch (e: Exception) {
            errores += "$nombre: ${e::class.simpleName}: ${e.message?.take(300)}"
        }
    }

    @Test
    fun endpointsDeClienteYRepartidorSeDecodifican() = runBlocking {
        if (base == null) { println("[contrato] H2TOGO_API no definido: se omite"); return@runBlocking }
        val errores = mutableListOf<String>()

        val tokenCli = login("cliente@h2togo.mx")
        val c = apis { tokenCli }
        val pedidos = PedidosApi(c)
        revisar("perfil", errores) { AuthApi(c).perfil() }
        revisar("direcciones", errores) { DireccionesApi(c).listarDirecciones() }
        revisar("marcas", errores) { NegociosApi(c).listarMarcas() }
        revisar("negocios cercanos", errores) { NegociosApi(c).buscarCercanos() }
        revisar("negocios cercanos (con ubicación)", errores) { NegociosApi(c).buscarCercanos("19.3790,-99.1640") }
        val cercanos = NegociosApi(c).buscarCercanos().body().orEmpty()
        cercanos.firstOrNull()?.let { n -> revisar("perfil negocio ${n.id}", errores) { NegociosApi(c).obtenerPerfilNegocio(n.id) } }
        revisar("historial cliente", errores) { pedidos.obtenerHistorialCliente() }
        val hist = pedidos.obtenerHistorialCliente().body()?.elementos.orEmpty()
        hist.take(5).forEach { p ->
            revisar("detalle pedido ${p.id}", errores) { pedidos.obtenerDetallePedido(p.id) }
            revisar("ubicación repartidor ${p.id}", errores) { pedidos.ubicacionRepartidor(p.id) }
        }

        val tokenRep = login("dueno@h2togo.mx")
        val r = apis { tokenRep }
        revisar("perfil repartidor", errores) { AuthApi(r).perfil() }
        revisar("mi negocio", errores) { NegociosApi(r).obtenerMiNegocio() }
        revisar("pedidos disponibles", errores) { RepartidorApi(r).obtenerPedidosDisponibles() }
        revisar("mis entregas", errores) { RepartidorApi(r).obtenerMisEntregas() }
        RepartidorApi(r).obtenerMisEntregas().body()?.elementos.orEmpty().take(5).forEach { p ->
            revisar("detalle entrega ${p.id}", errores) { PedidosApi(r).obtenerDetallePedido(p.id) }
        }
        revisar("inventario base", errores) { InventarioApi(r).obtenerInventarioBase() }
        revisar("inventario vehículo", errores) { InventarioApi(r).obtenerInventarioVehiculo() }
        revisar("solicitudes", errores) { SolicitudesApi(r).misSolicitudes() }

        assertTrue(errores.isEmpty(), "Respuestas que no coinciden con los DTOs:\n" + errores.joinToString("\n"))
    }

    @Test
    fun webSocketStompConectaYSuscribe() = runBlocking {
        if (base == null) return@runBlocking
        StompCliente.baseUrl = base
        val token = login("cliente@h2togo.mx")
        val primero = withTimeout(10_000) { StompCliente.suscribir(StompCliente.COLA_NOTIFICACIONES, token).first() }
        println("[contrato] STOMP -> $primero")
        assertTrue(primero is EventoStomp.Conectado, "Se esperaba CONNECTED, llegó $primero")
    }
}
