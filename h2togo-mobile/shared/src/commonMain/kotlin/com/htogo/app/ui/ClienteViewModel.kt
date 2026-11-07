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
import kotlinx.coroutines.joinAll
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

    // Pedido visualizado en pantalla de seguimiento (permite inspeccionar pedidos específicos o cerrados sin colisionar con el activo)
    private val _pedidoSeguimiento = MutableStateFlow<PedidoResponse?>(null)
    val pedidoSeguimiento: StateFlow<PedidoResponse?> = _pedidoSeguimiento.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Purificadora preseleccionada para CU-022 -> CU-004
    var purificadoraSeleccionadaId: Int? = null
    var purificadoraSeleccionadaNombre: String? = null
    var purificadoraSeleccionadaDistancia: String? = null


    private fun prioridadEstadoActivo(estado: String): Int = when (estado.lowercase()) {
        "en_camino" -> 4
        "asignado" -> 3
        "pendiente" -> 2
        "pendiente_programado" -> 1
        else -> 0
    }

    fun cargarHistorial(): Job {
        return viewModelScope.launch {
            try {
                val resp = apiClient.pedidosApi.obtenerHistorialCliente(page = 0, size = 50)
                if (resp.isSuccessful && resp.body() != null) {
                    val elementos = resp.body()?.elementos ?: emptyList()
                    _historial.value = elementos
                    val candidato = elementos
                        .filter { prioridadEstadoActivo(it.estado) > 0 && cierreConfirmadoPorPedido[it.id] == null }
                        .maxWithOrNull(compareBy<PedidoResumenDto> { prioridadEstadoActivo(it.estado) }.thenBy { it.id })
                    val actual = _pedidoActivo.value
                    if (candidato != null) {
                        if (actual == null || actual.id != candidato.id || !actual.estado.equals(candidato.estado, ignoreCase = true)) {
                            refrescarDetalle(candidato.id)
                        }
                    } else if (actual != null && actual.estado.lowercase() !in estadosCerrados) {
                        refrescarDetalle(actual.id)
                    }
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

    fun cargarDirecciones(onComplete: (() -> Unit)? = null): Job {
        return viewModelScope.launch {
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

    private val estadosCerrados = setOf("entregado", "no_entregado", "cancelado")
    private val cierreConfirmadoPorPedido = mutableMapOf<Int, String>()

    fun cargarDetallePedido(id: Int, onComplete: ((PedidoResponse?) -> Unit)? = null) {
        viewModelScope.launch {
            val resultado = refrescarDetalle(id, comoSeguimiento = true)
            onComplete?.invoke(resultado)
        }
    }

    private fun debePromoverComoPedidoActivo(candidato: PedidoResponse): Boolean {
        val estCand = candidato.estado.lowercase()
        if (estCand in estadosCerrados) return false
        val actual = _pedidoActivo.value ?: return true
        if (actual.id == candidato.id) return true
        val estActual = actual.estado.lowercase()
        if (estActual in estadosCerrados) return true
        val prioCand = prioridadEstadoActivo(estCand)
        val prioAct = prioridadEstadoActivo(estActual)
        return prioCand > prioAct || (prioCand == prioAct && candidato.id > actual.id)
    }

    private suspend fun refrescarDetalle(id: Int, comoSeguimiento: Boolean = false): PedidoResponse? {
        try {
            val esSeguimientoActual = comoSeguimiento ||
                observadoresRastreo["seguimiento"] == id ||
                idPedidoRastreado == id ||
                _pedidoSeguimiento.value?.id == id ||
                _pedidoSeguimiento.value == null
            val estadoPrevio = cierreConfirmadoPorPedido[id]
                ?: _pedidoSeguimiento.value?.takeIf { it.id == id }?.estado?.lowercase()?.takeIf { it in estadosCerrados }
                ?: _pedidoActivo.value?.takeIf { it.id == id }?.estado?.lowercase()?.takeIf { it in estadosCerrados }
            val resp = apiClient.pedidosApi.obtenerDetallePedido(id)
            if (resp.isSuccessful && resp.body() != null) {
                val recibido = resp.body()!!
                val estadoRecibido = recibido.estado.lowercase()
                val aAsignar = if (estadoRecibido in estadosCerrados) {
                    cierreConfirmadoPorPedido[id] = estadoRecibido
                    recibido
                } else if (estadoPrevio != null) {
                    // Protege contra respuestas en vuelo previas al commit de la BD para que nunca regrese a "en_camino"
                    val preliminar = recibido.copy(estado = estadoPrevio)
                    if (esSeguimientoActual) {
                        _pedidoSeguimiento.value = preliminar
                    }
                    if (_pedidoActivo.value?.id == id) {
                        _pedidoActivo.value = preliminar
                    }
                    delay(450)
                    val respReintento = apiClient.pedidosApi.obtenerDetallePedido(id)
                    val bodyReintento = respReintento.body()
                    if (respReintento.isSuccessful && bodyReintento != null) {
                        val estReintento = bodyReintento.estado.lowercase()
                        if (estReintento in estadosCerrados) {
                            cierreConfirmadoPorPedido[id] = estReintento
                            bodyReintento
                        } else {
                            bodyReintento.copy(estado = estadoPrevio)
                        }
                    } else {
                        preliminar
                    }
                } else {
                    recibido
                }
                if (esSeguimientoActual) {
                    _pedidoSeguimiento.value = aAsignar
                }
                if (_pedidoActivo.value?.id == id || debePromoverComoPedidoActivo(aAsignar)) {
                    _pedidoActivo.value = aAsignar
                }
                return aAsignar
            }
        } catch (e: Exception) {
            // Error
        }
        return _pedidoSeguimiento.value?.takeIf { it.id == id } ?: _pedidoActivo.value?.takeIf { it.id == id }
    }

    private fun aplicarCierrePedidoEnVivo(idPedido: Int, nuevoEstado: String) {
        val estadoNorm = nuevoEstado.lowercase()
        cierreConfirmadoPorPedido[idPedido] = estadoNorm
        rutaSeguimientoSeq++
        _repartidorLlego.value = false
        _ubicacionRepartidor.value = null
        _rutaSeguimiento.value = emptyList()
        _distanciaRutaKm.value = null
        origenRutaSeguimiento = null
        observadoresRastreo.values.removeAll { it == idPedido }
        _pedidoSeguimiento.value = _pedidoSeguimiento.value?.takeIf { it.id == idPedido }?.copy(estado = estadoNorm)
        _pedidoActivo.value = _pedidoActivo.value?.takeIf { it.id == idPedido }?.copy(estado = estadoNorm)
        detenerRastreo()
        viewModelScope.launch {
            delay(250)
            refrescarDetalle(idPedido)
            cargarHistorial()
        }
    }

    // ---------------------------------------------------------------- Rastreo en vivo (CU-006)

    /** Posición del repartidor que llega por el tópico del pedido. */
    /** [recibidaMs]: cuándo llegó a la app; sirve para avisar si la posición quedó vieja (CU-006 S1). */
    data class UbicacionEnVivo(
        val lat: Double,
        val lon: Double,
        val recibidaMs: Long = ahoraMs(),
        val timestampServidor: String? = null
    )

    private val _ubicacionRepartidor = MutableStateFlow<UbicacionEnVivo?>(null)
    val ubicacionRepartidor: StateFlow<UbicacionEnVivo?> = _ubicacionRepartidor.asStateFlow()

    private var rastreoJob: Job? = null
    private var idPedidoRastreado: Int? = null
    private var ultimoPedidoRastreadoId: Int? = null
    private val observadoresRastreo = mutableMapOf<String, Int>()

    private fun objetivoRastreoPreferido(): Int? =
        observadoresRastreo["seguimiento"] ?: observadoresRastreo["home"] ?: observadoresRastreo.values.lastOrNull()

    private fun estadoPedidoRastreado(idPedido: Int): String? =
        (_pedidoSeguimiento.value?.takeIf { it.id == idPedido }
            ?: _pedidoActivo.value?.takeIf { it.id == idPedido })
            ?.estado?.lowercase()

    private val _repartidorLlego = MutableStateFlow(false)
    /** CU-012: el repartidor avisó que ya está en el domicilio ("Llegué al destino"). */
    val repartidorLlego: StateFlow<Boolean> = _repartidorLlego.asStateFlow()

    private val _rutaSeguimiento = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    /** Ruta por calles del repartidor al domicilio; vacía mientras no hay (el mapa dibuja línea recta). */
    val rutaSeguimiento: StateFlow<List<Pair<Double, Double>>> = _rutaSeguimiento.asStateFlow()

    private val _distanciaRutaKm = MutableStateFlow<Double?>(null)
    /** Distancia total de la ruta por calles calculada por el backend (km). */
    val distanciaRutaKm: StateFlow<Double?> = _distanciaRutaKm.asStateFlow()

    private var origenRutaSeguimiento: UbicacionEnVivo? = null
    private var rutaSeguimientoMs = 0L
    private var rutaSeguimientoSeq = 0L

    /** Vincula una pantalla ("home" o "seguimiento") al rastreo en vivo del pedido sin reiniciar al navegar entre ambas. */
    fun vincularRastreo(origen: String, idPedido: Int) {
        if (cierreConfirmadoPorPedido[idPedido] != null) return
        observadoresRastreo[origen] = idPedido
        val objetivo = objetivoRastreoPreferido() ?: idPedido
        if (idPedidoRastreado != objetivo || rastreoJob?.isActive != true) {
            iniciarRastreo(objetivo)
        }
    }

    fun desvincularRastreo(origen: String) {
        observadoresRastreo.remove(origen)
        val siguiente = objetivoRastreoPreferido()
        if (siguiente == null) {
            detenerRastreo()
        } else if (idPedidoRastreado != siguiente || rastreoJob?.isActive != true) {
            iniciarRastreo(siguiente)
        }
    }

    /**
     * Mientras el pedido está activo: escucha el WebSocket (reconecta si se cae) y respalda con
     * consultas periódicas de posición y estado para que tanto Inicio ("Pedido en curso") como
     * Seguimiento avancen en tiempo real (pendiente → asignado → en camino → entregado) y actualicen el ETA.
     */
    fun iniciarRastreo(idPedido: Int) {
        if (idPedidoRastreado == idPedido && rastreoJob?.isActive == true) return
        rastreoJob?.cancel()
        if (ultimoPedidoRastreadoId != idPedido) {
            rutaSeguimientoSeq++
            _ubicacionRepartidor.value = null
            _repartidorLlego.value = false
            _rutaSeguimiento.value = emptyList()
            _distanciaRutaKm.value = null
            origenRutaSeguimiento = null
            ultimoPedidoRastreadoId = idPedido
        }
        if (cierreConfirmadoPorPedido[idPedido] != null) {
            idPedidoRastreado = null
            return
        }
        val token = sessionManager.obtenerToken() ?: return
        idPedidoRastreado = idPedido
        rastreoJob = viewModelScope.launch {
            var conectado = false
            var ultimoMensajeWsMs = 0L

            // Avisos instantáneos por el canal de notificaciones (/user/queue/notificaciones).
            launch {
                CanalNotificaciones.avisos.collect { a ->
                    if (a.datos["idPedido"] != idPedido.toString()) return@collect
                    when (a.datos["tipo"]) {
                        "repartidor_llego" -> {
                            if (cierreConfirmadoPorPedido[idPedido] == null) {
                                _repartidorLlego.value = true
                            }
                        }
                        "pedido_entregado" -> aplicarCierrePedidoEnVivo(idPedido, "entregado")
                        "pedido_no_entregado" -> aplicarCierrePedidoEnVivo(idPedido, "no_entregado")
                        "pedido_cancelado" -> aplicarCierrePedidoEnVivo(idPedido, "cancelado")
                        "pedido_en_camino", "pedido_asignado" -> {
                            if (cierreConfirmadoPorPedido[idPedido] == null) {
                                refrescarDetalle(idPedido)
                                consultarUbicacion(idPedido)
                                actualizarRutaSeguimiento(idPedido, _ubicacionRepartidor.value)
                            }
                        }
                    }
                }
            }

            // Consulta inicial inmediata de detalle, ubicación y ruta
            launch {
                refrescarDetalle(idPedido, comoSeguimiento = observadoresRastreo["seguimiento"] == idPedido)
                consultarUbicacion(idPedido)
                if (estadoPedidoRastreado(idPedido) in setOf("asignado", "en_camino")) {
                    actualizarRutaSeguimiento(idPedido, _ubicacionRepartidor.value)
                }
            }

            // Ruta por calles: se actualiza cuando el repartidor avanza > 45 m (máx. cada 5 s),
            // de inmediato si saltó > 120 m (p. ej. tras reconexión) o si _rutaSeguimiento está vacía.
            launch {
                combine(_pedidoSeguimiento, _pedidoActivo, _ubicacionRepartidor) { seg, act, u ->
                    val pedido = seg?.takeIf { it.id == idPedido } ?: act?.takeIf { it.id == idPedido }
                    pedido to u
                }.collect { (pedido, u) ->
                    val est = pedido?.estado?.lowercase()
                    if (est !in setOf("asignado", "en_camino")) return@collect
                    val necesitaRuta = _rutaSeguimiento.value.isEmpty()
                    val previo = origenRutaSeguimiento
                    val distCambio = if (previo != null && u != null) {
                        com.htogo.app.data.location.distanciaM(previo.lat, previo.lon, u.lat, u.lon)
                    } else Double.MAX_VALUE
                    val movido = previo == null || distCambio > 45
                    val saltoReconexion = distCambio > 120
                    if (necesitaRuta || saltoReconexion || (movido && ahoraMs() - rutaSeguimientoMs >= 5_000)) {
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
                                is EventoStomp.Conectado -> {
                                    conectado = true
                                    launch {
                                        consultarUbicacion(idPedido)
                                        if (estadoPedidoRastreado(idPedido) in setOf("asignado", "en_camino") && _rutaSeguimiento.value.isEmpty()) {
                                            actualizarRutaSeguimiento(idPedido, _ubicacionRepartidor.value)
                                        }
                                    }
                                }
                                is EventoStomp.Mensaje -> {
                                    when (extraerEventoTopico(evento.cuerpo)) {
                                        "llegada" -> {
                                            if (cierreConfirmadoPorPedido[idPedido] == null) {
                                                _repartidorLlego.value = true
                                            }
                                        }
                                        "entregado", "no_entregado" -> {
                                            val nuevoEstado = extraerEventoTopico(evento.cuerpo)!!
                                            aplicarCierrePedidoEnVivo(idPedido, nuevoEstado)
                                        }
                                        else -> {
                                            if (cierreConfirmadoPorPedido[idPedido] == null) {
                                                parsearUbicacion(evento.cuerpo)?.let { nueva ->
                                                    ultimoMensajeWsMs = ahoraMs()
                                                    _ubicacionRepartidor.value = nueva
                                                    // Si llegan coordenadas por el tópico y aún aparecía pendiente/asignado, sincroniza estado
                                                    if (estadoPedidoRastreado(idPedido) in setOf(null, "pendiente", "pendiente_programado", "asignado")) {
                                                        launch { refrescarDetalle(idPedido) }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                else -> Unit
                            }
                        }
                    conectado = false
                    delay(2_500) // reintento rápido tras caída de red o rechazo del servidor
                }
            }

            // Respaldo de ubicación por HTTP únicamente cuando el WebSocket no está activo o lleva ≥ 8 s sin emitir
            launch {
                while (isActive) {
                    val espera = if (_ubicacionRepartidor.value == null || !conectado) 2_500L else 5_000L
                    delay(espera)
                    val wsActivoReciente = conectado && _ubicacionRepartidor.value != null && (ahoraMs() - ultimoMensajeWsMs) < 8_000L
                    if (wsActivoReciente) continue
                    val est = estadoPedidoRastreado(idPedido)
                    if (est == null || est in setOf("asignado", "en_camino")) {
                        consultarUbicacion(idPedido)
                    }
                }
            }

            // Refresco periódico de respaldo del detalle del pedido (cada 4 s) y reintento de ruta si el repartidor se movió
            launch {
                while (isActive) {
                    delay(4_000)
                    val estadoAntes = estadoPedidoRastreado(idPedido)
                    refrescarDetalle(idPedido, comoSeguimiento = observadoresRastreo["seguimiento"] == idPedido)
                    val estado = estadoPedidoRastreado(idPedido)
                    if (estado != estadoAntes && estado in setOf("asignado", "en_camino") &&
                        (_ubicacionRepartidor.value == null || (ahoraMs() - ultimoMensajeWsMs) >= 8_000L)
                    ) {
                        consultarUbicacion(idPedido)
                    }
                    val uActual = _ubicacionRepartidor.value
                    val origRuta = origenRutaSeguimiento
                    val desfasada = uActual != null && (origRuta == null ||
                        com.htogo.app.data.location.distanciaM(origRuta.lat, origRuta.lon, uActual.lat, uActual.lon) > 45)
                    if ((_rutaSeguimiento.value.isEmpty() || desfasada) &&
                        estado in setOf("asignado", "en_camino") &&
                        ahoraMs() - rutaSeguimientoMs >= 5_000L
                    ) {
                        actualizarRutaSeguimiento(idPedido, uActual)
                    }
                    if (estado in setOf("entregado", "no_entregado", "cancelado")) {
                        _ubicacionRepartidor.value = null
                        cargarHistorial()
                        detenerRastreo() // este pedido puntual se cerró: corta WebSocket y consultas
                    }
                }
            }
        }
    }

    private suspend fun actualizarRutaSeguimiento(idPedido: Int, desde: UbicacionEnVivo?) {
        rutaSeguimientoMs = ahoraMs()
        val seq = ++rutaSeguimientoSeq
        try {
            val resp = apiClient.pedidosApi.rutaSeguimiento(idPedido, desde?.lat, desde?.lon)
            if (seq != rutaSeguimientoSeq || idPedidoRastreado != idPedido) return
            val ruta = resp.body()
            if (resp.isSuccessful && ruta != null && ruta.encontrada && ruta.coordenadas.size >= 2) {
                val primero = ruta.coordenadas.first()
                // Si el servidor aún no tenía el nuevo punto del repartidor en BD, evita sobrescribir con una ruta lejana.
                val origenDesfasado = desde != null &&
                    com.htogo.app.data.location.enZonaGrafoOsm(desde.lat, desde.lon) &&
                    com.htogo.app.data.location.distanciaM(desde.lat, desde.lon, primero.lat, primero.lon) > 450.0
                if (!origenDesfasado) {
                    _rutaSeguimiento.value = ruta.coordenadas.map { it.lat to it.lon }
                    if (ruta.distanciaTotalKm > 0.0) {
                        _distanciaRutaKm.value = ruta.distanciaTotalKm
                    }
                    origenRutaSeguimiento = desde ?: UbicacionEnVivo(primero.lat, primero.lon)
                }
            }
        } catch (_: Exception) {
            // Sin red: se reintenta en el próximo ciclo sin borrar la ruta previa
        }
    }

    /** Calcula la ruta A* por calles desde un punto explícito (p. ej. la purificadora en modo FakeGPS del cliente). */
    suspend fun obtenerRutaSeguimientoDesde(
        idPedido: Int,
        origenLat: Double,
        origenLon: Double
    ): List<Pair<Double, Double>> {
        return try {
            val resp = apiClient.pedidosApi.rutaSeguimiento(idPedido, origenLat, origenLon)
            val ruta = resp.body()
            if (resp.isSuccessful && ruta != null && ruta.encontrada && ruta.coordenadas.size >= 2) {
                val primero = ruta.coordenadas.first()
                if (!com.htogo.app.data.location.enZonaGrafoOsm(origenLat, origenLon) ||
                    com.htogo.app.data.location.distanciaM(origenLat, origenLon, primero.lat, primero.lon) <= 450.0
                ) {
                    ruta.coordenadas.map { it.lat to it.lon }
                } else emptyList()
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun detenerRastreo() {
        rutaSeguimientoSeq++
        rastreoJob?.cancel()
        rastreoJob = null
        idPedidoRastreado = null
    }

    /** El tópico del pedido lleva {"evento":"llegada" | "entregado" | "no_entregado"}. */
    private fun extraerEventoTopico(json: String): String? = try {
        com.htogo.shared.data.jsonApi.parseToJsonElement(json).jsonObject["evento"]?.jsonPrimitive?.content
    } catch (e: Exception) {
        null
    }

    private fun parsearUbicacion(json: String): UbicacionEnVivo? = try {
        val o = com.htogo.shared.data.jsonApi.parseToJsonElement(json).jsonObject
        val ts = o["timestamp"]?.jsonPrimitive?.content
        UbicacionEnVivo(o.getValue("lat").jsonPrimitive.double, o.getValue("lon").jsonPrimitive.double, ahoraMs(), ts)
    } catch (e: Exception) {
        null
    }

    private suspend fun consultarUbicacion(idPedido: Int) {
        if (cierreConfirmadoPorPedido[idPedido] != null) return
        try {
            val resp = apiClient.pedidosApi.ubicacionRepartidor(idPedido)
            resp.body()?.let { u ->
                if (cierreConfirmadoPorPedido[idPedido] != null) return
                if (u.llegoEn != null) _repartidorLlego.value = true
                val previa = _ubicacionRepartidor.value
                val tsPrevio = previa?.timestampServidor?.let {
                    runCatching { kotlinx.datetime.Instant.parse(it) }.getOrNull()
                }
                val tsNuevo = u.timestamp?.let {
                    runCatching { kotlinx.datetime.Instant.parse(it) }.getOrNull()
                }
                // Evita regresar el marcador/ruta a una posición rezagada de PostgreSQL (throttle de 5 s) frente al WebSocket en vivo.
                if (tsPrevio != null && tsNuevo != null && tsNuevo < tsPrevio) return
                if (previa != null && tsNuevo == null && (ahoraMs() - previa.recibidaMs) < 8_000L) return
                val movio = previa == null ||
                    com.htogo.app.data.location.distanciaM(previa.lat, previa.lon, u.lat, u.lon) > 0.5
                val tsAvanzo = tsNuevo != null && (tsPrevio == null || tsNuevo > tsPrevio)
                if (movio || tsAvanzo) {
                    _ubicacionRepartidor.value = UbicacionEnVivo(u.lat, u.lon, ahoraMs(), u.timestamp)
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

    private val _refrescando = MutableStateFlow(false)
    /** Deslizar hacia abajo para actualizar: indicador visible mientras se recargan los datos. */
    val refrescando: StateFlow<Boolean> = _refrescando.asStateFlow()

    // Al final de la clase: debe correr después de inicializar todos los StateFlow de arriba.
    // El ViewModel se crea al abrir la app (antes del login), así que no basta con cargar una vez:
    // se recarga cada vez que cambia la sesión y se limpia al cerrarla (evita ver datos de otra cuenta).
    init {
        viewModelScope.launch {
            sessionManager.token.collect { token ->
                if (token.isNullOrBlank()) limpiarDatosDeSesion() else recargarDatosDeSesion()
            }
        }
        refrescarAlRecibirAvisos()
    }

    private fun recargarDatosDeSesion() {
        cargarMarcas()
        cargarDirecciones()
        cargarHistorial()
    }

    /** Recarga pedidos, direcciones y purificadoras cercanas (y el pedido abierto, si hay). */
    fun refrescar() {
        if (_refrescando.value) return
        viewModelScope.launch {
            _refrescando.value = true
            try {
                val trabajos = mutableListOf(cargarHistorial(), cargarDirecciones())
                _pedidoActivo.value?.id?.let { id -> trabajos += launch { refrescarDetalle(id) } }
                trabajos.joinAll()
            } finally {
                _refrescando.value = false
            }
        }
    }

    /**
     * Cualquier aviso de un pedido (asignado, en camino, llegada, entregado…) actualiza de inmediato
     * tanto el pedido activo ("Pedido en curso" en Inicio) como el historial, sin tener que deslizar.
     */
    private fun refrescarAlRecibirAvisos() {
        viewModelScope.launch {
            CanalNotificaciones.avisos.collect { aviso ->
                val tipo = aviso.datos["tipo"]
                val idPed = aviso.datos["idPedido"]?.toIntOrNull()
                if (idPed != null) {
                    when (tipo) {
                        "pedido_entregado" -> aplicarCierrePedidoEnVivo(idPed, "entregado")
                        "pedido_no_entregado" -> aplicarCierrePedidoEnVivo(idPed, "no_entregado")
                        "pedido_cancelado" -> aplicarCierrePedidoEnVivo(idPed, "cancelado")
                        "pedido_asignado" -> if (cierreConfirmadoPorPedido[idPed] == null) {
                            _pedidoActivo.value?.takeIf {
                                it.id == idPed && it.estado.lowercase() in setOf("pendiente", "pendiente_programado")
                            }?.let { _pedidoActivo.value = it.copy(estado = "asignado") }
                            _pedidoSeguimiento.value?.takeIf {
                                it.id == idPed && it.estado.lowercase() in setOf("pendiente", "pendiente_programado")
                            }?.let { _pedidoSeguimiento.value = it.copy(estado = "asignado") }
                            launch {
                                refrescarDetalle(idPed)
                                if (idPedidoRastreado == idPed) {
                                    consultarUbicacion(idPed)
                                    actualizarRutaSeguimiento(idPed, _ubicacionRepartidor.value)
                                }
                            }
                        }
                        "pedido_en_camino" -> if (cierreConfirmadoPorPedido[idPed] == null) {
                            _pedidoActivo.value?.takeIf {
                                it.id == idPed && it.estado.lowercase() !in estadosCerrados
                            }?.let { _pedidoActivo.value = it.copy(estado = "en_camino") }
                            _pedidoSeguimiento.value?.takeIf {
                                it.id == idPed && it.estado.lowercase() !in estadosCerrados
                            }?.let { _pedidoSeguimiento.value = it.copy(estado = "en_camino") }
                            launch {
                                refrescarDetalle(idPed)
                                if (idPedidoRastreado == idPed) {
                                    consultarUbicacion(idPed)
                                    actualizarRutaSeguimiento(idPed, _ubicacionRepartidor.value)
                                }
                            }
                        }
                        "repartidor_llego" -> if (cierreConfirmadoPorPedido[idPed] == null && (_pedidoActivo.value == null || _pedidoActivo.value?.id == idPed)) {
                            _repartidorLlego.value = true
                            launch { refrescarDetalle(idPed) }
                        }
                    }
                }
                if (tipo?.startsWith("pedido_") == true || tipo == "repartidor_llego") cargarHistorial()
            }
        }
    }

    private fun limpiarDatosDeSesion() {
        observadoresRastreo.clear()
        detenerRastreo()
        ultimoPedidoRastreadoId = null
        cierreConfirmadoPorPedido.clear()
        _direcciones.value = emptyList()
        _historial.value = emptyList()
        _pedidoActivo.value = null
        _pedidoSeguimiento.value = null
        _ubicacionRepartidor.value = null
        _repartidorLlego.value = false
        _rutaSeguimiento.value = emptyList()
        _distanciaRutaKm.value = null
    }
}
