package com.htogo.app.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.htogo.app.data.location.EntregaEnCursoService
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
    repartidorViewModel: RepartidorViewModel = viewModel()
) {
    val livePedidoEnRuta by repartidorViewModel.pedidoEnRuta.collectAsState()
    val livePedidoDisponible by repartidorViewModel.pedidoDisponibleSeleccionado.collectAsState()
    val liveRutaCalculada by repartidorViewModel.rutaCalculada.collectAsState()
    val isLoading by repartidorViewModel.isLoading.collectAsState()
    val context = LocalContext.current

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

        EntregaActiva(
            pedidoId = idStr,
            cliente = nombre,
            inicialesCliente = iniciales,
            telefono = tel,
            direccion = dir,
            notas = notasTxt,
            productos = prods,
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

    // ---- GPS real del repartidor ----
    val posicion by EntregaEnCursoService.posicion.collectAsState()
    var tienePermiso by remember { mutableStateOf(UbicacionTracker.tienePermiso(context)) }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        tienePermiso = r[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }
    LaunchedEffect(Unit) {
        if (!tienePermiso) {
            pedirPermiso.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
    var enCaminoSolicitado by remember(entrega.pedidoId) { mutableStateOf(false) }
    // El GPS vive en un servicio en primer plano: sigue reportando aunque se abra Waze/Maps.
    LaunchedEffect(tienePermiso, entrega.pedidoId) {
        val idInt = entrega.pedidoId.toIntOrNull() ?: return@LaunchedEffect
        if (!tienePermiso) return@LaunchedEffect
        EntregaEnCursoService.iniciar(context, idInt)
        EntregaEnCursoService.posicion.filterNotNull().collect { p ->
            // Con el primer fix el pedido pasa a "en camino": el cliente empieza a ver el rastreo.
            if (!enCaminoSolicitado && livePedidoEnRuta?.estado.equals("asignado", ignoreCase = true)) {
                enCaminoSolicitado = true
                repartidorViewModel.marcarEnCamino(idInt, p.lat, p.lon, onError = { err ->
                    enCaminoSolicitado = false
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                })
            }
            repartidorViewModel.alMoverse(idInt, p)
        }
    }
    DisposableEffect(Unit) { onDispose { repartidorViewModel.detenerNavegacion() } }

    val rutaNoDisponible by repartidorViewModel.rutaNoDisponible.collectAsState()
    val distanciaDestinoM = posicion?.let {
        UbicacionTracker.distanciaM(it.lat, it.lon, entrega.latDestino, entrega.lonDestino)
    }

    var estado by remember { mutableStateOf(EstadoRuta.EN_RUTA) }
    var mostrarModalEntregado by remember { mutableStateOf(false) }
    var mostrarModalNoEntregado by remember { mutableStateOf(false) }

    var segundosRestantes by remember { mutableStateOf(10 * 60) }
    LaunchedEffect(estado) {
        if (estado == EstadoRuta.LLEGADO) {
            segundosRestantes = 10 * 60
            while (segundosRestantes > 0) {
                delay(1000)
                segundosRestantes--
            }
        }
    }
    val puedeMarcarNoEntregado = estado == EstadoRuta.LLEGADO && segundosRestantes <= 0

    Scaffold(containerColor = HToGoColors.Background) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            // Mapa interactivo con la ruta del repartidor
            OsmRouteMapView(
                originLat = posicion?.lat,
                originLon = posicion?.lon,
                destLat = entrega.latDestino,
                destLon = entrega.lonDestino,
                routePoints = routePoints,
                modifier = Modifier.fillMaxSize()
            )

            if (!tienePermiso) {
                PermisoUbicacionBanner(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    onActivar = {
                        pedirPermiso.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    },
                    onAjustes = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        )
                    }
                )
            }

            // Header superior flotante
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 44.dp, start = 12.dp, end = 12.dp),
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
                FloatingIconButton(Icons.Filled.MoreVert, {})
            }

            // Tarjeta inferior con información del pedido
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .heightIn(max = 560.dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(18.dp)) {
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
                                    distanciaDestinoM == null -> Toast.makeText(
                                        context, "Esperando la señal del GPS…", Toast.LENGTH_SHORT
                                    ).show()
                                    distanciaDestinoM > RADIO_ENTREGA_M -> Toast.makeText(
                                        context,
                                        "Estás a %.0f m del domicilio. Acércate a menos de %d m.".format(distanciaDestinoM, RADIO_ENTREGA_M),
                                        Toast.LENGTH_LONG
                                    ).show()
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
                }
            }
        }

        if (mostrarModalEntregado) {
            ModalEntregado(
                entrega = entrega,
                onDismiss = { mostrarModalEntregado = false },
                onConfirm = {
                    val idInt = entrega.pedidoId.toIntOrNull()
                    val p = posicion
                    if (idInt != null && p != null) {
                        repartidorViewModel.registrarResultadoEntrega(
                            id = idInt,
                            esEntregado = true,
                            lat = p.lat,
                            lon = p.lon,
                            onSuccess = {
                                EntregaEnCursoService.detener(context)
                                Toast.makeText(context, "¡Entrega completada con éxito!", Toast.LENGTH_SHORT).show()
                                mostrarModalEntregado = false
                                onCompletada()
                            },
                            onError = { err ->
                                // Se queda en la pantalla para reintentar (p. ej. fuera del radio de 50 m).
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                mostrarModalEntregado = false
                            }
                        )
                    } else {
                        Toast.makeText(context, "Esperando la señal del GPS…", Toast.LENGTH_SHORT).show()
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
                                EntregaEnCursoService.detener(context)
                                Toast.makeText(context, "Entrega reportada como no realizada", Toast.LENGTH_SHORT).show()
                                mostrarModalNoEntregado = false
                                onCompletada()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                mostrarModalNoEntregado = false
                            }
                        )
                    } else {
                        Toast.makeText(context, "Esperando la señal del GPS…", Toast.LENGTH_SHORT).show()
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
    val context = LocalContext.current
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
                        Text(
                            "Sin ruta calculada: estás fuera de la zona con mapa de calles (Benito Juárez). Usa Navegar.",
                            fontSize = 11.sp,
                            color = HToGoColors.AccentAmber,
                            lineHeight = 14.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
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
                                abrirGoogleMaps(context, entrega)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Abrir en Waze") },
                            onClick = {
                                menuNavegar = false
                                abrirWaze(context, entrega)
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

/** Navegación paso a paso en Google Maps; si no está instalado, cualquier app de mapas (geo:). */
private fun abrirGoogleMaps(context: Context, entrega: EntregaActiva) {
    val destino = "${entrega.latDestino},${entrega.lonDestino}"
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$destino"))
                .setPackage("com.google.android.apps.maps")
        )
    } catch (_: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("geo:$destino?q=$destino(${Uri.encode(entrega.cliente)})"))
        )
    }
}

