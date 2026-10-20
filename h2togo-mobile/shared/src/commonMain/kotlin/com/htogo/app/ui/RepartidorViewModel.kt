package com.htogo.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htogo.shared.data.mensajeDeError
import com.htogo.app.plataforma.ahoraMs
import com.htogo.app.plataforma.Plataforma
import com.htogo.app.data.api.ApiClient
import com.htogo.app.data.dto.AgendaPedidoDto
import com.htogo.app.data.dto.ParadaDto
import com.htogo.app.data.dto.PedidoDisponibleResponse
import com.htogo.app.data.dto.RutaOptimaDto
import com.htogo.app.data.dto.PedidoResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RepartidorViewModel : ViewModel() {

    private val apiClient = ApiClient.getInstance()
    private val sessionManager = com.htogo.app.data.local.SessionManager.getInstance()

    private val _pedidosDisponibles = MutableStateFlow<List<PedidoDisponibleResponse>>(emptyList())
    val pedidosDisponibles: StateFlow<List<PedidoDisponibleResponse>> = _pedidosDisponibles.asStateFlow()

    private val _pedidoEnRuta = MutableStateFlow<PedidoResponse?>(null)
    val pedidoEnRuta: StateFlow<PedidoResponse?> = _pedidoEnRuta.asStateFlow()

    private val _pedidoDisponibleSeleccionado = MutableStateFlow<PedidoDisponibleResponse?>(null)
    val pedidoDisponibleSeleccionado: StateFlow<PedidoDisponibleResponse?> = _pedidoDisponibleSeleccionado.asStateFlow()

    private val _rutaCalculada = MutableStateFlow<com.htogo.app.data.dto.RouteResponse?>(null)
    val rutaCalculada: StateFlow<com.htogo.app.data.dto.RouteResponse?> = _rutaCalculada.asStateFlow()

    fun setPedidoDisponibleSeleccionado(pedido: PedidoDisponibleResponse?) {
        _pedidoDisponibleSeleccionado.value = pedido
    }

    private val _miNegocio = MutableStateFlow<com.htogo.app.data.dto.PerfilNegocioResponse?>(null)
    val miNegocio: StateFlow<com.htogo.app.data.dto.PerfilNegocioResponse?> = _miNegocio.asStateFlow()

    private val _totalEntregas = MutableStateFlow(0)
    val totalEntregas: StateFlow<Int> = _totalEntregas.asStateFlow()

    private val _misEntregas = MutableStateFlow<List<com.htogo.app.data.dto.PedidoResumenDto>>(emptyList())
    val misEntregas: StateFlow<List<com.htogo.app.data.dto.PedidoResumenDto>> = _misEntregas.asStateFlow()

    // Inventario (CU-018 / CU-019)
    private val _inventarioBase = MutableStateFlow<com.htogo.app.data.dto.InventarioBaseResponse?>(null)
    val inventarioBase: StateFlow<com.htogo.app.data.dto.InventarioBaseResponse?> = _inventarioBase.asStateFlow()

    private val _inventarioVehiculo = MutableStateFlow<com.htogo.app.data.dto.InventarioVehiculoResponse?>(null)
    val inventarioVehiculo: StateFlow<com.htogo.app.data.dto.InventarioVehiculoResponse?> = _inventarioVehiculo.asStateFlow()

    private val _marcas = MutableStateFlow<List<com.htogo.app.data.dto.MarcaResponse>>(emptyList())
    val marcas: StateFlow<List<com.htogo.app.data.dto.MarcaResponse>> = _marcas.asStateFlow()

    // Solicitudes de cambio del negocio (CU-020)
    private val _solicitudes = MutableStateFlow<List<com.htogo.app.data.dto.SolicitudResponse>>(emptyList())
    val solicitudes: StateFlow<List<com.htogo.app.data.dto.SolicitudResponse>> = _solicitudes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()


    private val _esDueno = MutableStateFlow(false)
    /** RN-021: solo el dueño ve las acciones que cambian el negocio (inventario base, catálogo, solicitudes). */
    val esDueno: StateFlow<Boolean> = _esDueno.asStateFlow()

    fun cargarMiNegocio() {
        viewModelScope.launch {
            try {
                apiClient.authApi.perfil().body()?.let { _esDueno.value = it.repartidor?.esDueno == true }
            } catch (e: Exception) {
                // Silencioso: sin dato se ocultan las acciones de dueño
            }
            try {
                val resp = apiClient.negociosApi.obtenerMiNegocio()
                if (resp.isSuccessful && resp.body() != null) {
                    _miNegocio.value = resp.body()!!
                    sessionManager.guardarNombreNegocio(resp.body()!!.nombreComercial)
                }
            } catch (e: Exception) {
                // Silencioso
            }
        }
    }

    fun actualizarPrecio(
        idProducto: Int,
        nuevoPrecio: Double,
        precioEnvase: Double,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val resp = apiClient.negociosApi.actualizarPrecio(
                    id = idProducto,
                    request = com.htogo.app.data.dto.PrecioRequest(precio = nuevoPrecio, precioEnvase = precioEnvase)
                )
                if (resp.isSuccessful) {
                    cargarMiNegocio()
                    onSuccess()
                } else {
                    onError(parseError(resp.errorBody()?.string()) ?: "No se pudo actualizar el precio (${resp.code()})")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de red al actualizar el precio")
            }
        }
    }

    fun actualizarHorarios(
        dias: List<com.htogo.app.data.dto.HorarioNegocioDto>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val resp = apiClient.negociosApi.actualizarHorarios(dias)
                if (resp.isSuccessful) {
                    cargarMiNegocio()
                    onSuccess()
                } else {
                    onError(parseError(resp.errorBody()?.string()) ?: "No se pudo guardar el horario (${resp.code()})")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de red al guardar el horario")
            }
        }
    }

    fun cargarSolicitudes() {
        viewModelScope.launch {
            try {
                val resp = apiClient.solicitudesApi.misSolicitudes()
                if (resp.isSuccessful && resp.body() != null) {
                    _solicitudes.value = resp.body()!!.sortedByDescending { it.fechaSolicitud }
                }
            } catch (e: Exception) {
                // Silencioso
            }
        }
    }

    /** CU-020: pide al admin agregar una marca al catálogo del negocio (RN-020). */
    fun solicitarProducto(
        idMarca: Int,
        nombreMarca: String,
        precio: Double,
        precioEnvase: Double,
        capacidadMaxima: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val resp = apiClient.solicitudesApi.crear(
                    com.htogo.app.data.dto.SolicitudRequest(
                        codigoCambio = "AGREGAR_PRODUCTO",
                        // "marca" solo es informativo (el panel lo muestra); el backend usa idMarca.
                        valorNuevo = mapOf(
                            "idMarca" to idMarca,
                            "marca" to nombreMarca,
                            "precio" to precio,
                            "precioEnvase" to precioEnvase,
                            "capacidadMaxima" to capacidadMaxima
                        )
                    )
                )
                if (resp.isSuccessful) {
                    cargarSolicitudes()
                    onSuccess()
                } else {
                    onError(parseError(resp.errorBody()?.string()) ?: "No se pudo enviar la solicitud (${resp.code()})")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de red al enviar la solicitud")
            }
        }
    }

    fun cancelarSolicitud(id: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val resp = apiClient.solicitudesApi.cancelar(id)
                if (resp.isSuccessful) {
                    _solicitudes.value = _solicitudes.value.filterNot { it.id == id }
                    onSuccess()
                } else {
                    // 409: el admin la resolvió mientras tanto; refrescar para mostrar su estado real.
                    cargarSolicitudes()
                    onError(parseError(resp.errorBody()?.string()) ?: "No se pudo cancelar (${resp.code()})")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de red al cancelar la solicitud")
            }
        }
    }

    /**
     * CU-009 / CU-020: Pide al admin agregar un vehículo al negocio (RN-019, RN-020).
     */
    fun solicitarAgregarVehiculo(
        request: com.htogo.app.data.dto.ActualizarVehiculoRequest,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val resp = apiClient.solicitudesApi.crear(
                    com.htogo.app.data.dto.SolicitudRequest(
                        codigoCambio = "AGREGAR_VEHICULO",
                        valorNuevo = mapOf(
                            "tipoVehiculo" to (request.tipoVehiculo ?: "motocicleta"),
                            "marca" to (request.marca ?: ""),
                            "modelo" to (request.modelo ?: ""),
                            "color" to (request.color ?: ""),
                            "placas" to (request.placas ?: "").uppercase(),
                            "capacidadGarrafones" to (request.capacidadGarrafones ?: 30)
                        )
                    )
                )
                if (resp.isSuccessful) {
                    cargarSolicitudes()
                    onSuccess()
                } else {
                    onError(parseError(resp.errorBody()?.string()) ?: "No se pudo enviar la solicitud (${resp.code()})")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de red al enviar la solicitud")
            }
        }
    }

    /**
     * CU-009 / CU-020: Pide al admin modificar los datos de un vehículo del negocio (RN-019).
     */
    fun solicitarModificarVehiculo(
        idVehiculo: Int,
        request: com.htogo.app.data.dto.ActualizarVehiculoRequest,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val resp = apiClient.solicitudesApi.crear(
                    com.htogo.app.data.dto.SolicitudRequest(
                        codigoCambio = "DATOS_VEHICULO",
                        idVehiculo = idVehiculo,
                        valorNuevo = mapOf(
                            "tipoVehiculo" to (request.tipoVehiculo ?: "motocicleta"),
                            "marca" to (request.marca ?: ""),
                            "modelo" to (request.modelo ?: ""),
                            "color" to (request.color ?: ""),
                            "placas" to (request.placas ?: "").uppercase(),
                            "capacidadGarrafones" to (request.capacidadGarrafones ?: 30)
                        )
                    )
                )
                if (resp.isSuccessful) {
                    cargarSolicitudes()
                    onSuccess()
                } else {
                    onError(parseError(resp.errorBody()?.string()) ?: "No se pudo enviar la solicitud (${resp.code()})")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de red al enviar la solicitud")
            }
        }
    }

    /**
     * CU-009 / CU-020: Pide al admin dar de baja un vehículo del negocio (RN-019).
     */
    fun solicitarEliminarVehiculo(
        idVehiculo: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val resp = apiClient.solicitudesApi.crear(
                    com.htogo.app.data.dto.SolicitudRequest(
                        codigoCambio = "ELIMINAR_VEHICULO",
                        idVehiculo = idVehiculo,
                        valorNuevo = emptyMap()
                    )
                )
                if (resp.isSuccessful) {
                    cargarSolicitudes()
                    onSuccess()
                } else {
                    onError(parseError(resp.errorBody()?.string()) ?: "No se pudo enviar la solicitud (${resp.code()})")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de red al enviar la solicitud")
            }
        }
    }

    /**
     * CU-020: Pide al admin cambiar el nombre comercial del negocio (RN-019, RN-021).
     */
    fun solicitarCambioNombreNegocio(
        nuevoNombre: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val resp = apiClient.solicitudesApi.crear(
                    com.htogo.app.data.dto.SolicitudRequest(
                        codigoCambio = "NOMBRE_NEGOCIO",
                        valorNuevo = mapOf("nombreComercial" to nuevoNombre.trim())
                    )
                )
                if (resp.isSuccessful) {
                    cargarSolicitudes()
                    onSuccess()
                } else {
                    onError(parseError(resp.errorBody()?.string()) ?: "No se pudo enviar la solicitud (${resp.code()})")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de red al enviar la solicitud")
            }
        }
    }

    /**
     * CU-020: Pide al admin cambiar la dirección de la base del negocio (RN-019, RN-021).
     */
    fun solicitarCambioDireccionBase(
        calle: String,
        numeroExterior: String,
        numeroInterior: String?,
        colonia: String,
        codigoPostal: String,
        referencias: String?,
        lat: Double? = null,
        lon: Double? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val mapa = mutableMapOf<String, Any>(
                    "calle" to calle.trim(),
                    "numeroExterior" to numeroExterior.trim(),
                    "colonia" to colonia.trim(),
                    "codigoPostal" to codigoPostal.trim()
                )
                if (!numeroInterior.isNullOrBlank()) mapa["numeroInterior"] = numeroInterior.trim()
                if (!referencias.isNullOrBlank()) mapa["referencias"] = referencias.trim()
                if (lat != null && lon != null) {
                    mapa["lat"] = lat
                    mapa["lon"] = lon
                }
                val resp = apiClient.solicitudesApi.crear(
                    com.htogo.app.data.dto.SolicitudRequest(
                        codigoCambio = "DIRECCION_BASE",
                        valorNuevo = mapa
                    )
                )
                if (resp.isSuccessful) {
                    cargarSolicitudes()
                    onSuccess()
                } else {
                    onError(parseError(resp.errorBody()?.string()) ?: "No se pudo enviar la solicitud (${resp.code()})")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de red al enviar la solicitud")
            }
        }
    }

    fun cargarMisEntregas() {
        viewModelScope.launch {
            try {
                // Todas las páginas (100 es el máximo del backend), con tope de 1000 entregas: los
                // ingresos del mes y el historial se calculan sobre esta lista.
                val todas = mutableListOf<com.htogo.app.data.dto.PedidoResumenDto>()
                var pagina = 0
                var total = 0L
                do {
                    val resp = apiClient.repartidorApi.obtenerMisEntregas(page = pagina, size = 100)
                    val body = resp.body()
                    if (!resp.isSuccessful || body == null) break
                    todas += body.elementos
                    total = body.totalElementos
                    pagina++
                } while (pagina < body.totalPaginas && pagina < 10)
                _misEntregas.value = todas
                _totalEntregas.value = total.toInt()
            } catch (e: Exception) {
                // Silencioso
            }
        }
    }

    /**
     * CU-013: Carga el detalle completo de un pedido con historial de timestamps del ciclo de vida.
     */
    fun cargarDetallePedido(idPedido: Int, onResult: (com.htogo.app.data.dto.PedidoResponse?) -> Unit) {
        viewModelScope.launch {
            try {
                val resp = apiClient.pedidosApi.obtenerDetallePedido(idPedido)
                if (resp.isSuccessful) {
                    onResult(resp.body())
                } else {
                    onResult(null)
                }
            } catch (e: Exception) {
                onResult(null)
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

    fun iniciarJornada(
        idVehiculo: Int,
        cargasIniciales: List<com.htogo.app.data.dto.CargaItemDto>? = null,
        onSuccess: (com.htogo.app.data.dto.JornadaResponse) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resp = apiClient.repartidorApi.iniciarJornada(
                    com.htogo.app.data.dto.IniciarJornadaRequest(
                        idVehiculo = idVehiculo,
                        cargaInicial = cargasIniciales
                    )
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val body = resp.body()!!
                    if (body.inventario != null) {
                        _inventarioVehiculo.value = body.inventario
                    } else {
                        cargarInventarioVehiculo()
                    }
                    cargarInventarioBase()
                    onSuccess(body)
                } else {
                    val err = parseError(resp.errorBody()?.string())
                        ?: "Error al iniciar jornada (${resp.code()})"
                    onError(err)
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de red al iniciar jornada")
            } finally {
                _isLoading.value = false
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
                onError(e.message ?: "Error de red al cargar el vehículo")
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
                onError(e.message ?: "Error de red al registrar lote")
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
                onError(e.message ?: "Error al devolver inventario")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun registrarSalidaManual(
        idMarca: Int,
        cantidad: Int,
        motivo: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resp = apiClient.inventarioApi.registrarSalidaManual(
                    com.htogo.app.data.dto.SalidaManualRequest(
                        idMarca = idMarca,
                        cantidad = cantidad,
                        motivo = motivo
                    )
                )
                if (resp.isSuccessful) {
                    cargarInventarioBase()
                    cargarMiNegocio()
                    onSuccess()
                } else {
                    val err = parseError(resp.errorBody()?.string())
                        ?: "Error al registrar salida (${resp.code()})"
                    onError(err)
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de red al registrar salida manual")
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
        pedidoDisponible: PedidoDisponibleResponse? = null,
        onSuccess: (PedidoResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                if (pedidoDisponible != null) {
                    _pedidoDisponibleSeleccionado.value = pedidoDisponible
                }
                val resp = apiClient.repartidorApi.aceptarPedido(id)
                if (resp.isSuccessful && resp.body() != null) {
                    val pedido = resp.body()!!
                    _pedidoEnRuta.value = pedido
                    cargarPedidosDisponibles()
                    cargarMisEntregas()
                    onSuccess(pedido)
                } else {
                    val err = parseError(resp.errorBody()?.string())
                        ?: "No se pudo tomar el pedido. Puede que otro repartidor ya lo haya tomado."
                    _errorMessage.value = err
                    onError(err)
                }
            } catch (e: Exception) {
                val err = e.message ?: "Error al aceptar el pedido"
                _errorMessage.value = err
                onError(err)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ---------------------------------------------------------------- Navegación (CU-011/CU-006)

    private val _rutaNoDisponible = MutableStateFlow(false)
    /**
     * true si el backend no encontró ruta (422): el grafo de calles solo cubre Benito Juárez,
     * así que pasa si el repartidor está fuera de esa zona.
     */
    val rutaNoDisponible: StateFlow<Boolean> = _rutaNoDisponible.asStateFlow()

    private var origenRuta: com.htogo.app.data.location.Posicion? = null
    private var ultimaRutaMs = 0L
    private var calculandoRuta = false

    /**
     * Cada fix del GPS (lo reporta al backend [EntregaEnCursoService]): recalcula la ruta si el
     * repartidor se movió más de 60 m desde el último cálculo, como mucho cada 20 s (o si forzar = true).
     */
    fun alMoverse(idPedido: Int, p: com.htogo.app.data.location.Posicion, forzar: Boolean = false) {
        if (calculandoRuta && !forzar) return
        val ahora = ahoraMs()
        val origen = origenRuta
        val recalcular = forzar || origen == null || (ahora - ultimaRutaMs >= 20_000 &&
            com.htogo.app.data.location.distanciaM(origen.lat, origen.lon, p.lat, p.lon) > 60)
        if (!recalcular) return
        calculandoRuta = true
        viewModelScope.launch {
            try {
                // La ruta parte de la ubicación guardada en el backend: se reporta justo antes para
                // no depender de que el servicio ya haya enviado este mismo fix.
                apiClient.repartidorApi.reportarUbicacion(mapOf("lat" to p.lat, "lon" to p.lon))
                val resp = apiClient.repartidorApi.obtenerRuta(idPedido)
                if (resp.isSuccessful && resp.body() != null) {
                    _rutaCalculada.value = resp.body()
                    _rutaNoDisponible.value = false
                    origenRuta = p
                    ultimaRutaMs = ahora
                } else if (resp.code() == 422) {
                    _rutaCalculada.value = null
                    _rutaNoDisponible.value = true
                    // Se reintenta cuando se mueva lo suficiente (p. ej. al entrar a la zona).
                    origenRuta = p
                    ultimaRutaMs = ahora
                }
            } catch (_: Exception) {
                // Sin red: se reintenta con el siguiente fix del GPS.
            } finally {
                calculandoRuta = false
            }
        }
    }

    /**
     * Reporta la ubicación inicial (p. ej. la purificadora en FakeGPS) y espera la ruta A* calculada
     * por el backend desde ese punto hasta el domicilio del pedido.
     */
    suspend fun recalcularRutaDesde(
        idPedido: Int,
        p: com.htogo.app.data.location.Posicion
    ): List<Pair<Double, Double>> {
        calculandoRuta = true
        try {
            apiClient.repartidorApi.reportarUbicacion(mapOf("lat" to p.lat, "lon" to p.lon))
            val resp = apiClient.repartidorApi.obtenerRuta(idPedido)
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                _rutaCalculada.value = body
                _rutaNoDisponible.value = false
                origenRuta = p
                ultimaRutaMs = ahoraMs()
                return body.coordenadas.map { it.lat to it.lon }
            }
        } catch (_: Exception) {
        } finally {
            calculandoRuta = false
        }
        return _rutaCalculada.value?.coordenadas?.map { it.lat to it.lon }.orEmpty()
    }

    /** Envía al backend la posición simulada de FakeGPS para que el cliente la reciba en vivo por WebSocket. */
    fun reportarUbicacionSimulada(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                apiClient.repartidorApi.reportarUbicacion(mapOf("lat" to lat, "lon" to lon))
            } catch (_: Exception) {
                // Sin red: continúa la simulación local en el mapa.
            }
        }
    }

    /** Carga el detalle real de un pedido del repartidor (pedido apartado, o tras reiniciarse la app). */
    fun cargarPedidoEnRuta(idPedido: Int) {
        viewModelScope.launch {
            try {
                val resp = apiClient.pedidosApi.obtenerDetallePedido(idPedido)
                if (resp.isSuccessful && resp.body() != null) _pedidoEnRuta.value = resp.body()
            } catch (_: Exception) {
                // Sin red: la pantalla de ruta lo mostrará en cuanto haya datos.
            }
        }
    }

    /** Al salir de la pantalla de ruta: el próximo pedido empieza con ruta nueva. */
    fun detenerNavegacion() {
        _rutaNoDisponible.value = false
        origenRuta = null
        ultimaRutaMs = 0L
    }

    fun marcarEnCamino(
        id: Int,
        lat: Double,
        lon: Double,
        onSuccess: (PedidoResponse) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val resp = apiClient.repartidorApi.marcarEnCamino(id, mapOf("lat" to lat, "lon" to lon))
                if (resp.isSuccessful && resp.body() != null) {
                    _pedidoEnRuta.value = resp.body()!!
                    onSuccess(resp.body()!!)
                } else {
                    onError(parseError(resp.errorBody()?.string()) ?: "No se pudo iniciar el viaje (${resp.code()})")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de red al iniciar el viaje")
            }
        }
    }

    /**
     * CU-012: avisa al backend que el repartidor llegó (≤ 50 m) para que notifique al cliente.
     * [onError] recibe el código HTTP, o null si no hubo red.
     */
    fun avisarLlegada(
        id: Int,
        lat: Double,
        lon: Double,
        onSuccess: () -> Unit,
        onError: (String, Int?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val resp = apiClient.repartidorApi.avisarLlegada(id, mapOf("lat" to lat, "lon" to lon))
                if (resp.isSuccessful) {
                    onSuccess()
                } else {
                    onError(parseError(resp.errorBody()?.string()) ?: "No se pudo avisar al cliente (${resp.code()})", resp.code())
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de red al avisar al cliente", null)
            }
        }
    }

    fun cargarApartadosBase(
        id: Int,
        onSuccess: (PedidoResponse) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resp = apiClient.repartidorApi.cargarApartadosBase(id)
                if (resp.isSuccessful && resp.body() != null) {
                    _pedidoEnRuta.value = resp.body()!!
                    cargarInventarios()
                    onSuccess(resp.body()!!)
                } else {
                    onError(parseError(resp.errorBody()?.string()) ?: "No se pudieron cargar los apartados (${resp.code()})")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de red al cargar apartados de la base")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * CU-012. El backend exige la posición actual: para ENTREGADO valida que esté a ≤ 50 m del
     * domicilio (RF-014); para NO_ENTREGADO exige el motivo. Las líneas permiten ajustes en sitio
     * y cobro de envases nuevos (RN-032 / RN-033).
     */
    fun registrarResultadoEntrega(
        id: Int,
        esEntregado: Boolean,
        lat: Double,
        lon: Double,
        motivoNoEntrega: String? = null,
        lineas: List<com.htogo.app.data.dto.LineaResultadoDto>? = null,
        onSuccess: (PedidoResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val req = com.htogo.app.data.dto.ResultadoEntregaRequestDto(
                    resultado = if (esEntregado) "ENTREGADO" else "NO_ENTREGADO",
                    lat = lat,
                    lon = lon,
                    motivoNoEntrega = if (!esEntregado) (motivoNoEntrega ?: "Sin motivo") else null,
                    lineas = lineas
                )
                val resp = apiClient.repartidorApi.finalizarEntrega(id, req)
                if (resp.isSuccessful && resp.body() != null) {
                    _pedidoEnRuta.value = null
                    _pedidoDisponibleSeleccionado.value = null
                    _rutaCalculada.value = null
                    detenerNavegacion()
                    cargarPedidosDisponibles()
                    cargarMisEntregas()
                    cargarInventarios()
                    onSuccess(resp.body()!!)
                } else {
                    val err = parseError(resp.errorBody()?.string())
                        ?: "Error al registrar el resultado de entrega (${resp.code()})"
                    onError(err)
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de red al registrar entrega")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ---------------------------------------------------------------- Ruta del día (OR-Tools)

    private val _agenda = MutableStateFlow<List<AgendaPedidoDto>>(emptyList())
    /** Programados de hoy: libres para reservar o ya reservados por este repartidor. */
    val agenda: StateFlow<List<AgendaPedidoDto>> = _agenda.asStateFlow()

    private val _rutaOptima = MutableStateFlow<RutaOptimaDto?>(null)
    /** Orden óptimo de todas sus paradas; null mientras no se ha calculado. */
    val rutaOptima: StateFlow<RutaOptimaDto?> = _rutaOptima.asStateFlow()

    private val _calculandoPlan = MutableStateFlow(false)
    val calculandoPlan: StateFlow<Boolean> = _calculandoPlan.asStateFlow()

    private val _errorPlan = MutableStateFlow<String?>(null)
    val errorPlan: StateFlow<String?> = _errorPlan.asStateFlow()

    /** La entrega en curso salió de la ruta del día: al terminarla se regresa a la ruta. */
    var enRutaDelDia = false

    /**
     * Posición del FakeGPS compartida entre la ruta del día y la pantalla de entrega. Cada parada
     * abre una pantalla nueva: sin esto la simulación volvía a empezar en la purificadora en vez de
     * seguir desde el domicilio recién entregado. null = se usa el GPS real.
     */
    var ultimaPosicionSimulada: com.htogo.app.data.location.Posicion? = null

    /** Coordenadas de la base de la purificadora del repartidor (para partir de ahí en FakeGPS). */
    fun ubicacionPurificadora(): Pair<Double, Double>? {
        val dir = _miNegocio.value?.direccionObj
        if (dir?.lat != null && dir.lon != null && (dir.lat != 0.0 || dir.lon != 0.0)) {
            return dir.lat to dir.lon
        }
        val opt = _rutaOptima.value
        if (opt != null && (opt.origenLat != 0.0 || opt.origenLon != 0.0)) {
            return opt.origenLat to opt.origenLon
        }
        return null
    }

    /** Recarga la agenda y recalcula la ruta óptima (también tras un aviso de RN-009). */
    fun cargarPlan() {
        viewModelScope.launch {
            _calculandoPlan.value = true
            try {
                val agenda = apiClient.repartidorApi.agenda()
                if (agenda.isSuccessful) _agenda.value = agenda.body().orEmpty()
                val ruta = apiClient.repartidorApi.rutaOptima()
                if (ruta.isSuccessful && ruta.body() != null) {
                    _rutaOptima.value = ruta.body()
                    _errorPlan.value = null
                } else {
                    _errorPlan.value = parseError(ruta.errorBody()?.string())
                        ?: "No se pudo calcular la ruta (${ruta.code()})"
                }
            } catch (e: Exception) {
                _errorPlan.value = e.message ?: "Error de red al calcular la ruta"
            } finally {
                _calculandoPlan.value = false
            }
        }
    }

    /** Reporta una ubicación inicial (p. ej. la purificadora en modo FakeGPS) y recalcula la ruta OR-Tools desde ahí. */
    fun cargarPlanDesdeUbicacion(lat: Double, lon: Double, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _calculandoPlan.value = true
            try {
                apiClient.repartidorApi.reportarUbicacion(mapOf("lat" to lat, "lon" to lon))
                val agenda = apiClient.repartidorApi.agenda()
                if (agenda.isSuccessful) _agenda.value = agenda.body().orEmpty()
                val ruta = apiClient.repartidorApi.rutaOptima()
                if (ruta.isSuccessful && ruta.body() != null) {
                    _rutaOptima.value = ruta.body()
                    _errorPlan.value = null
                } else {
                    _errorPlan.value = parseError(ruta.errorBody()?.string())
                        ?: "No se pudo calcular la ruta (${ruta.code()})"
                }
            } catch (e: Exception) {
                _errorPlan.value = e.message ?: "Error de red al calcular la ruta"
            } finally {
                _calculandoPlan.value = false
                onComplete()
            }
        }
    }

    fun reservar(idPedido: Int, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val resp = apiClient.repartidorApi.reservar(idPedido)
                if (resp.isSuccessful) {
                    cargarPlan()
                    cargarMisEntregas()
                } else {
                    onError(parseError(resp.errorBody()?.string()) ?: "No se pudo reservar (${resp.code()})")
                    cargarPlan() // p. ej. otro repartidor lo tomó: refresca la agenda
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de red al reservar")
            }
        }
    }

    fun liberar(idPedido: Int, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val resp = apiClient.repartidorApi.liberar(idPedido)
                if (resp.isSuccessful) {
                    cargarPlan()
                    cargarMisEntregas()
                } else {
                    onError(parseError(resp.errorBody()?.string()) ?: "No se pudo liberar (${resp.code()})")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de red al liberar")
            }
        }
    }

    /**
     * Prepara la siguiente parada para la pantalla de ruta: un reservado se acepta (aparta los
     * garrafones) y uno ya asignado solo se carga. Al terminar la entrega se vuelve a la ruta.
     */
    fun iniciarParada(parada: ParadaDto, onListo: () -> Unit, onError: (String) -> Unit) {
        enRutaDelDia = true
        _pedidoDisponibleSeleccionado.value = null
        if (parada.estado == "pendiente" || parada.estado == "pendiente_programado") {
            aceptarPedido(parada.idPedido, onSuccess = { onListo() }, onError = onError)
            return
        }
        viewModelScope.launch {
            try {
                val resp = apiClient.pedidosApi.obtenerDetallePedido(parada.idPedido)
                if (resp.isSuccessful && resp.body() != null) {
                    _pedidoEnRuta.value = resp.body()
                    onListo()
                } else {
                    onError(parseError(resp.errorBody()?.string()) ?: "No se pudo abrir el pedido (${resp.code()})")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de red al abrir el pedido")
            }
        }
    }

    private fun parseError(errorJson: String?): String? {
        return mensajeDeError(errorJson)
    }

    // Al final de la clase: debe correr después de inicializar todos los StateFlow de arriba.
    // Igual que ClienteViewModel: recarga al iniciar sesión y limpia al cerrarla.
    init {
        viewModelScope.launch {
            sessionManager.token.collect { token ->
                if (token.isNullOrBlank()) limpiarDatosDeSesion() else recargarDatosDeSesion()
            }
        }
    }

    private fun recargarDatosDeSesion() {
        cargarPedidosDisponibles()
        cargarInventarios()
        cargarMarcas()
        cargarMiNegocio()
        cargarMisEntregas()
    }

    private fun limpiarDatosDeSesion() {
        _pedidosDisponibles.value = emptyList()
        _pedidoEnRuta.value = null
        _pedidoDisponibleSeleccionado.value = null
        _rutaCalculada.value = null
        _miNegocio.value = null
        _esDueno.value = false
        _misEntregas.value = emptyList()
        _totalEntregas.value = 0
        _inventarioBase.value = null
        _inventarioVehiculo.value = null
        _solicitudes.value = emptyList()
        _agenda.value = emptyList()
        _rutaOptima.value = null
        _errorPlan.value = null
        enRutaDelDia = false
        detenerNavegacion()
    }
}
