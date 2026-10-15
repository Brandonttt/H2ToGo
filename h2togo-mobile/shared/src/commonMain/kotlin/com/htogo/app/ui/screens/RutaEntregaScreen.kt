package com.htogo.app.ui.screens

import androidx.compose.ui.platform.LocalUriHandler
import com.htogo.app.plataforma.AppNavegacion
import com.htogo.app.plataforma.LocalUiPlataforma
import com.htogo.app.plataforma.Plataforma
import com.htogo.app.plataforma.mostrarAviso
import com.htogo.app.plataforma.ahoraMs
import com.htogo.app.util.format

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.htogo.app.data.dto.DetalleResponse
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.htogo.app.data.local.LlegadaEntrega
import com.htogo.app.data.location.EntregaEnCurso
import com.htogo.app.data.location.UbicacionTracker
import com.htogo.app.ui.RepartidorViewModel
import com.htogo.app.ui.components.OsmRouteMapView
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull

enum class EstadoRuta { EN_RUTA, LLEGADO }

data class EntregaActiva(
    val pedidoId: String,
    val cliente: String,
    val inicialesCliente: String,
    val telefono: String?,
    val direccion: String,
    val notas: String,
    val productos: String,
    val garrafonesTotales: Int = 1,
    val total: Double,
    val entregaActual: Int,
    val totalEntregas: Int,
    val latDestino: Double = 19.38204,
    val lonDestino: Double = -99.16202
)

enum class MotivoNoEntrega(val titulo: String, val subtitulo: String) {
    CLIENTE_NO_ENCONTRADO("Cliente no se encontraba", "Toqué el timbre y nadie respondió"),
    DIRECCION_INCORRECTA("Dirección incorrecta", "No pude localizar la ubicación"),
    CLIENTE_RECHAZO("Cliente rechazó el pedido", "No quiso recibir los garrafones"),
    OTRO("Otro motivo", "Especificar en notas")
}

