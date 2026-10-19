package com.htogo.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.htogo.app.data.dto.AgendaPedidoDto
import com.htogo.app.data.dto.ParadaDto
import com.htogo.app.data.dto.RutaOptimaDto
import com.htogo.app.data.notificaciones.CanalNotificaciones
import com.htogo.app.plataforma.mostrarAviso
import com.htogo.app.ui.RepartidorViewModel
import com.htogo.app.ui.components.BarraEstadoClara
import com.htogo.app.ui.components.OsmRouteMapView
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.util.conDecimales
import com.htogo.app.util.fechaCdmx
import com.htogo.app.util.horaAmPm

/**
 * Ruta del día del repartidor: el orden óptimo de sus paradas (OR-Tools en el backend), el mapa
 * con las paradas numeradas y la agenda de pedidos programados de hoy para reservar.
 */
@Composable
fun PlanRutaScreen(
    onBack: () -> Unit = {},
    onIrAParada: () -> Unit = {},
    repartidorViewModel: RepartidorViewModel = viewModel { RepartidorViewModel() }
) {
    val ruta by repartidorViewModel.rutaOptima.collectAsState()
    val agenda by repartidorViewModel.agenda.collectAsState()
    val calculando by repartidorViewModel.calculandoPlan.collectAsState()
    val error by repartidorViewModel.errorPlan.collectAsState()
    var abriendo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { repartidorViewModel.cargarPlan() }
    // RN-009: si un cliente cancela un pedido de la ruta (o se activa un programado), se recalcula.
    LaunchedEffect(Unit) {
        CanalNotificaciones.avisos.collect { aviso ->
            if (aviso.datos["tipo"] == "ruta_recalculada") repartidorViewModel.cargarPlan()
        }
    }

    BarraEstadoClara()
    Scaffold(
        containerColor = HToGoColors.Background,
        topBar = {
            Surface(color = HToGoColors.PrimaryDark) {
                Row(
                    Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = Color.White)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Mi ruta del día", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("Orden óptimo calculado con OR-Tools", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                    IconButton(onClick = { repartidorViewModel.cargarPlan() }, enabled = !calculando) {
                        if (calculando) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Refresh, "Recalcular", tint = Color.White)
                        }
                    }
                }
            }
        }
    ) { padding ->
        val disponibles = agenda.filter { !it.reservadoPorMi }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            val r = ruta
            if (r != null && r.paradas.isNotEmpty()) {
                item { MapaRuta(r) }
                item { ResumenRuta(r) }
                item {
                    val siguiente = r.paradas.first()
                    Button(
                        onClick = {
                            abriendo = true
                            repartidorViewModel.iniciarParada(
                                siguiente,
                                onListo = { abriendo = false; onIrAParada() },
                                onError = { abriendo = false; mostrarAviso(it, largo = true) }
                            )
                        },
                        enabled = !abriendo,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(54.dp),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary)
                    ) {
                        if (abriendo) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Navigation, null)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Ir a la parada 1 · ${siguiente.nombreCliente ?: "Cliente"}",
                            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                item { Titulo("Orden de visita", "${r.paradas.size} ${if (r.paradas.size == 1) "parada" else "paradas"}") }
                items(r.paradas, key = { "p${it.idPedido}" }) { p ->
                    ParadaItem(p, onLiberar = {
                        repartidorViewModel.liberar(p.idPedido) { mostrarAviso(it, largo = true) }
                    })
                }
                if (r.omitidos.isNotEmpty()) {
                    item { Titulo("No entran en esta ruta", "${r.omitidos.size}") }
                    items(r.omitidos, key = { "o${it.idPedido}" }) { o ->
                        Tarjeta(borde = HToGoColors.AccentRose.copy(alpha = 0.4f)) {
                            Text(o.nombreCliente ?: "Pedido #${o.idPedido}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(o.direccion.orEmpty(), fontSize = 13.sp, color = HToGoColors.TextSecondary)
                            Spacer(Modifier.height(4.dp))
                            Text(o.motivo, fontSize = 13.sp, color = HToGoColors.AccentRose)
                        }
                    }
                }
            } else if (!calculando) {
                item { SinParadas(error) }
            }

            item { Titulo("Programados de hoy", if (disponibles.isEmpty()) "" else "${disponibles.size} sin repartidor") }
            if (disponibles.isEmpty()) {
                item {
                    Text(
                        "No hay pedidos programados para hoy que tu purificadora pueda surtir.",
                        fontSize = 13.sp, color = HToGoColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            }
            items(disponibles, key = { "a${it.idPedido}" }) { a ->
                AgendaItem(a, onReservar = {
                    repartidorViewModel.reservar(a.idPedido) { mostrarAviso(it, largo = true) }
                })
            }
        }
    }
}

@Composable
private fun MapaRuta(r: RutaOptimaDto) {
    val primera = r.paradas.first()
    Box(
        Modifier.fillMaxWidth().padding(16.dp).height(280.dp)
            .clip(RoundedCornerShape(16.dp)).background(HToGoColors.OutlineSoft)
    ) {
        OsmRouteMapView(
            originLat = r.origenLat,
            originLon = r.origenLon,
            destLat = primera.lat,
            destLon = primera.lon,
            routePoints = r.coordenadas.map { it.lat to it.lon },
            repartidorLabel = "Tú",
            avisoSinUbicacion = null,
            paradas = r.paradas.map { it.lat to it.lon },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ResumenRuta(r: RutaOptimaDto) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Dato("${conDecimales(r.distanciaTotalKm, 1)} km", "recorrido", Modifier.weight(1f))
        Dato("${r.duracionMin} min", "con entregas", Modifier.weight(1f))
        Dato(
            if (r.capacidadVehiculo > 0) "${r.garrafonesEnRuta}/${r.capacidadVehiculo}" else "${r.garrafonesEnRuta}",
            "garrafones", Modifier.weight(1f)
        )
    }
}

@Composable
private fun Dato(valor: String, etiqueta: String, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, HToGoColors.OutlineSoft)) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(valor, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = HToGoColors.PrimaryDark)
            Text(etiqueta, fontSize = 12.sp, color = HToGoColors.TextSecondary)
        }
    }
}