/** Waze con navegación inmediata; si no está instalado, el enlace web abre la tienda o el navegador. */
private fun abrirWaze(context: Context, entrega: EntregaActiva) {
    val destino = "${entrega.latDestino},${entrega.lonDestino}"
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("waze://?ll=$destino&navigate=yes")))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://waze.com/ul?ll=$destino&navigate=yes")))
    }
}

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
    val context = LocalContext.current
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
                                val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${entrega.telefono}"))
                                context.startActivity(callIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No se pudo iniciar llamada: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Teléfono de cliente no disponible", Toast.LENGTH_SHORT).show()
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

@Composable
private fun ModalEntregado(
    entrega: EntregaActiva,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar entrega", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Text(
                    "Verifica el cobro antes de confirmar.",
                    fontSize = 13.sp,
                    color = HToGoColors.TextSecondary
                )
                Spacer(Modifier.height(12.dp))
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
                                "Total cobrado en efectivo",
                                fontSize = 11.sp,
                                color = HToGoColors.TextSecondary
                            )
                            Text(
                                "$%.0f MXN".format(entrega.total),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = HToGoColors.StatusEntregado
                            )
                            Text(
                                "${entrega.productos} · ${entrega.cliente}",
                                fontSize = 11.sp,
                                color = HToGoColors.TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.StatusEntregado)
            ) {
                Icon(Icons.Filled.CheckCircle, null)
                Spacer(Modifier.width(4.dp))
                Text("Confirmar entrega")
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

@Preview(showBackground = true, widthDp = 412, heightDp = 868)
@Composable
fun RutaEntregaScreenPreview() {
    HToGoTheme { RutaEntregaScreen() }
}