@Composable
fun RutaEntregaScreen(
    onBack: () -> Unit = {},
    onCompletada: () -> Unit = {},
    repartidorViewModel: RepartidorViewModel = viewModel { RepartidorViewModel() }
) {
    val livePedidoEnRuta by repartidorViewModel.pedidoEnRuta.collectAsState()
    val livePedidoDisponible by repartidorViewModel.pedidoDisponibleSeleccionado.collectAsState()
    val liveRutaCalculada by repartidorViewModel.rutaCalculada.collectAsState()
    val isLoading by repartidorViewModel.isLoading.collectAsState()

    val marcas by repartidorViewModel.marcas.collectAsState()
    val nombresMarca = remember(marcas) { marcas.associate { it.id to it.nombre } }
    val entrega = remember(livePedidoEnRuta, livePedidoDisponible, nombresMarca) {
        val pEnRuta = livePedidoEnRuta
        val pDisp = livePedidoDisponible

        val idStr = pEnRuta?.id?.toString() ?: pDisp?.id?.toString() ?: "1287"
        val nombre = pEnRuta?.nombreCliente?.takeIf { it.isNotBlank() }
            ?: pDisp?.nombreCliente?.takeIf { it.isNotBlank() }
            ?: "Cliente H2ToGo"

        val iniciales = nombre.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .ifEmpty { "C" }

        val tel = pEnRuta?.telefonoCliente ?: pDisp?.telefonoCliente

        val dir = pEnRuta?.direccionTexto?.takeIf { it.isNotBlank() }
            ?: pDisp?.direccionResumen?.takeIf { it.isNotBlank() }
            ?: "Insurgentes Sur, Benito Juárez, CDMX"

        val notasTxt = pEnRuta?.indicaciones?.takeIf { it.isNotBlank() }
            ?: "Entregar en domicilio especificado"

        val prods = when {
            !pEnRuta?.detalles.isNullOrEmpty() -> {
                pEnRuta!!.detalles!!.joinToString(", ") { d ->
                    "${d.cantidad} × ${d.nombreMarca ?: nombresMarca[d.idMarca] ?: "Garrafón 20 L"}"
                }
            }
            !pDisp?.detalles.isNullOrEmpty() -> {
                pDisp!!.detalles!!.joinToString(", ") { d ->
                    "${d.cantidad} × ${d.nombreMarca ?: nombresMarca[d.idMarca] ?: "Garrafón 20 L"}"
                }
            }
            (pEnRuta?.garrafonesTotales ?: 0) > 0 -> {
                "${pEnRuta!!.garrafonesTotales} × Garrafón 20 L"
            }
            (pDisp?.garrafonesTotales ?: 0) > 0 -> {
                "${pDisp!!.garrafonesTotales} × Garrafón 20 L"
            }
            else -> "2 × Garrafón 20 L"
        }

        val totalMonto = pEnRuta?.totalPagar ?: pDisp?.totalEstimado ?: 90.0

        val latDest = pEnRuta?.latEntrega ?: pDisp?.latEntrega ?: 19.38204
        val lonDest = pEnRuta?.lonEntrega ?: pDisp?.lonEntrega ?: -99.16202

        val cantGarrafones: Int = when {
            !pEnRuta?.detalles.isNullOrEmpty() -> pEnRuta!!.detalles!!.sumOf { it.cantidad }
            !pDisp?.detalles.isNullOrEmpty() -> pDisp!!.detalles!!.sumOf { it.cantidad }
            (pEnRuta?.garrafonesTotales ?: 0) > 0 -> pEnRuta?.garrafonesTotales ?: 1
            (pDisp?.garrafonesTotales ?: 0) > 0 -> pDisp?.garrafonesTotales ?: 1
            else -> 1
        }

        EntregaActiva(
            pedidoId = idStr,
            cliente = nombre,
            inicialesCliente = iniciales,
            telefono = tel,
            direccion = dir,
            notas = notasTxt,
            productos = prods,
            garrafonesTotales = cantGarrafones,
            total = totalMonto,
            entregaActual = 1,
            totalEntregas = 1,
            latDestino = latDest,
            lonDestino = lonDest
        )
    }

    val routePoints = remember(liveRutaCalculada) {
        liveRutaCalculada?.coordenadas?.map { Pair(it.lat, it.lon) } ?: emptyList()
    }

    // ---- GPS real del repartidor (con soporte para modo de simulación de prueba) ----
    var posicionSimulada by remember { mutableStateOf<com.htogo.app.data.location.Posicion?>(null) }
    val posicionReal by EntregaEnCurso.posicion.collectAsState()
    val posicion = posicionSimulada ?: posicionReal

    var tienePermiso by remember { mutableStateOf(UbicacionTracker.tienePermiso()) }
    val pedirPermiso = LocalUiPlataforma.current.rememberSolicitudUbicacion { tienePermiso = it }
    LaunchedEffect(Unit) {
        if (!tienePermiso) {
            pedirPermiso()
        }
    }
    LaunchedEffect(entrega.pedidoId) {
        val idInt = entrega.pedidoId.toIntOrNull()
        if (idInt != null && (livePedidoEnRuta?.detalles.isNullOrEmpty())) {
            repartidorViewModel.cargarPedidoEnRuta(idInt)
        }
    }
    var errorCargaBase by remember { mutableStateOf<String?>(null) }
    var enCaminoSolicitado by remember(entrega.pedidoId) { mutableStateOf(false) }

    // Si se activa la simulación, reportamos la posición simulada al backend y recalculamos ruta
    LaunchedEffect(posicionSimulada) {
        val sim = posicionSimulada ?: return@LaunchedEffect
        val idInt = entrega.pedidoId.toIntOrNull() ?: return@LaunchedEffect
        if (!enCaminoSolicitado && livePedidoEnRuta?.estado.equals("asignado", ignoreCase = true)) {
            enCaminoSolicitado = true
            repartidorViewModel.marcarEnCamino(idInt, sim.lat, sim.lon, onError = { err ->
                enCaminoSolicitado = false
                if (err.contains("apartados en la base", ignoreCase = true) || err.contains("CARGA_PENDIENTE", ignoreCase = true)) {
                    errorCargaBase = err
                }
            })
        }
        repartidorViewModel.alMoverse(idInt, sim, forzar = true)
    }

    // El GPS vive en un servicio en primer plano: sigue reportando aunque se abra Waze/Maps.
    LaunchedEffect(tienePermiso, entrega.pedidoId) {
        val idInt = entrega.pedidoId.toIntOrNull() ?: return@LaunchedEffect
        if (!tienePermiso) return@LaunchedEffect
        EntregaEnCurso.iniciar(idInt)
        EntregaEnCurso.posicion.filterNotNull().collect { p ->
            if (posicionSimulada == null) {
                // Con el primer fix el pedido pasa a "en camino": el cliente empieza a ver el rastreo.
                if (!enCaminoSolicitado && livePedidoEnRuta?.estado.equals("asignado", ignoreCase = true)) {
                    enCaminoSolicitado = true
                    repartidorViewModel.marcarEnCamino(idInt, p.lat, p.lon, onError = { err ->
                        enCaminoSolicitado = false
                        if (err.contains("apartados en la base", ignoreCase = true) || err.contains("CARGA_PENDIENTE", ignoreCase = true)) {
                            errorCargaBase = err
                        } else {
                            mostrarAviso(err, largo = true)
                        }
                    })
                }
                repartidorViewModel.alMoverse(idInt, p)
            }
        }
    }
    DisposableEffect(Unit) { onDispose { repartidorViewModel.detenerNavegacion() } }

    val rutaNoDisponible by repartidorViewModel.rutaNoDisponible.collectAsState()
    val distanciaDestinoM = posicion?.let {
        UbicacionTracker.distanciaM(it.lat, it.lon, entrega.latDestino, entrega.lonDestino)
    }

    val idPedidoRuta = livePedidoEnRuta?.id
    var estado by remember { mutableStateOf(EstadoRuta.EN_RUTA) }
    var mostrarModalEntregado by remember { mutableStateOf(false) }
    var mostrarModalNoEntregado by remember { mutableStateOf(false) }

    // RN-012: la llegada se guarda en disco; al volver a la pantalla se retoma el conteo.
    LaunchedEffect(idPedidoRuta) {
        if (idPedidoRuta != null && LlegadaEntrega.obtener(idPedidoRuta) != null) {
            estado = EstadoRuta.LLEGADO
        }
    }
    var segundosRestantes by remember { mutableStateOf(10 * 60) }
    LaunchedEffect(estado, idPedidoRuta) {
        if (estado == EstadoRuta.LLEGADO) {
            val llegada = idPedidoRuta?.let { LlegadaEntrega.registrar(it) } ?: ahoraMs()
            do {
                val transcurridos = ((ahoraMs() - llegada) / 1000).toInt()
                segundosRestantes = (10 * 60 - transcurridos).coerceAtLeast(0)
                if (segundosRestantes > 0) delay(1000)
            } while (segundosRestantes > 0)
        }
    }
    val puedeMarcarNoEntregado = estado == EstadoRuta.LLEGADO && segundosRestantes <= 0

    Scaffold(containerColor = HToGoColors.Background) { padding ->
        BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
            val maxScreenHeight = maxHeight
            var isSheetMinimized by remember { mutableStateOf(false) }
            val sheetHeight by animateDpAsState(
                targetValue = if (isSheetMinimized) 84.dp else (maxScreenHeight * 0.70f).coerceAtMost(560.dp),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "sheetHeight"
            )

            // Mapa interactivo con la ruta del repartidor
            OsmRouteMapView(
                originLat = posicion?.lat,
                originLon = posicion?.lon,
                destLat = entrega.latDestino,
                destLon = entrega.lonDestino,
                routePoints = routePoints,
                seguirRepartidor = true,
                estadoPedido = livePedidoEnRuta?.estado,
                modifier = Modifier.fillMaxSize()
            )

            if (!tienePermiso) {
                PermisoUbicacionBanner(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    onActivar = {
                        pedirPermiso()
                    },
                    onAjustes = {
                        Plataforma.servicios.abrirAjustesApp()
                    }
                )
            }

            // Header superior flotante
            var menuOpciones by remember { mutableStateOf(false) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 10.dp, start = 12.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingIconButton(Icons.AutoMirrored.Filled.ArrowBack, onBack)
                Spacer(Modifier.width(8.dp))
                StepPill(
                    estado = estado,
                    pedidoId = entrega.pedidoId,
                    actual = entrega.entregaActual,
                    total = entrega.totalEntregas
                )
                Spacer(Modifier.width(8.dp))
                Box {
                    FloatingIconButton(Icons.Filled.MoreVert, { menuOpciones = true })
                    DropdownMenu(
                        expanded = menuOpciones,
                        onDismissRequest = { menuOpciones = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (posicionSimulada == null) "🧪 Simular GPS en Benito Juárez (a 30 m)"
                                    else "📍 Usar GPS real del teléfono"
                                )
                            },
                            onClick = {
                                menuOpciones = false
                                if (posicionSimulada == null) {
                                    // Simula estar a ~30 metros del domicilio para probar la llegada y entrega
                                    val latSim = entrega.latDestino + 0.00025
                                    val lonSim = entrega.lonDestino + 0.00015
                                    posicionSimulada = com.htogo.app.data.location.Posicion(latSim, lonSim, 5f)
                                    mostrarAviso("GPS simulado en Benito Juárez (a ~30 m del destino)")
                                } else {
                                    posicionSimulada = null
                                    mostrarAviso("Volviendo al GPS real")
                                }
                            }
                        )
                        if (posicionSimulada != null) {
                            DropdownMenuItem(
                                text = { Text("🚗 Simular en ruta (a 1.2 km)") },
                                onClick = {
                                    menuOpciones = false
                                    val latSim = entrega.latDestino + 0.010
                                    val lonSim = entrega.lonDestino - 0.005
                                    posicionSimulada = com.htogo.app.data.location.Posicion(latSim, lonSim, 5f)
                                    mostrarAviso("GPS simulado en ruta en Benito Juárez (a ~1.2 km)")
                                }
                            )
                        }
                    }
                }
            }

            // Indicador compacto si el GPS está en modo simulación de prueba
            if (posicionSimulada != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF7C3AED).copy(alpha = 0.92f),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(top = 60.dp, start = 12.dp)
                        .clickable { posicionSimulada = null }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🧪 Simulado ✕",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Tarjeta inferior con información del pedido (minimizable / deslizable)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(sheetHeight),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (isSheetMinimized) 0.dp else 12.dp)
                ) {
                    // Cabecera interactiva y arrastrable (Handle + Resumen cuando está minimizado + Chevron)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .draggable(
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { delta ->
                                    if (delta > 10f && !isSheetMinimized) {
                                        isSheetMinimized = true
                                    } else if (delta < -10f && isSheetMinimized) {
                                        isSheetMinimized = false
                                    }
                                }
                            )
                            .clickable { isSheetMinimized = !isSheetMinimized }
                            .padding(top = 10.dp, bottom = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier
                                .width(44.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(HToGoColors.OutlineSoft)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSheetMinimized) {
                                Surface(
                                    shape = RoundedCornerShape(99.dp),
                                    color = if (estado == EstadoRuta.LLEGADO) HToGoColors.StatusEntregado.copy(alpha = .15f)
                                    else HToGoColors.Primary.copy(alpha = .15f)
                                ) {
                                    Text(
                                        if (estado == EstadoRuta.LLEGADO) "Llegaste" else "En ruta",
                                        color = if (estado == EstadoRuta.LLEGADO) HToGoColors.StatusEntregado else HToGoColors.Primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Pedido #${entrega.pedidoId} · ${entrega.productos}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = HToGoColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    val distTexto = when {
                                        estado == EstadoRuta.LLEGADO -> "En domicilio · Espera: %02d:%02d".format(segundosRestantes / 60, segundosRestantes % 60)
                                        liveRutaCalculada?.takeIf { it.encontrada }?.distanciaTotalKm != null -> "%.2f km restantes".format(liveRutaCalculada?.distanciaTotalKm)
                                        distanciaDestinoM != null && distanciaDestinoM < 1000 -> "%.0f m al destino".format(distanciaDestinoM)
                                        distanciaDestinoM != null -> "%.2f km al destino".format(distanciaDestinoM / 1000)
                                        else -> entrega.direccion.substringBefore(",")
                                    }
                                    Text(
                                        distTexto,
                                        fontSize = 11.5.sp,
                                        color = HToGoColors.TextSecondary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                Text(
                                    "Desglose de entrega",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = HToGoColors.TextSecondary
                                )
                                Spacer(Modifier.weight(1f))
                            }
                            IconButton(
                                onClick = { isSheetMinimized = !isSheetMinimized },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    if (isSheetMinimized) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = if (isSheetMinimized) "Expandir desglose" else "Minimizar desglose",
                                    tint = HToGoColors.TextSecondary
                                )
                            }
                        }
                    }

                    if (!isSheetMinimized) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 18.dp)
                        ) {
                            if (errorCargaBase != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = HToGoColors.AccentAmber.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, HToGoColors.AccentAmber),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Warning, null, tint = HToGoColors.AccentAmber, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Carga pendiente en la base", fontWeight = FontWeight.Bold, color = HToGoColors.AccentAmber, fontSize = 13.sp)
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            errorCargaBase ?: "Hay garrafones apartados en la base para este pedido. Debes cargarlos al vehículo antes de iniciar la ruta (CU-011 / CU-019).",
                                            fontSize = 11.sp,
                                            color = HToGoColors.TextPrimary
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                val idInt = entrega.pedidoId.toIntOrNull() ?: return@Button
                                                repartidorViewModel.cargarApartadosBase(
                                                    id = idInt,
                                                    onSuccess = {
                                                        errorCargaBase = null
                                                        mostrarAviso("Garrafones cargados al vehículo")
                                                        posicion?.let { pos ->
                                                            repartidorViewModel.marcarEnCamino(idInt, pos.lat, pos.lon)
                                                        }
                                                    },
                                                    onError = { err ->
                                                        mostrarAviso(err, largo = true)
                                                    }
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.AccentAmber),
                                            modifier = Modifier.fillMaxWidth().height(36.dp),
                                            shape = RoundedCornerShape(18.dp)
                                        ) {
                                            Icon(Icons.Filled.Warehouse, null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Cargar garrafones al vehículo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }

                            if (estado == EstadoRuta.EN_RUTA) {
                                EnRutaSheet(
                                    entrega, distanciaDestinoM,
                                    rutaKm = liveRutaCalculada?.takeIf { it.encontrada }?.distanciaTotalKm,
                                    rutaNoDisponible = rutaNoDisponible
                                )
                                Spacer(Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        // RF-014: el backend solo acepta la entrega a ≤ 50 m del domicilio.
                                        when {
                                            distanciaDestinoM == null -> mostrarAviso("Esperando la señal del GPS…")
                                            distanciaDestinoM > RADIO_ENTREGA_M -> mostrarAviso("Estás a %.0f m del domicilio. Acércate a menos de %d m.".format(distanciaDestinoM, RADIO_ENTREGA_M), largo = true)
                                            else -> estado = EstadoRuta.LLEGADO
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(54.dp),
                                    shape = RoundedCornerShape(27.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary)
                                ) {
                                    Icon(Icons.Filled.Flag, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Llegué al destino", fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                LlegadoSheet(entrega, segundosRestantes)
                                Spacer(Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { mostrarModalNoEntregado = true },
                                        modifier = Modifier.weight(1f).height(54.dp),
                                        shape = RoundedCornerShape(27.dp),
                                        enabled = puedeMarcarNoEntregado,
                                        border = BorderStroke(
                                            1.5.dp,
                                            if (puedeMarcarNoEntregado) HToGoColors.AccentRose else HToGoColors.OutlineSoft
                                        )
                                    ) {
                                        Icon(
                                            if (puedeMarcarNoEntregado) Icons.Filled.Cancel else Icons.Filled.Lock,
                                            null,
                                            tint = if (puedeMarcarNoEntregado) HToGoColors.AccentRose else HToGoColors.TextTertiary
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "No entregado",
                                            color = if (puedeMarcarNoEntregado) HToGoColors.AccentRose else HToGoColors.TextTertiary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Button(
                                        onClick = { mostrarModalEntregado = true },
                                        modifier = Modifier.weight(1.5f).height(54.dp),
                                        shape = RoundedCornerShape(27.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = HToGoColors.StatusEntregado
                                        )
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Marcar entregado", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                        }
                    }
                }
            }
        }

        if (mostrarModalEntregado) {
            ModalEntregado(
                entrega = entrega,
                detalles = livePedidoEnRuta?.detalles ?: emptyList(),
                onDismiss = { mostrarModalEntregado = false },
                onConfirm = { lineasPayload ->
                    val idInt = entrega.pedidoId.toIntOrNull()
                    val p = posicion
                    if (idInt != null && p != null) {
                        repartidorViewModel.registrarResultadoEntrega(
                            id = idInt,
                            esEntregado = true,
                            lat = p.lat,
                            lon = p.lon,
                            lineas = lineasPayload,
                            onSuccess = {
                                EntregaEnCurso.detener()
                                LlegadaEntrega.limpiar(idInt)
                                mostrarAviso("¡Entrega completada con éxito!")
                                mostrarModalEntregado = false
                                onCompletada()
                            },
                            onError = { err ->
                                // Se queda en la pantalla para reintentar (p. ej. fuera del radio de 50 m).
                                mostrarAviso(err, largo = true)
                                mostrarModalEntregado = false
                            }
                        )
                    } else {
                        mostrarAviso("Esperando la señal del GPS…")
                        mostrarModalEntregado = false
                    }
                }
            )
        }
        if (mostrarModalNoEntregado) {
            ModalNoEntregado(
                onDismiss = { mostrarModalNoEntregado = false },
                onConfirm = { motivo, nota ->
                    val idInt = entrega.pedidoId.toIntOrNull()
                    val p = posicion
                    if (idInt != null && p != null) {
                        repartidorViewModel.registrarResultadoEntrega(
                            id = idInt,
                            esEntregado = false,
                            lat = p.lat,
                            lon = p.lon,
                            motivoNoEntrega = motivo.titulo + nota.trim().takeIf { it.isNotEmpty() }?.let { ": $it" }.orEmpty(),
                            onSuccess = {
                                EntregaEnCurso.detener()
                                LlegadaEntrega.limpiar(idInt)
                                mostrarAviso("Entrega reportada como no realizada")
                                mostrarModalNoEntregado = false
                                onCompletada()
                            },
                            onError = { err ->
                                mostrarAviso(err, largo = true)
                                mostrarModalNoEntregado = false
                            }
                        )
                    } else {
                        mostrarAviso("Esperando la señal del GPS…")
                        mostrarModalNoEntregado = false
                    }
                }
            )
        }
    }
}

@Composable
private fun FloatingIconButton(icon: ImageVector, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier.size(40.dp)
    ) {
        IconButton(onClick = onClick) { Icon(icon, null, tint = HToGoColors.TextPrimary) }
    }
}

@Composable
private fun StepPill(estado: EstadoRuta, pedidoId: String, actual: Int, total: Int) {
    val bgColor = if (estado == EstadoRuta.LLEGADO) HToGoColors.StatusEntregado else Color.White
    val textColor = if (estado == EstadoRuta.LLEGADO) Color.White else HToGoColors.TextPrimary
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = bgColor,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(0.7f)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (estado == EstadoRuta.LLEGADO) {
                Icon(
                    Icons.Filled.Flag, null,
                    tint = HToGoColors.StatusEntregado,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                        .padding(3.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Llegaste al destino",
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    "Entrega $actual de $total · #$pedidoId",
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EnRutaSheet(entrega: EntregaActiva, distanciaDestinoM: Double?, rutaKm: Double?, rutaNoDisponible: Boolean) {
    var menuNavegar by remember { mutableStateOf(false) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Pedido #${entrega.pedidoId}",
                    fontSize = 12.sp,
                    color = HToGoColors.TextSecondary
                )
                Text(
                    entrega.productos,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HToGoColors.TextPrimary
                )
            }
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = HToGoColors.Primary.copy(alpha = .12f)
            ) {
                Text(
                    "En ruta",
                    color = HToGoColors.Primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = HToGoColors.Primary.copy(alpha = .08f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Directions, null, tint = HToGoColors.Primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            rutaKm != null -> "%.2f km restantes".format(rutaKm)
                            distanciaDestinoM != null && distanciaDestinoM < 1000 -> "%.0f m en línea recta".format(distanciaDestinoM)
                            distanciaDestinoM != null -> "%.2f km en línea recta".format(distanciaDestinoM / 1000)
                            else -> "Buscando tu ubicación…"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = HToGoColors.Primary
                    )
                    Text(
                        entrega.direccion.split(" · ").first(),
                        fontSize = 11.sp,
                        color = HToGoColors.TextSecondary
                    )
                    if (rutaNoDisponible && rutaKm == null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = HToGoColors.AccentAmber.copy(alpha = 0.15f),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "⚠️ Estás fuera de Benito Juárez. Toca el menú (⋮) arriba para simular GPS o usa 'Navegar' para abrir tu app externa.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB45309),
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                Box {
                    Button(
                        onClick = { menuNavegar = true },
                        colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary)
                    ) {
                        Icon(Icons.Filled.Navigation, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Navegar", fontSize = 12.sp)
                    }
                    DropdownMenu(expanded = menuNavegar, onDismissRequest = { menuNavegar = false }) {
                        DropdownMenuItem(
                            text = { Text("Abrir en Google Maps") },
                            onClick = {
                                menuNavegar = false
                                abrirGoogleMaps(entrega)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Abrir en Waze") },
                            onClick = {
                                menuNavegar = false
                                abrirWaze(entrega)
                            }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        ClienteCard(entrega, HToGoColors.Primary)
        Spacer(Modifier.height(12.dp))
        PagoBox(entrega, soloTotal = true)
    }
}

@Composable
private fun LlegadoSheet(entrega: EntregaActiva, segundos: Int) {
    val mm = segundos / 60
    val ss = segundos % 60
    val pct = segundos / (10f * 60f)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Pedido #${entrega.pedidoId}",
                    fontSize = 12.sp,
                    color = HToGoColors.TextSecondary
                )
                Text(
                    "${entrega.productos} · ${entrega.cliente}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HToGoColors.TextPrimary
                )
            }
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = HToGoColors.StatusEntregado.copy(alpha = .12f)
            ) {
                Text(
                    "Llegaste",
                    color = HToGoColors.StatusEntregado,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFFEF3C7),
            border = BorderStroke(1.dp, Color(0xFFFCD34D)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(HToGoColors.AccentAmber),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Timer, null, tint = Color.White) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Tiempo de espera obligatorio",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF78350F)
                    )
                    Text(
                        "Podrás marcar como no entregado cuando termine",
                        fontSize = 11.sp,
                        color = Color(0xFF92400E)
                    )
                }
                Text(
                    "%02d:%02d".format(mm, ss),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = HToGoColors.AccentAmber
                )
            }
        }
        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 6.dp),
            color = HToGoColors.AccentAmber,
            trackColor = Color(0xFFFEF3C7)
        )

        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = HToGoColors.PrimaryWash,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Payments, null, tint = HToGoColors.Primary)
                Spacer(Modifier.width(10.dp))
                Text(
                    entrega.productos,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HToGoColors.TextPrimary
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        ClienteCard(entrega, HToGoColors.StatusEntregado)
    }
}