@Composable
private fun ParadaItem(p: ParadaDto, onLiberar: () -> Unit) {
    val reservado = p.estado == "pendiente_programado" || p.estado == "pendiente"
    Tarjeta(borde = if (p.orden == 1) HToGoColors.AccentEmerald else HToGoColors.OutlineSoft) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(30.dp).clip(CircleShape)
                    .background(if (p.orden == 1) HToGoColors.AccentEmerald else HToGoColors.TextPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text("${p.orden}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.nombreCliente ?: "Pedido #${p.idPedido}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(p.direccion.orEmpty(), fontSize = 13.sp, color = HToGoColors.TextSecondary)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Chip(textoEstado(p.estado), HToGoColors.Primary)
                    Chip("${p.garrafones} garrafones", HToGoColors.TextSecondary)
                    if (p.fueraDeHorario) Chip("Fuera de horario", HToGoColors.AccentRose)
                }
                Spacer(Modifier.height(6.dp))
                val llegada = fechaCdmx(p.llegadaEstimada)?.horaAmPm()
                val programada = fechaCdmx(p.fechaProgramada)?.horaAmPm()
                Text(
                    listOfNotNull(
                        llegada?.let { "Llegada ~$it" },
                        programada?.let { "programado $it" },
                        if (p.orden > 1) "${conDecimales(p.distanciaDesdeAnteriorKm, 1)} km desde la anterior" else null
                    ).joinToString(" · "),
                    fontSize = 12.sp, color = HToGoColors.TextSecondary
                )
            }
            if (reservado) {
                TextButton(onClick = onLiberar) { Text("Liberar", fontSize = 13.sp) }
            }
        }
    }
}

@Composable
private fun AgendaItem(a: AgendaPedidoDto, onReservar: () -> Unit) {
    Tarjeta(borde = HToGoColors.OutlineSoft) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Schedule, null, tint = HToGoColors.Primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    listOfNotNull(fechaCdmx(a.fechaProgramada)?.horaAmPm(), a.nombreCliente).joinToString(" · "),
                    fontWeight = FontWeight.SemiBold, fontSize = 15.sp
                )
                Text(a.direccion ?: a.colonia.orEmpty(), fontSize = 13.sp, color = HToGoColors.TextSecondary)
                Text(
                    listOfNotNull(
                        "${a.garrafonesTotales} garrafones",
                        a.distanciaM?.let { "a ${conDecimales(it / 1000, 1)} km" }
                    ).joinToString(" · "),
                    fontSize = 12.sp, color = HToGoColors.TextSecondary
                )
            }
            Button(
                onClick = onReservar,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.PrimaryDark),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) { Text("Tomar", fontSize = 14.sp) }
        }
    }
}

@Composable
private fun SinParadas(error: String?) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            if (error != null) Icons.Filled.Warning else Icons.Filled.Route, null,
            tint = if (error != null) HToGoColors.AccentAmber else HToGoColors.TextTertiary,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            error ?: "Aún no tienes paradas",
            fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = HToGoColors.TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Toma pedidos programados de abajo o acepta pedidos disponibles; aquí verás el mejor orden para entregarlos.",
            fontSize = 13.sp, color = HToGoColors.TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun Titulo(texto: String, detalle: String) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(texto, style = MaterialTheme.typography.titleMedium, color = HToGoColors.TextPrimary, modifier = Modifier.weight(1f))
        Text(detalle, fontSize = 12.sp, color = HToGoColors.TextSecondary)
    }
}

@Composable
private fun Tarjeta(borde: Color, contenido: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, borde)
    ) {
        Column(Modifier.padding(14.dp), content = contenido)
    }
}

@Composable
private fun Chip(texto: String, color: Color) {
    Surface(shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.1f)) {
        Text(texto, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

private fun textoEstado(estado: String): String = when (estado) {
    "pendiente_programado", "pendiente" -> "Reservado"
    "asignado" -> "Aceptado"
    "en_camino" -> "En camino"
    else -> estado
}
