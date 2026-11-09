package com.htogo.app.ui.screens

import com.htogo.app.ui.components.DeslizarParaActualizar
import com.htogo.app.ui.components.BarraEstadoClara
import com.htogo.app.plataforma.mostrarAviso
import com.htogo.app.util.format

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.htogo.app.ui.ClienteViewModel
import com.htogo.app.ui.components.ClienteBottomBar
import com.htogo.app.ui.components.ClienteTab
import com.htogo.app.ui.components.EstadoPedido
import com.htogo.app.ui.components.EstadoPedidoChip
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme

@Composable
fun HomeClienteScreen(
    onNuevoPedido: () -> Unit = {},
    onElegirPurificadora: () -> Unit = {},
    onPedirAbierto: () -> Unit = {},
    onTrackPedido: () -> Unit = {},
    onHistorial: () -> Unit = {},
    onPerfil: () -> Unit = {},
    onAbrirPurificadora: () -> Unit = {},
    onSwitchRol: () -> Unit = {},
    clienteViewModel: ClienteViewModel = viewModel { ClienteViewModel() }
) {
    val sessionManager = remember { com.htogo.app.data.local.SessionManager.getInstance() }
    val nombre = remember { sessionManager.obtenerNombre()?.substringBefore(" ")?.ifBlank { "Cliente" } ?: "Cliente" }
    var notificacionesActivas by remember { mutableStateOf(sessionManager.estanNotificacionesActivas()) }

    val direccionesCliente by clienteViewModel.direcciones.collectAsState()
    val defaultDir = remember(direccionesCliente) { direccionesCliente.firstOrNull() }
    val direccion = remember(defaultDir) {
        defaultDir?.let {
            if (!it.colonia.isNullOrBlank()) "${it.alias}: Col. ${it.colonia}"
            else "${it.alias}: ${it.calle}"
        } ?: "Benito Juárez, CDMX"
    }

    LaunchedEffect(Unit) {
        clienteViewModel.cargarDirecciones()
        clienteViewModel.cargarHistorial()
    }

    val livePurificadoras by clienteViewModel.purificadoras.collectAsState()
    val livePedidoActivo by clienteViewModel.pedidoActivo.collectAsState()
    val ubicacionRepartidor by clienteViewModel.ubicacionRepartidor.collectAsState()
    val rutaSeguimiento by clienteViewModel.rutaSeguimiento.collectAsState()
    val distanciaRutaKm by clienteViewModel.distanciaRutaKm.collectAsState()
    val repartidorLlego by clienteViewModel.repartidorLlego.collectAsState()

    val estadosCerradosHome = remember { setOf("entregado", "no_entregado", "cancelado") }
    val pedidoEnCurso = livePedidoActivo?.takeIf { it.estado.lowercase() !in estadosCerradosHome }

    DisposableEffect(pedidoEnCurso?.id) {
        val idActivo = pedidoEnCurso?.id
        if (idActivo != null) {
            clienteViewModel.vincularRastreo("home", idActivo)
        }
        onDispose {
            clienteViewModel.desvincularRastreo("home")
        }
    }

    BarraEstadoClara() // el encabezado oscuro llega hasta arriba
    val refrescando by clienteViewModel.refrescando.collectAsState()
    Scaffold(
        // Sin margen superior: el encabezado pinta su degradado detrás de la barra de estado.
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        containerColor = HToGoColors.Background,
        bottomBar = {
            ClienteBottomBar(
                selected = ClienteTab.INICIO,
                onInicio = {},
                onPedidos = onHistorial,
                onPerfil = onPerfil
            )
        },
        floatingActionButton = {
            var menuPedir by remember { mutableStateOf(false) }
            Box {
                ExtendedFloatingActionButton(
                    onClick = { menuPedir = true },
                    containerColor = HToGoColors.Primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(28.dp),
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text("Pedir agua", fontWeight = FontWeight.SemiBold) }
                )
                DropdownMenu(
                    expanded = menuPedir,
                    onDismissRequest = { menuPedir = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    listOf(
                        Triple(Icons.Filled.Storefront, "Directo a una purificadora", onElegirPurificadora),
                        Triple(Icons.Filled.AttachMoney, "Con precio máximo", onPedirAbierto),
                        Triple(Icons.Filled.History, "Repetir un pedido", onHistorial),
                    ).forEach { (icono, texto, accion) ->
                        DropdownMenuItem(
                            text = { Text(texto, fontSize = 15.sp) },
                            leadingIcon = { Icon(icono, null, tint = HToGoColors.Primary) },
                            onClick = { menuPedir = false; accion() }
                        )
                    }
                }
            }
        }
    ) { padding ->
        // Deslizar hacia abajo para actualizar los datos de la pantalla.
        DeslizarParaActualizar(refrescando, { clienteViewModel.refrescar() }, Modifier.fillMaxSize(), bajoBarraDeEstado = true) {
            Column(
                Modifier.fillMaxSize().padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(HToGoColors.PrimaryDark, HToGoColors.Primary)
                            )
                        )
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Hola, $nombre",
                                    color = Color.White, fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.LocationOn, null,
                                        tint = HToGoColors.PrimarySoft,
                                        modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(direccion,
                                        color = HToGoColors.PrimarySoft, fontSize = 13.sp,
                                        maxLines = 1)
                                }
                            }
                            IconButton(onClick = {
                                val nuevoEstado = !notificacionesActivas
                                notificacionesActivas = nuevoEstado
                                sessionManager.guardarNotificacionesActivas(nuevoEstado)
                                mostrarAviso(if (nuevoEstado) "Notificaciones activadas" else "Notificaciones desactivadas")
                            }) {
                                Icon(
                                    if (notificacionesActivas) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                                    contentDescription = if (notificacionesActivas) "Desactivar notificaciones" else "Activar notificaciones",
                                    tint = if (notificacionesActivas) Color.White else Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Card(
                            onClick = onElegirPurificadora,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Search, null, tint = HToGoColors.TextTertiary)
                                Spacer(Modifier.width(10.dp))
                                Text("¿Buscas una purificadora cerca?",
                                    color = HToGoColors.TextSecondary, fontSize = 14.sp)
                            }
                        }
                    }
                }

                if (pedidoEnCurso != null) {
                    val estadoNorm = pedidoEnCurso.estado.lowercase()
                    val cant = pedidoEnCurso.garrafonesTotales ?: 1
                    val garrafonesTxt = if (cant == 1) "1 garrafón" else "$cant garrafones"
                    val negocioTxt = (pedidoEnCurso.repartidor?.negocio ?: pedidoEnCurso.nombreNegocio)
                        ?.takeIf { it.isNotBlank() }
                    val resumenCard = if (negocioTxt != null) "$garrafonesTxt · $negocioTxt" else garrafonesTxt

                    val kmRestantes = remember(
                        pedidoEnCurso,
                        ubicacionRepartidor,
                        rutaSeguimiento,
                        distanciaRutaKm,
                        livePurificadoras,
                        defaultDir
                    ) {
                        if (estadoNorm !in setOf("asignado", "en_camino")) {
                            null
                        } else {
                            val latDest = pedidoEnCurso.latEntrega ?: defaultDir?.lat
                            val lonDest = pedidoEnCurso.lonEntrega ?: defaultDir?.lon
                            val posOrigen = ubicacionRepartidor?.let { it.lat to it.lon }
                                ?: rutaSeguimiento.firstOrNull()
                                ?: livePurificadoras.firstOrNull { p ->
                                    (pedidoEnCurso.idNegocioSolicitado != null && p.id == pedidoEnCurso.idNegocioSolicitado) ||
                                        (negocioTxt != null && p.nombreComercial.equals(negocioTxt, ignoreCase = true))
                                }?.let { p ->
                                    if (p.lat != null && p.lon != null && (p.lat != 0.0 || p.lon != 0.0)) p.lat to p.lon else null
                                }
                            when {
                                posOrigen != null && latDest != null && lonDest != null && rutaSeguimiento.size >= 2 ->
                                    com.htogo.app.data.location.distanciaRestanteRutaKm(
                                        lat = posOrigen.first,
                                        lon = posOrigen.second,
                                        ruta = rutaSeguimiento,
                                        destLat = latDest,
                                        destLon = lonDest,
                                        distanciaTotalOriginalKm = distanciaRutaKm
                                    )
                                distanciaRutaKm != null && distanciaRutaKm!! > 0.0 -> distanciaRutaKm
                                posOrigen != null && latDest != null && lonDest != null ->
                                    (com.htogo.app.data.location.distanciaM(
                                        posOrigen.first, posOrigen.second, latDest, lonDest
                                    ) * 1.25) / 1000.0
                                else -> null
                            }
                        }
                    }

                    val llegoAlDomicilio = estadoNorm == "en_camino" &&
                        (repartidorLlego || (kmRestantes != null && kmRestantes <= 0.030))
                    val pasosCompletados = when (estadoNorm) {
                        "pendiente", "pendiente_programado" -> 1
                        "asignado" -> 2
                        "en_camino" -> if (llegoAlDomicilio) 4 else 3
                        else -> 1
                    }
                    val textoTiempo = when (estadoNorm) {
                        "pendiente_programado" -> {
                            val fp = pedidoEnCurso.fechaProgramada
                            if (!fp.isNullOrBlank() && fp.length >= 16) {
                                "Programado · ${fp.substring(8, 10)}/${fp.substring(5, 7)} ${fp.substring(11, 16)}"
                            } else {
                                "Pedido programado · esperando horario"
                            }
                        }
                        "pendiente" -> {
                            val fp = pedidoEnCurso.fechaProgramada
                            if (pedidoEnCurso.esProgramado && !fp.isNullOrBlank() && fp.length >= 16) {
                                "Programado · ${fp.substring(8, 10)}/${fp.substring(5, 7)} ${fp.substring(11, 16)}"
                            } else {
                                "Esperando que acepten tu pedido…"
                            }
                        }
                        "asignado" -> {
                            if (kmRestantes != null) {
                                val tiempoPrep = com.htogo.app.data.location.formatearDuracionRuta(kmRestantes, minutosExtra = 2)
                                "Repartidor asignado · llega en ~$tiempoPrep (${com.htogo.app.data.location.formatearDistanciaCorta(kmRestantes)})"
                            } else {
                                "Repartidor asignado · preparando tu pedido"
                            }
                        }
                        "en_camino" -> when {
                            llegoAlDomicilio -> "¡Tu repartidor está afuera de tu domicilio!"
                            kmRestantes != null && kmRestantes <= 0.08 ->
                                "Llegando a tu domicilio (~${com.htogo.app.data.location.formatearDuracionRuta(kmRestantes)} · ${com.htogo.app.data.location.formatearDistanciaCorta(kmRestantes)})"
                            kmRestantes != null ->
                                "Llega en ~${com.htogo.app.data.location.formatearDuracionRuta(kmRestantes)} (${com.htogo.app.data.location.formatearDistanciaCorta(kmRestantes)})"
                            else -> "En camino a tu domicilio"
                        }
                        else -> "Actualizando estado…"
                    }

                    Spacer(Modifier.height(20.dp))
                    Text("Pedido en curso",
                        style = MaterialTheme.typography.titleMedium,
                        color = HToGoColors.TextPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(Modifier.height(8.dp))
                    PedidoActivoCard(
                        id = pedidoEnCurso.id.toString(),
                        resumen = resumenCard,
                        estadoStr = pedidoEnCurso.estado,
                        pasosCompletados = pasosCompletados,
                        textoTiempo = textoTiempo,
                        destacarTiempo = llegoAlDomicilio,
                        onClick = onTrackPedido
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text("¿Cómo quieres pedir?",
                    style = MaterialTheme.typography.titleMedium,
                    color = HToGoColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OpcionPedido(Icons.Filled.Storefront, "Directo a purificadora", "Tú eliges cuál", Modifier.weight(1f), onElegirPurificadora)
                    OpcionPedido(Icons.Filled.AttachMoney, "Precio máximo", "Tú pones el tope", Modifier.weight(1f), onPedirAbierto)
                    OpcionPedido(Icons.Filled.History, "Repetir pedido", "De tu historial", Modifier.weight(1f), onHistorial)
                }

                Spacer(Modifier.height(24.dp))

                Text("Purificadoras cerca de ti",
                    style = MaterialTheme.typography.titleMedium,
                    color = HToGoColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(8.dp))
                if (livePurificadoras.isNotEmpty()) {
                    livePurificadoras.forEach { p ->
                        val pLat = p.lat
                        val pLon = p.lon
                        val distKm = when {
                            p.distanciaKm > 0.0 -> p.distanciaKm
                            defaultDir != null && pLat != null && pLon != null && (pLat != 0.0 || pLon != 0.0) ->
                                com.htogo.app.data.location.UbicacionTracker.distanciaM(defaultDir.lat, defaultDir.lon, pLat, pLon) / 1000.0
                            else -> p.distanciaKm
                        }
                        val distanciaStr = com.htogo.app.data.location.formatearDistanciaCorta(distKm)
                        PurificadoraMini(
                            nombre = p.nombreComercial,
                            distancia = distanciaStr,
                            onAbrir = {
                                clienteViewModel.purificadoraSeleccionadaId = p.id
                                clienteViewModel.purificadoraSeleccionadaNombre = p.nombreComercial
                                clienteViewModel.purificadoraSeleccionadaDistancia = distanciaStr
                                clienteViewModel.cargarPerfilPurificadora(p.id)
                                onAbrirPurificadora()
                            },
                            onPedir = {
                                clienteViewModel.purificadoraSeleccionadaId = p.id
                                clienteViewModel.purificadoraSeleccionadaNombre = p.nombreComercial
                                clienteViewModel.purificadoraSeleccionadaDistancia = distanciaStr
                                clienteViewModel.cargarPerfilPurificadora(p.id)
                                onNuevoPedido()
                            }
                        )
                    }
                } else {
                    Text(
                        "No encontramos purificadoras con cobertura cerca de tu domicilio.",
                        fontSize = 13.sp, color = HToGoColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                Spacer(Modifier.height(96.dp))
            }
        }
    }
}

@Composable
private fun PedidoActivoCard(
    id: String = "HG-1287",
    resumen: String = "3 garrafones · Aguas Del Valle",
    estadoStr: String = "en_camino",
    pasosCompletados: Int = 3,
    textoTiempo: String = "En camino a tu domicilio",
    destacarTiempo: Boolean = false,
    onClick: () -> Unit
) {
    val estadoEnum = when (estadoStr.lowercase()) {
        "pendiente", "pendiente_programado" -> EstadoPedido.PENDIENTE
        "asignado" -> EstadoPedido.ASIGNADO
        "en_camino" -> EstadoPedido.EN_CAMINO
        "entregado" -> EstadoPedido.ENTREGADO
        "cancelado" -> EstadoPedido.CANCELADO
        "no_entregado" -> EstadoPedido.NO_ENTREGADO
        else -> EstadoPedido.PENDIENTE
    }
    val colorTiempo = if (destacarTiempo) HToGoColors.StatusEntregado else HToGoColors.TextSecondary

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(HToGoColors.PrimarySoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.WaterDrop, null, tint = HToGoColors.Primary)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Pedido #$id",
                        style = MaterialTheme.typography.titleMedium,
                        color = HToGoColors.TextPrimary)
                    Text(resumen,
                        style = MaterialTheme.typography.bodyMedium,
                        color = HToGoColors.TextSecondary)
                }
                EstadoPedidoChip(estadoEnum)
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                (0 until 4).forEach { i ->
                    val done = i < pasosCompletados.coerceIn(1, 4)
                    Box(
                        Modifier
                            .height(6.dp).weight(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (done) HToGoColors.Primary else HToGoColors.OutlineSoft)
                    )
                    if (i < 3) Spacer(Modifier.width(4.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Schedule, null,
                    tint = colorTiempo, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    textoTiempo,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (destacarTiempo) FontWeight.SemiBold else FontWeight.Normal,
                    color = colorTiempo
                )
            }
        }
    }
}

@Composable
private fun OpcionPedido(
    icon: ImageVector, title: String, subtitle: String,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(HToGoColors.PrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = HToGoColors.Primary, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = HToGoColors.TextPrimary,
                    lineHeight = 16.sp, maxLines = 2)
                Text(subtitle, fontSize = 11.sp, color = HToGoColors.TextSecondary, lineHeight = 13.sp, maxLines = 2)
            }
        }
    }
}

@Composable
private fun PurificadoraMini(
    nombre: String,
    distancia: String,
    onAbrir: () -> Unit,
    onPedir: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onAbrir),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(HToGoColors.PrimarySoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Storefront, null,
                        tint = HToGoColors.Primary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(nombre, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = HToGoColors.TextPrimary)
                    Text(distancia,
                        fontSize = 12.sp, color = HToGoColors.TextSecondary)
                }
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(HToGoColors.PrimarySoft)
                    .clickable(onClick = onPedir)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Pedir", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = HToGoColors.Primary)
            }
        }
    }
}