/** Radio de entrega del backend (RF-014, h2togo.pedidos.entrega-radio-m). */
private const val RADIO_ENTREGA_M = 50

/** Navegación paso a paso en Google Maps (o la app de mapas del sistema si no está instalado). */
private fun abrirGoogleMaps(entrega: EntregaActiva) =
    Plataforma.servicios.abrirNavegacion(AppNavegacion.GOOGLE_MAPS, entrega.latDestino, entrega.lonDestino, entrega.cliente)

/** Waze con navegación inmediata; si no está instalado, el enlace web. */
private fun abrirWaze(entrega: EntregaActiva) =
    Plataforma.servicios.abrirNavegacion(AppNavegacion.WAZE, entrega.latDestino, entrega.lonDestino, entrega.cliente)

@Composable
private fun PermisoUbicacionBanner(modifier: Modifier, onActivar: () -> Unit, onAjustes: () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.LocationOff, null, tint = HToGoColors.AccentRose, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text("Activa tu ubicación", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "La necesitamos para calcular tu ruta, avisar al cliente que vas en camino y validar la entrega.",
                fontSize = 12.sp,
                color = HToGoColors.TextSecondary
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onActivar,
                colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Permitir ubicación") }
            TextButton(onClick = onAjustes) { Text("Abrir ajustes", fontSize = 12.sp) }
        }
    }
}

