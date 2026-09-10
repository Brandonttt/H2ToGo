package com.htogo.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.htogo.app.data.api.ApiClient
import com.htogo.app.data.dto.PedidoDisponibleResponse
import com.htogo.app.data.dto.PedidoResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RepartidorViewModel(application: Application) : AndroidViewModel(application) {

    private val apiClient = ApiClient.getInstance(application)
    private val gson = Gson()

    private val _pedidosDisponibles = MutableStateFlow<List<PedidoDisponibleResponse>>(emptyList())
    val pedidosDisponibles: StateFlow<List<PedidoDisponibleResponse>> = _pedidosDisponibles.asStateFlow()

    private val _pedidoEnRuta = MutableStateFlow<PedidoResponse?>(null)
    val pedidoEnRuta: StateFlow<PedidoResponse?> = _pedidoEnRuta.asStateFlow()

    // Inventario (CU-018 / CU-019)
    private val _inventarioBase = MutableStateFlow<com.htogo.app.data.dto.InventarioBaseResponse?>(null)
    val inventarioBase: StateFlow<com.htogo.app.data.dto.InventarioBaseResponse?> = _inventarioBase.asStateFlow()

    private val _inventarioVehiculo = MutableStateFlow<com.htogo.app.data.dto.InventarioVehiculoResponse?>(null)
    val inventarioVehiculo: StateFlow<com.htogo.app.data.dto.InventarioVehiculoResponse?> = _inventarioVehiculo.asStateFlow()

    private val _marcas = MutableStateFlow<List<com.htogo.app.data.dto.MarcaResponse>>(emptyList())
    val marcas: StateFlow<List<com.htogo.app.data.dto.MarcaResponse>> = _marcas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        cargarPedidosDisponibles()
        cargarInventarios()
        cargarMarcas()
    }

    fun cargarMarcas() {
        viewModelScope.launch {
            try {
                val resp = apiClient.negociosApi.listarMarcas()
                if (resp.isSuccessful && resp.body() != null) {
                    _marcas.value = resp.body()!!
                }
            } catch (e: Exception) {
                // Silencioso
            }
        }
    }

    fun cargarInventarios() {
        cargarInventarioBase()
        cargarInventarioVehiculo()
    }

    fun cargarInventarioBase() {
        viewModelScope.launch {
            try {
                val resp = apiClient.inventarioApi.obtenerInventarioBase()
                if (resp.isSuccessful && resp.body() != null) {
                    _inventarioBase.value = resp.body()!!
                }
            } catch (e: Exception) {
                // Silencioso
            }
        }
    }

    fun cargarInventarioVehiculo() {
        viewModelScope.launch {
            try {
                val resp = apiClient.inventarioApi.obtenerInventarioVehiculo()
                if (resp.isSuccessful && resp.body() != null) {
                    _inventarioVehiculo.value = resp.body()!!
                }
            } catch (e: Exception) {
                // Silencioso
            }
        }
    }

    fun cargarVehiculo(
        cargas: List<com.htogo.app.data.dto.CargaItemDto>,
        onSuccess: (com.htogo.app.data.dto.InventarioVehiculoResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resp = apiClient.inventarioApi.cargarVehiculo(com.htogo.app.data.dto.CargaVehiculoRequest(cargas))
                if (resp.isSuccessful && resp.body() != null) {
                    _inventarioVehiculo.value = resp.body()!!
                    cargarInventarioBase()
                    onSuccess(resp.body()!!)
                } else {
                    val err = parseError(resp.errorBody()?.string())
                        ?: "No se pudo cargar el vehículo (${resp.code()})"
                    onError(err)
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error de red al cargar el vehículo")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun registrarLote(
        request: com.htogo.app.data.dto.LoteEntradaRequest,
        onSuccess: (com.htogo.app.data.dto.LoteResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resp = apiClient.inventarioApi.registrarLote(request)
                if (resp.isSuccessful && resp.body() != null) {
                    cargarInventarioBase()
                    onSuccess(resp.body()!!)
                } else {
                    val err = parseError(resp.errorBody()?.string())
                        ?: "Error al registrar lote (${resp.code()})"
                    onError(err)
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error de red al registrar lote")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun devolverABase(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resp = apiClient.inventarioApi.devolverABase()
                if (resp.isSuccessful) {
                    cargarInventarios()
                    onSuccess()
                } else {
                    val err = parseError(resp.errorBody()?.string())
                        ?: "Error al devolver al almacén base"
                    onError(err)
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error al devolver inventario")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cargarPedidosDisponibles() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resp = apiClient.repartidorApi.obtenerPedidosDisponibles()
                if (resp.isSuccessful && resp.body() != null) {
                    _pedidosDisponibles.value = resp.body()!!
                }
            } catch (e: Exception) {
                // Silencioso o log
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun aceptarPedido(
        id: Int,
        onSuccess: (PedidoResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val resp = apiClient.repartidorApi.aceptarPedido(id)
                if (resp.isSuccessful && resp.body() != null) {
                    val pedido = resp.body()!!
                    _pedidoEnRuta.value = pedido
                    cargarPedidosDisponibles()
                    onSuccess(pedido)
                } else {
                    val err = parseError(resp.errorBody()?.string())
                        ?: "No se pudo tomar el pedido. Puede que otro repartidor ya lo haya tomado."
                    _errorMessage.value = err
                    onError(err)
                }
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "Error al aceptar el pedido"
                _errorMessage.value = err
                onError(err)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun marcarEnCamino(
        id: Int,
        lat: Double = 19.376692,
        lon: Double = -99.165057,
        onSuccess: (PedidoResponse) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val resp = apiClient.repartidorApi.marcarEnCamino(id, mapOf("lat" to lat, "lon" to lon))
                if (resp.isSuccessful && resp.body() != null) {
                    _pedidoEnRuta.value = resp.body()!!
                    onSuccess(resp.body()!!)
                }
            } catch (e: Exception) {
                // Log
            }
        }
    }

    fun registrarResultadoEntrega(
        id: Int,
        esEntregado: Boolean,
        onSuccess: (PedidoResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val payload: Map<String, Any> = if (esEntregado) {
                    mapOf("resultado" to "entregado")
                } else {
                    mapOf("resultado" to "no_entregado", "motivo" to "Cliente no se presentó tras 10 minutos")
                }

                val resp = apiClient.repartidorApi.finalizarEntrega(id, payload)
                if (resp.isSuccessful && resp.body() != null) {
                    _pedidoEnRuta.value = null
                    cargarPedidosDisponibles()
                    onSuccess(resp.body()!!)
                } else {
                    val err = parseError(resp.errorBody()?.string())
                        ?: "Error al registrar el resultado de entrega (${resp.code()})"
                    onError(err)
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Error de red al registrar entrega")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun reportarUbicacion(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                apiClient.repartidorApi.reportarUbicacion(mapOf("lat" to lat, "lon" to lon))
            } catch (e: Exception) {
                // Silencioso
            }
        }
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
