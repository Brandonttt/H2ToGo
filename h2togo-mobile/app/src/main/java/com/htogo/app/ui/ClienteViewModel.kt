package com.htogo.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
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
import com.htogo.app.data.realtime.EventoStomp
import com.htogo.app.data.realtime.StompCliente
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClienteViewModel(application: Application) : AndroidViewModel(application) {

    private val apiClient = ApiClient.getInstance(application)
    private val sessionManager = SessionManager.getInstance(application)
    private val gson = Gson()

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
    var purificadoraSeleccionadaRating: Float? = null
    var purificadoraSeleccionadaResenas: Int? = null

    init {
        cargarMarcas()
        cargarDirecciones()
        cargarHistorial()
        // Benito Juárez centro por defecto: 19.376692, -99.165057
        buscarPurificadoras(19.376692, -99.165057)
    }

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

    fun cargarDirecciones() {
        viewModelScope.launch {
            try {
                val resp = apiClient.direccionesApi.listarDirecciones()
                if (resp.isSuccessful && resp.body() != null) {
                    val lista = resp.body()!!
                    _direcciones.value = lista
                    val defaultDir = lista.firstOrNull()
                    if (defaultDir != null) {
                        buscarPurificadoras(defaultDir.lat, defaultDir.lon)
                    }
                }
            } catch (e: Exception) {
                // Error de red
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
                    onSuccess(nueva)
                } else {
                    val err = parseError(resp.errorBody()?.string())
                        ?: "No se pudo registrar la dirección (Código ${resp.code()})"
                    onError(err)
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error de conexión al guardar dirección")
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
                    onSuccess(actualizada)
                } else {
                    val err = parseError(resp.errorBody()?.string())
                        ?: "No se pudo actualizar la dirección (Código ${resp.code()})"
                    onError(err)
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error de conexión al actualizar dirección")
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
                    onSuccess()
                } else {
                    val err = parseError(resp.errorBody()?.string())
                        ?: "No se pudo eliminar la dirección (Código ${resp.code()})"
                    onError(err)
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error de conexión al eliminar dirección")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun buscarPurificadoras(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val resp = apiClient.negociosApi.buscarCercanos("$lat,$lon", limite = 15)
                if (resp.isSuccessful && resp.body() != null) {
                    _purificadoras.value = resp.body()!!
                }
            } catch (e: Exception) {
                // Error
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
                val err = e.localizedMessage ?: "Error de red al crear el pedido"
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
    data class UbicacionEnVivo(val lat: Double, val lon: Double)

    private val _ubicacionRepartidor = MutableStateFlow<UbicacionEnVivo?>(null)
    val ubicacionRepartidor: StateFlow<UbicacionEnVivo?> = _ubicacionRepartidor.asStateFlow()

    private var rastreoJob: Job? = null

    /**
     * Mientras el pedido está activo: escucha el WebSocket (reconecta si se cae) y, cuando no hay
     * conexión, consulta la última posición cada 10 s. Además refresca el estado del pedido cada
     * 20 s para que la línea de tiempo avance (asignado → en camino → entregado) sin recargar.
     */
    fun iniciarRastreo(idPedido: Int) {
        rastreoJob?.cancel()
        _ubicacionRepartidor.value = null
        val token = sessionManager.obtenerToken() ?: return
        rastreoJob = viewModelScope.launch {
            var conectado = false
            launch {
                while (isActive) {
                    StompCliente.suscribir(StompCliente.topicoUbicacion(idPedido), token)
                        .takeWhile { it !is EventoStomp.Desconectado }
                        .collect { evento ->
                            when (evento) {
                                is EventoStomp.Conectado -> conectado = true
                                is EventoStomp.Mensaje -> parsearUbicacion(evento.cuerpo)?.let { _ubicacionRepartidor.value = it }
                                else -> Unit
                            }
                        }
                    conectado = false
                    delay(5_000) // reintento tras caída de red o rechazo del servidor
                }
            }
            launch {
                while (isActive) {
                    if (!conectado) consultarUbicacion(idPedido)
                    delay(10_000)
                }
            }
            launch {
                while (isActive) {
                    delay(20_000)
                    refrescarDetalle(idPedido)
                    val estado = _pedidoActivo.value?.estado?.lowercase()
                    if (estado in setOf("entregado", "no_entregado", "cancelado")) {
                        _ubicacionRepartidor.value = null
                        detenerRastreo() // pedido cerrado: corta WebSocket y consultas
                    }
                }
            }
        }
    }

    fun detenerRastreo() {
        rastreoJob?.cancel()
        rastreoJob = null
    }

    private fun parsearUbicacion(json: String): UbicacionEnVivo? = try {
        val o = org.json.JSONObject(json)
        UbicacionEnVivo(o.getDouble("lat"), o.getDouble("lon"))
    } catch (e: Exception) {
        null
    }

    private suspend fun consultarUbicacion(idPedido: Int) {
        try {
            val resp = apiClient.pedidosApi.ubicacionRepartidor(idPedido)
            resp.body()?.let { _ubicacionRepartidor.value = UbicacionEnVivo(it.lat, it.lon) }
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
                onError(e.localizedMessage ?: "Error al cancelar el pedido")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun parseError(errorJson: String?): String? {
        if (errorJson.isNullOrBlank()) return null
        return try {
            val map = gson.fromJson(errorJson, Map::class.java)
            map["mensaje"]?.toString() ?: map["message"]?.toString()
        } catch (e: Exception) {
            null
        }
    }
}