@Composable
private fun ClienteCard(entrega: EntregaActiva, accent: Color) {
    val uriHandler = LocalUriHandler.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = HToGoColors.Background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(50)).background(accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    entrega.inicialesCliente,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    entrega.cliente,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HToGoColors.TextPrimary
                )
                Text(
                    entrega.direccion,
                    fontSize = 11.sp,
                    color = HToGoColors.TextSecondary,
                    maxLines = 1
                )
            }
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(50))
                    .background(HToGoColors.StatusEntregado)
                    .clickable {
                        if (!entrega.telefono.isNullOrBlank()) {
                            try {
                                uriHandler.openUri("tel:${entrega.telefono}")
                            } catch (e: Exception) {
                                mostrarAviso("No se pudo iniciar llamada: ${e.message}")
                            }
                        } else {
                            mostrarAviso("Teléfono de cliente no disponible")
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Call, null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun PagoBox(entrega: EntregaActiva, soloTotal: Boolean) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = HToGoColors.StatusEntregado.copy(alpha = .08f),
        border = BorderStroke(1.dp, HToGoColors.StatusEntregado.copy(alpha = .25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Payments, null, tint = HToGoColors.StatusEntregado)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "Cobrarás en efectivo",
                    fontSize = 11.sp,
                    color = HToGoColors.TextSecondary
                )
                Text(
                    "$%.0f MXN".format(entrega.total),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HToGoColors.StatusEntregado
                )
            }
        }
    }
}

