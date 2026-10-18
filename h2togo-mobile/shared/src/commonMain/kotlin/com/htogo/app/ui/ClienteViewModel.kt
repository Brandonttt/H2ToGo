package com.htogo.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htogo.shared.data.mensajeDeError
import com.htogo.app.plataforma.ahoraMs
import com.htogo.app.plataforma.Plataforma
import com.htogo.app.data.api.ApiClient
import com.htogo.app.data.dto.CancelacionRequest
import com.htogo.app.data.dto.DireccionRequest
import com.htogo.app.data.dto.DireccionResponse
import com.htogo.app.data.dto.MarcaResponse
import com.htogo.app.data.dto.NegocioCercanoResponse
import com.htogo.app.data.dto.PedidoCreateRequest
import com.htogo.app.data.dto.PedidoResponse
import com.htogo.app.data.dto.PerfilNegocioResponse
import com.htogo.app.data.dto.PedidoResumenDto
import com.htogo.app.data.local.SessionManager
import com.htogo.app.data.notificaciones.CanalNotificaciones
import com.htogo.app.data.realtime.EventoStomp
import com.htogo.app.data.realtime.StompCliente
import kotlinx.coroutines.Job
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClienteViewModel : ViewModel() {

    private val apiClient = ApiClient.getInstance()
    private val sessionManager = SessionManager.getInstance()

    // Catálogos
    private val _marcas = MutableStateFlow<List<MarcaResponse>>(emptyList())
    val marcas: StateFlow<List<MarcaResponse>> = _marcas.asStateFlow()

    private val _direcciones = MutableStateFlow<List<DireccionResponse>>(emptyList())
    val direcciones: StateFlow<List<DireccionResponse>> = _direcciones.asStateFlow()

    private val _purificadoras = MutableStateFlow<List<NegocioCercanoResponse>>(emptyList())
    val purificadoras: StateFlow<List<NegocioCercanoResponse>> = _purificadoras.asStateFlow()

    private val _perfilPurificadora = MutableStateFlow<PerfilNegocioResponse?>(null)
    val perfilPurificadora: StateFlow<PerfilNegocioResponse?> = _perfilPurificadora.asStateFlow()

    // Historial
    private val _historial = MutableStateFlow<List<PedidoResumenDto>>(emptyList())
    val historial: StateFlow<List<PedidoResumenDto>> = _historial.asStateFlow()

    // Pedido activo y creación
    private val _pedidoActivo = MutableStateFlow<PedidoResponse?>(null)
    val pedidoActivo: StateFlow<PedidoResponse?> = _pedidoActivo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Purificadora preseleccionada para CU-022 -> CU-004
    var purificadoraSeleccionadaId: Int? = null
    var purificadoraSeleccionadaNombre: String? = null
    var purificadoraSeleccionadaDistancia: String? = null


    fun cargarHistorial() {
        viewModelScope.launch {
            try {
                val resp = apiClient.pedidosApi.obtenerHistorialCliente(page = 0, size = 50)
                if (resp.isSuccessful && resp.body() != null) {
                    _historial.value = resp.body()?.elementos ?: emptyList()
                }
            } catch (e: Exception) {
                // Silencioso
            }
        }
    }

    fun cargarMarcas() {
        viewModelScope.launch {
            try {
                val resp = apiClient.negociosApi.listarMarcas()
                if (resp.isSuccessful && resp.body() != null) {
                    _marcas.value = resp.body()!!
                }
            } catch (e: Exception) {
                // Silencioso o log
            }
        }
    }

    fun cargarDirecciones(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val resp = apiClient.direccionesApi.listarDirecciones()
                if (resp.isSuccessful && resp.body() != null) {
                    val lista = resp.body()!!
                    _direcciones.value = lista
                    val defaultDir = lista.firstOrNull()
                    if (defaultDir != null && (defaultDir.lat != 0.0 || defaultDir.lon != 0.0)) {
                        buscarPurificadoras(defaultDir.lat, defaultDir.lon)
                    } else {
                        buscarPurificadoras(19.376692, -99.165057)
                    }
                } else if (_purificadoras.value.isEmpty()) {
                    buscarPurificadoras(19.376692, -99.165057)
                }
            } catch (e: Exception) {
                if (_purificadoras.value.isEmpty()) {
                    buscarPurificadoras(19.376692, -99.165057)
                }
            } finally {
                onComplete?.invoke()
            }
        }
    }

    fun crearDireccion(
        alias: String,
        calle: String,
        numeroExterior: String,
        numeroInterior: String?,
        colonia: String,
        codigoPostal: String,
        referencias: String,
        lat: Double,
        lon: Double,
        onSuccess: (DireccionResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val req = DireccionRequest(
                    alias = alias.trim(),
                    calle = calle.trim(),
                    numeroExterior = numeroExterior.trim(),
                    numeroInterior = numeroInterior?.trim()?.ifBlank { null },
                    colonia = colonia.trim(),
                    codigoPostal = codigoPostal.trim(),
                    referencias = referencias.trim(),
                    lat = lat,
                    lon = lon
                )
                val resp = apiClient.direccionesApi.crearDireccion(req)
                if (resp.isSuccessful && resp.body() != null) {
                    val nueva = resp.body()!!
                    _direcciones.value = _direcciones.value + nueva
                    if (_direcciones.value.size == 1 && (nueva.lat != 0.0 || nueva.lon != 0.0)) {
                        buscarPurificadoras(nueva.lat, nueva.lon)
                    }
                    onSuccess(nueva)
                } else {
                    val err = parseError(resp.errorBody()?.string())
                        ?: "No se pudo registrar la dirección (Código ${resp.code()})"
                    onError(err)
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de conexión al guardar dirección")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun actualizarDireccion(
        id: Int,
        alias: String,
        calle: String,
        numeroExterior: String,
        numeroInterior: String?,
        colonia: String,
        codigoPostal: String,
        referencias: String,
        lat: Double,
        lon: Double,
        onSuccess: (DireccionResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val req = DireccionRequest(
                    alias = alias.trim(),
                    calle = calle.trim(),
                    numeroExterior = numeroExterior.trim(),
                    numeroInterior = numeroInterior?.trim()?.ifBlank { null },
                    colonia = colonia.trim(),
                    codigoPostal = codigoPostal.trim(),
                    referencias = referencias.trim(),
                    lat = lat,
                    lon = lon
                )
                val resp = apiClient.direccionesApi.actualizarDireccion(id, req)
                if (resp.isSuccessful && resp.body() != null) {
                    val actualizada = resp.body()!!
                    _direcciones.value = _direcciones.value.map { if (it.id == id) actualizada else it }
                    val defaultDir = _direcciones.value.firstOrNull()
                    if (defaultDir?.id == id && (actualizada.lat != 0.0 || actualizada.lon != 0.0)) {
                        buscarPurificadoras(actualizada.lat, actualizada.lon)
                    }
                    onSuccess(actualizada)
                } else {
                    val err = parseError(resp.errorBody()?.string())
                        ?: "No se pudo actualizar la dirección (Código ${resp.code()})"
                    onError(err)
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de conexión al actualizar dirección")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun eliminarDireccion(
        id: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resp = apiClient.direccionesApi.eliminarDireccion(id)
                if (resp.isSuccessful) {
                    _direcciones.value = _direcciones.value.filter { it.id != id }
                    val defaultDir = _direcciones.value.firstOrNull()
                    if (defaultDir != null && (defaultDir.lat != 0.0 || defaultDir.lon != 0.0)) {
                        buscarPurificadoras(defaultDir.lat, defaultDir.lon)
                    } else {
                        buscarPurificadoras(19.376692, -99.165057)
                    }
                    onSuccess()
                } else {
                    val err = parseError(resp.errorBody()?.string())
                        ?: "No se pudo eliminar la dirección (Código ${resp.code()})"
                    onError(err)
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de conexión al eliminar dirección")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private var buscarPurificadorasJob: Job? = null

    fun buscarPurificadoras(lat: Double, lon: Double) {
        buscarPurificadorasJob?.cancel()
        buscarPurificadorasJob = viewModelScope.launch {
            try {
                val resp = apiClient.negociosApi.buscarCercanos("$lat,$lon", limite = 15)
                if (resp.isSuccessful && !resp.body().isNullOrEmpty()) {
                    _purificadoras.value = resp.body()!!
                } else {
                    // Fallback para evitar lista vacía si la coordenada está fuera de la zona estricta
                    val fallback = apiClient.negociosApi.buscarCercanos(cerca = null, limite = 15)
                    if (fallback.isSuccessful && fallback.body() != null) {
                        _purificadoras.value = fallback.body()!!
                    }
                }
            } catch (e: Exception) {
                try {
                    val fallback = apiClient.negociosApi.buscarCercanos(cerca = null, limite = 15)
                    if (fallback.isSuccessful && fallback.body() != null) {
                        _purificadoras.value = fallback.body()!!
                    }
                } catch (ignored: Exception) {}
            }
        }
    }

    fun cargarPerfilPurificadora(id: Int) {
        viewModelScope.launch {
            _perfilPurificadora.value = null
            _isLoading.value = true
            try {
                val resp = apiClient.negociosApi.obtenerPerfilNegocio(id)
                if (resp.isSuccessful && resp.body() != null) {
                    _perfilPurificadora.value = resp.body()!!
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar perfil de purificadora"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun crearPedido(
        request: PedidoCreateRequest,
        onSuccess: (PedidoResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val resp = apiClient.pedidosApi.crearPedido(request)
                if (resp.isSuccessful && resp.body() != null) {
                    val nuevoPedido = resp.body()!!
                    _pedidoActivo.value = nuevoPedido
                    onSuccess(nuevoPedido)
                } else {
                    val err = parseError(resp.errorBody()?.string())
                        ?: "No se pudo realizar el pedido (Código ${resp.code()})"
                    _errorMessage.value = err
                    onError(err)
                }
            } catch (e: Exception) {
                val err = e.message ?: "Error de red al crear el pedido"
                _errorMessage.value = err
                onError(err)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cargarDetallePedido(id: Int) {
        viewModelScope.launch { refrescarDetalle(id) }
    }

    private suspend fun refrescarDetalle(id: Int) {
        try {
            val resp = apiClient.pedidosApi.obtenerDetallePedido(id)
            if (resp.isSuccessful && resp.body() != null) {
                _pedidoActivo.value = resp.body()!!
            }
        } catch (e: Exception) {
            // Error
        }
    }

    // ---------------------------------------------------------------- Rastreo en vivo (CU-006)

    /** Posición del repartidor que llega por el tópico del pedido. */
    /** [recibidaMs]: cuándo llegó a la app; sirve para avisar si la posición quedó vieja (CU-006 S1). */
    data class UbicacionEnVivo(val lat: Double, val lon: Double, val recibidaMs: Long = ahoraMs())

    private val _ubicacionRepartidor = MutableStateFlow<UbicacionEnVivo?>(null)
    val ubicacionRepartidor: StateFlow<UbicacionEnVivo?> = _ubicacionRepartidor.asStateFlow()

    private var rastreoJob: Job? = null

    private val _repartidorLlego = MutableStateFlow(false)
    /** CU-012: el repartidor avisó que ya está en el domicilio ("Llegué al destino"). */
    val repartidorLlego: StateFlow<Boolean> = _repartidorLlego.asStateFlow()

    private val _rutaSeguimiento = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    /** Ruta por calles del repartidor al domicilio; vacía mientras no hay (el mapa dibuja línea recta). */
    val rutaSeguimiento: StateFlow<List<Pair<Double, Double>>> = _rutaSeguimiento.asStateFlow()
    private var origenRutaSeguimiento: UbicacionEnVivo? = null
    private var rutaSeguimientoMs = 0L

    /**
     * Mientras el pedido está activo: escucha el WebSocket (reconecta si se cae) y, cuando no hay
     * conexión, consulta la última posición cada 10 s. Además refresca el estado del pedido cada
     * 20 s para que la línea de tiempo avance (asignado → en camino → entregado) sin recargar.
     */
    fun iniciarRastreo(idPedido: Int) {
        rastreoJob?.cancel()
        _ubicacionRepartidor.value = null
        _repartidorLlego.value = false
        _rutaSeguimiento.value = emptyList()
        origenRutaSeguimiento = null
        val token = sessionManager.obtenerToken() ?: return
        rastreoJob = viewModelScope.launch {
            var conectado = false

            // Avisos instantáneos por el canal de notificaciones (/user/queue/notificaciones).
            launch {
                CanalNotificaciones.avisos.collect { a ->
                    if (a.datos["idPedido"] != idPedido.toString()) return@collect
                    when (a.datos["tipo"]) {
                        "repartidor_llego" -> _repartidorLlego.value = true
                        "pedido_entregado" -> {
                            // Cambio optimista inmediato en UI (0 ms) + sincronización con el servidor
                            _pedidoActivo.value = _pedidoActivo.value?.takeIf { it.id == idPedido }?.copy(estado = "entregado")
                            _ubicacionRepartidor.value = null
                            refrescarDetalle(idPedido)
                            cargarHistorial()
                            detenerRastreo()
                        }
                        "pedido_no_entregado" -> {
                            _pedidoActivo.value = _pedidoActivo.value?.takeIf { it.id == idPedido }?.copy(estado = "no_entregado")
                            _ubicacionRepartidor.value = null
                            refrescarDetalle(idPedido)
                            cargarHistorial()
                            detenerRastreo()
                        }
                        "pedido_en_camino", "pedido_asignado" -> {
                            refrescarDetalle(idPedido)
                            actualizarRutaSeguimiento(idPedido, _ubicacionRepartidor.value)
                        }
                    }
                }
            }

            // Consulta inicial inmediata de ubicación y ruta
            launch {
                consultarUbicacion(idPedido)
                actualizarRutaSeguimiento(idPedido, _ubicacionRepartidor.value)
            }

            // Ruta por calles: se actualiza cuando el repartidor avanza > 60 m (máx. cada 15 s)
            // o de inmediato si todavía no tenemos la ruta (_rutaSeguimiento vacía).
            launch {
                combine(_pedidoActivo, _ubicacionRepartidor) { pedido, u -> pedido to u }
                    .collect { (pedido, u) ->
                        val est = pedido?.estado?.lowercase()
                        if (est !in setOf("asignado", "en_camino")) return@collect
                        val necesitaRuta = _rutaSeguimiento.value.isEmpty()
                        val previo = origenRutaSeguimiento
                        val movido = previo == null || (u != null && com.htogo.app.data.location.distanciaM(previo.lat, previo.lon, u.lat, u.lon) > 60)
                        if (necesitaRuta || (movido && ahoraMs() - rutaSeguimientoMs >= 15_000)) {
                            actualizarRutaSeguimiento(idPedido, u)
                        }
                    }
            }

            // Canal WebSocket en tiempo real del pedido (/topic/pedidos/{id}/ubicacion)
            launch {
                while (isActive) {
                    StompCliente.suscribir(StompCliente.topicoUbicacion(idPedido), token)
                        .takeWhile { it !is EventoStomp.Desconectado }
                        .collect { evento ->
                            when (evento) {
                                is EventoStomp.Conectado -> conectado = true
                                is EventoStomp.Mensaje -> {
                                    when (extraerEventoTopico(evento.cuerpo)) {
                                        "llegada" -> _repartidorLlego.value = true
                                        "entregado", "no_entregado" -> {
                                            val nuevoEstado = extraerEventoTopico(evento.cuerpo)!!
                                            _pedidoActivo.value = _pedidoActivo.value?.takeIf { it.id == idPedido }?.copy(estado = nuevoEstado)
                                            _ubicacionRepartidor.value = null
                                            refrescarDetalle(idPedido)
                                            cargarHistorial()
                                            detenerRastreo()
                                        }
                                        else -> parsearUbicacion(evento.cuerpo)?.let { _ubicacionRepartidor.value = it }
                                    }
                                }
                                else -> Unit
                            }
                        }
                    conectado = false
                    delay(5_000) // reintento tras caída de red o rechazo del servidor
                }
            }

            // Respaldo de ubicación si no hay WebSocket o si aún no tenemos posición
            launch {
                while (isActive) {
                    if (!conectado || _ubicacionRepartidor.value == null) {
                        consultarUbicacion(idPedido)
                    }
                    delay(10_000)
                }
            }

            // Refresco periódico de respaldo del detalle del pedido (cada 6 s) y reintento de ruta si falta
            launch {
                while (isActive) {
                    delay(6_000)
                    refrescarDetalle(idPedido)
                    val estado = _pedidoActivo.value?.estado?.lowercase()
                    if (_rutaSeguimiento.value.isEmpty() && estado in setOf("asignado", "en_camino")) {
                        actualizarRutaSeguimiento(idPedido, _ubicacionRepartidor.value)
                    }
                    if (estado in setOf("entregado", "no_entregado", "cancelado")) {
                        _ubicacionRepartidor.value = null
                        cargarHistorial()
                        detenerRastreo() // pedido cerrado: corta WebSocket y consultas
                    }
                }
            }
        }
    }

    private suspend fun actualizarRutaSeguimiento(idPedido: Int, desde: UbicacionEnVivo?) {
        rutaSeguimientoMs = ahoraMs()
        try {
            val resp = apiClient.pedidosApi.rutaSeguimiento(idPedido)
            val ruta = resp.body()
            if (resp.isSuccessful && ruta != null && ruta.encontrada && ruta.coordenadas.isNotEmpty()) {
                _rutaSeguimiento.value = ruta.coordenadas.map { it.lat to it.lon }
                origenRutaSeguimiento = desde ?: UbicacionEnVivo(ruta.coordenadas.first().lat, ruta.coordenadas.first().lon)
            }
        } catch (_: Exception) {
            // Sin red: se reintenta en el próximo ciclo sin borrar la ruta previa
        }
    }

    fun detenerRastreo() {
        rastreoJob?.cancel()
        rastreoJob = null
    }

    /** El tópico del pedido lleva {"evento":"llegada" | "entregado" | "no_entregado"}. */
    private fun extraerEventoTopico(json: String): String? = try {
        com.htogo.shared.data.jsonApi.parseToJsonElement(json).jsonObject["evento"]?.jsonPrimitive?.content
    } catch (e: Exception) {
        null
    }

    private fun parsearUbicacion(json: String): UbicacionEnVivo? = try {
        val o = com.htogo.shared.data.jsonApi.parseToJsonElement(json).jsonObject
        UbicacionEnVivo(o.getValue("lat").jsonPrimitive.double, o.getValue("lon").jsonPrimitive.double)
    } catch (e: Exception) {
        null
    }

    private suspend fun consultarUbicacion(idPedido: Int) {
        try {
            val resp = apiClient.pedidosApi.ubicacionRepartidor(idPedido)
            resp.body()?.let { u ->
                if (u.llegoEn != null) _repartidorLlego.value = true
                val previa = _ubicacionRepartidor.value
                // Misma posición que ya teníamos: no la marca como reciente.
                if (previa == null || previa.lat != u.lat || previa.lon != u.lon) {
                    _ubicacionRepartidor.value = UbicacionEnVivo(u.lat, u.lon)
                }
            }
        } catch (_: Exception) {
            // Sin red: se reintenta en el siguiente ciclo.
        }
    }

    fun cancelarPedido(
        id: Int,
        motivo: String = "Cancelado por el cliente",
        onSuccess: (PedidoResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resp = apiClient.pedidosApi.cancelarPedido(id, CancelacionRequest(motivo))
                if (resp.isSuccessful && resp.body() != null) {
                    val actualizado = resp.body()!!
                    _pedidoActivo.value = actualizado
                    onSuccess(actualizado)
                } else {
                    val err = parseError(resp.errorBody()?.string())
                        ?: "El pedido ya no puede ser cancelado (Código ${resp.code()})"
                    onError(err)
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error al cancelar el pedido")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun parseError(errorJson: String?): String? {
        return mensajeDeError(errorJson)
    }

    // Al final de la clase: debe correr después de inicializar todos los StateFlow de arriba.
    // El ViewModel se crea al abrir la app (antes del login), así que no basta con cargar una vez:
    // se recarga cada vez que cambia la sesión y se limpia al cerrarla (evita ver datos de otra cuenta).
    init {
        viewModelScope.launch {
            sessionManager.token.collect { token ->
                if (token.isNullOrBlank()) limpiarDatosDeSesion() else recargarDatosDeSesion()
            }
        }
    }

    private fun recargarDatosDeSesion() {
        cargarMarcas()
        cargarDirecciones()
        cargarHistorial()
    }

    private fun limpiarDatosDeSesion() {
        detenerRastreo()
        _direcciones.value = emptyList()
        _historial.value = emptyList()
        _pedidoActivo.value = null
        _ubicacionRepartidor.value = null
    }
}