private data class ItemLineaEntrega(
    val idDetalle: Int?,
    val idMarca: Int,
    val nombreMarca: String,
    val cantidadSolicitada: Int,
    val cantidadEntregada: Int,
    val tieneEnvase: Boolean,
    val envaseApto: Boolean = true,
    val cobrarEnvaseNuevo: Boolean = false,
    val precioUnitario: Double,
    val precioEnvaseUnitario: Double
)

@Composable
private fun ModalEntregado(
    entrega: EntregaActiva,
    detalles: List<DetalleResponse>,
    onDismiss: () -> Unit,
    onConfirm: (List<com.htogo.app.data.dto.LineaResultadoDto>) -> Unit
) {
    var lineasState by remember(detalles) {
        val list = if (detalles.isNotEmpty()) {
            detalles.map { d ->
                ItemLineaEntrega(
                    idDetalle = d.id,
                    idMarca = d.idMarca,
                    nombreMarca = d.nombreMarca ?: "Garrafón 20 L",
                    cantidadSolicitada = d.cantidad,
                    cantidadEntregada = d.cantidad,
                    tieneEnvase = d.tieneEnvase,
                    envaseApto = true,
                    cobrarEnvaseNuevo = false,
                    precioUnitario = d.precioUnitario ?: (d.subtotal?.div(d.cantidad) ?: 45.0),
                    precioEnvaseUnitario = d.precioEnvaseUnitario ?: 80.0
                )
            }
        } else {
            listOf(
                ItemLineaEntrega(
                    idDetalle = null,
                    idMarca = 1,
                    nombreMarca = "Garrafón 20 L",
                    cantidadSolicitada = entrega.garrafonesTotales.coerceAtLeast(1),
                    cantidadEntregada = entrega.garrafonesTotales.coerceAtLeast(1),
                    tieneEnvase = true,
                    envaseApto = true,
                    cobrarEnvaseNuevo = false,
                    precioUnitario = entrega.total / entrega.garrafonesTotales.coerceAtLeast(1),
                    precioEnvaseUnitario = 80.0
                )
            )
        }
        mutableStateOf(list)
    }

    val totalRecalculado = remember(lineasState) {
        lineasState.sumOf { item ->
            val pAgua = item.precioUnitario
            val pEnvase = if (!item.tieneEnvase || item.cobrarEnvaseNuevo) item.precioEnvaseUnitario else 0.0
            item.cantidadEntregada * (pAgua + pEnvase)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar entrega en sitio", fontWeight = FontWeight.SemiBold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "Revisa físicamente los envases recibidos y confirma las cantidades por línea (CU-012 / RN-032).",
                        fontSize = 12.sp,
                        color = HToGoColors.TextSecondary
                    )
                }

                items(lineasState.indices.toList()) { index ->
                    val linea = lineasState[index]
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        linea.nombreMarca,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = HToGoColors.TextPrimary
                                    )
                                    Text(
                                        "Solicitados: ${linea.cantidadSolicitada} · $%.0f c/u".format(linea.precioUnitario),
                                        fontSize = 11.sp,
                                        color = HToGoColors.TextSecondary
                                    )
                                }

                                // Controles de cantidad entregada (Entrega parcial / S4)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (linea.cantidadEntregada > 0) {
                                                val updated = lineasState.toMutableList()
                                                updated[index] = linea.copy(cantidadEntregada = linea.cantidadEntregada - 1)
                                                lineasState = updated
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Filled.Remove, null, tint = HToGoColors.Primary)
                                    }
                                    Text(
                                        "${linea.cantidadEntregada}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                    IconButton(
                                        onClick = {
                                            if (linea.cantidadEntregada < linea.cantidadSolicitada) {
                                                val updated = lineasState.toMutableList()
                                                updated[index] = linea.copy(cantidadEntregada = linea.cantidadEntregada + 1)
                                                lineasState = updated
                                            }
                                        },
                                        modifier = Modifier.size(28.dp),
                                        enabled = linea.cantidadEntregada < linea.cantidadSolicitada
                                    ) {
                                        Icon(
                                            Icons.Filled.Add, null,
                                            tint = if (linea.cantidadEntregada < linea.cantidadSolicitada) HToGoColors.Primary else HToGoColors.TextTertiary
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(6.dp))

                            // Validación física del envase del cliente (CU-012 Paso 5 y Flujo S3)
                            if (linea.tieneEnvase) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (linea.envaseApto) HToGoColors.StatusEntregado.copy(alpha = 0.08f) else HToGoColors.AccentRose.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, if (linea.envaseApto) HToGoColors.StatusEntregado.copy(alpha = 0.3f) else HToGoColors.AccentRose.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                if (linea.envaseApto) "✓ Envase del cliente apto" else "⚠ Envase dañado / no apto",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (linea.envaseApto) HToGoColors.StatusEntregado else HToGoColors.AccentRose
                                            )
                                            TextButton(
                                                onClick = {
                                                    val updated = lineasState.toMutableList()
                                                    val nuevoApto = !linea.envaseApto
                                                    updated[index] = linea.copy(
                                                        envaseApto = nuevoApto,
                                                        cobrarEnvaseNuevo = if (nuevoApto) false else linea.cobrarEnvaseNuevo
                                                    )
                                                    lineasState = updated
                                                },
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text(if (linea.envaseApto) "Marcar dañado" else "Marcar apto", fontSize = 11.sp)
                                            }
                                        }

                                        // Flujo S3: opciones si el envase no es apto
                                        if (!linea.envaseApto) {
                                            Text(
                                                "El envase no está en condiciones. ¿Desea el cliente pagar un envase nuevo?",
                                                fontSize = 11.sp,
                                                color = HToGoColors.TextSecondary
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                Checkbox(
                                                    checked = linea.cobrarEnvaseNuevo,
                                                    onCheckedChange = { chk ->
                                                        val updated = lineasState.toMutableList()
                                                        updated[index] = linea.copy(cobrarEnvaseNuevo = chk)
                                                        lineasState = updated
                                                    }
                                                )
                                                Text(
                                                    "Cobrar envase nuevo (+ $%.0f MXN)".format(linea.precioEnvaseUnitario),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = HToGoColors.AccentAmber.copy(alpha = 0.08f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Pedido sin envase retornado (incluye envase nuevo: +$%.0f MXN)".format(linea.precioEnvaseUnitario),
                                        fontSize = 11.sp,
                                        color = HToGoColors.AccentAmber,
                                        modifier = Modifier.padding(6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    // Resumen y Total a cobrar
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = HToGoColors.StatusEntregado.copy(alpha = .08f),
                        border = BorderStroke(1.dp, HToGoColors.StatusEntregado.copy(alpha = .25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Payments, null, tint = HToGoColors.StatusEntregado)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Total definitivo a cobrar",
                                    fontSize = 11.sp,
                                    color = HToGoColors.TextSecondary
                                )
                                Text(
                                    "$%.2f MXN".format(totalRecalculado),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HToGoColors.StatusEntregado
                                )
                                Text(
                                    "Efectivo / Transferencia / Tarjeta",
                                    fontSize = 10.sp,
                                    color = HToGoColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lineasPayload = mutableListOf<com.htogo.app.data.dto.LineaResultadoDto>()
                    for (l in lineasState) {
                        if (l.idDetalle != null) {
                            lineasPayload.add(
                                com.htogo.app.data.dto.LineaResultadoDto(
                                    idDetalle = l.idDetalle,
                                    cantidadEntregada = l.cantidadEntregada,
                                    agregadaEnSitio = false
                                )
                            )
                        } else {
                            lineasPayload.add(
                                com.htogo.app.data.dto.LineaResultadoDto(
                                    idMarca = l.idMarca,
                                    cantidadEntregada = l.cantidadEntregada,
                                    agregadaEnSitio = false
                                )
                            )
                        }
                        if (l.cobrarEnvaseNuevo && l.cantidadEntregada > 0) {
                            lineasPayload.add(
                                com.htogo.app.data.dto.LineaResultadoDto(
                                    idMarca = l.idMarca,
                                    cantidadEntregada = l.cantidadEntregada,
                                    agregadaEnSitio = true
                                )
                            )
                        }
                    }
                    onConfirm(lineasPayload)
                },
                colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.StatusEntregado)
            ) {
                Icon(Icons.Filled.CheckCircle, null)
                Spacer(Modifier.width(4.dp))
                Text("Confirmar entrega ($%.0f)".format(totalRecalculado))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun ModalNoEntregado(
    onDismiss: () -> Unit,
    onConfirm: (MotivoNoEntrega, String) -> Unit
) {
    var seleccionado by remember { mutableStateOf<MotivoNoEntrega?>(MotivoNoEntrega.CLIENTE_NO_ENCONTRADO) }
    var nota by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Por qué no se entregó?", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                MotivoNoEntrega.values().forEach { m ->
                    val sel = seleccionado == m
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (sel) HToGoColors.AccentRose.copy(alpha = .04f) else Color.White,
                        border = BorderStroke(
                            1.5.dp,
                            if (sel) HToGoColors.AccentRose else HToGoColors.OutlineSoft
                        ),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(role = Role.RadioButton) { seleccionado = m }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    m.titulo,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = HToGoColors.TextPrimary
                                )
                                Text(
                                    m.subtitulo,
                                    fontSize = 11.sp,
                                    color = HToGoColors.TextSecondary
                                )
                            }
                            RadioButton(
                                selected = sel,
                                onClick = { seleccionado = m },
                                colors = RadioButtonDefaults.colors(selectedColor = HToGoColors.AccentRose)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = { Text("Notas adicionales (opcional)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { seleccionado?.let { onConfirm(it, nota) } },
                colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.AccentRose)
            ) {
                Icon(Icons.Filled.Cancel, null)
                Spacer(Modifier.width(4.dp))
                Text("Reportar no entrega")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

