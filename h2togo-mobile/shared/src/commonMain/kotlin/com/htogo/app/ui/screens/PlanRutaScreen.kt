package com.htogo.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
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
import com.htogo.app.data.location.EntregaEnCurso
import com.htogo.app.data.notificaciones.CanalNotificaciones
import com.htogo.app.plataforma.mostrarAviso
import com.htogo.app.ui.RepartidorViewModel
import com.htogo.app.ui.components.BarraEstadoClara
import com.htogo.app.ui.components.OsmRouteMapView
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.util.conDecimales
import com.htogo.app.util.fechaCdmx
import com.htogo.app.util.horaAmPm
import kotlinx.coroutines.delay

/** Paleta de alto contraste sincronizada con PALETA_TRAMOS en MapasHtml.kt para distinguir cada tramo de la ruta OR-Tools. */
private val ColoresTramoRuta = listOf(
    Color(0xFF10B981), // Tramo 1: Verde Esmeralda
    Color(0xFF7C3AED), // Tramo 2: Violeta intenso
    Color(0xFFEA580C), // Tramo 3: Naranja vivo
    Color(0xFF2563EB), // Tramo 4: Azul Rey
    Color(0xFFDB2777), // Tramo 5: Rosa / Magenta
    Color(0xFF0891B2)  // Tramo 6: Turquesa / Cian
)

private fun colorTramo(orden: Int): Color =
    ColoresTramoRuta[(orden - 1).coerceAtLeast(0) % ColoresTramoRuta.size]

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
    val liveMiNegocio by repartidorViewModel.miNegocio.collectAsState()
    val entregasCompletadasRuta by repartidorViewModel.entregasCompletadasRuta.collectAsState()
    var abriendo by remember { mutableStateOf(false) }

    // FakeGPS para simular el recorrido completo de la ruta OR-Tools partiendo desde la purificadora
    // FakeGPS compartido con la pantalla de entrega: tras entregar, sigue desde ese domicilio.
    var posicionSimulada by remember {
        mutableStateOf(repartidorViewModel.ultimaPosicionSimulada?.let { it.lat to it.lon })
    }
    LaunchedEffect(posicionSimulada) {
        repartidorViewModel.ultimaPosicionSimulada =
            posicionSimulada?.let { com.htogo.app.data.location.Posicion(it.first, it.second, 5f) }
    }
    var simulandoRecorrido by remember { mutableStateOf(false) }
    var pasoSimulado by remember { mutableStateOf(0) }
    var totalPasosSimulados by remember { mutableStateOf(0) }
    var menuFakeGps by remember { mutableStateOf(false) }

    val ubicacionPurificadora = remember(liveMiNegocio, ruta) {
        val dir = liveMiNegocio?.direccionObj
        when {
            dir?.lat != null && dir.lon != null && (dir.lat != 0.0 || dir.lon != 0.0) -> dir.lat to dir.lon
            ruta != null && (ruta!!.origenLat != 0.0 || ruta!!.origenLon != 0.0) -> ruta!!.origenLat to ruta!!.origenLon
            else -> 19.376692 to -99.165057
        }
    }
    val nombrePurificadora = liveMiNegocio?.nombreComercial?.ifBlank { null } ?: "Purificadora"

    LaunchedEffect(posicionSimulada != null) {
        EntregaEnCurso.simulacionActiva.value = posicionSimulada != null
    }
    DisposableEffect(Unit) {
        onDispose { EntregaEnCurso.simulacionActiva.value = false }
    }

    LaunchedEffect(Unit) {
        if (liveMiNegocio == null) repartidorViewModel.cargarMiNegocio()
        repartidorViewModel.cargarPlan()
    }
    // RN-009: si un cliente cancela un pedido de la ruta (o se activa un programado), se recalcula.
    LaunchedEffect(Unit) {
        CanalNotificaciones.avisos.collect { aviso ->
            if (aviso.datos["tipo"] == "ruta_recalculada") repartidorViewModel.cargarPlan()
        }
    }

    // Simulación paso a paso desde la purificadora siguiendo toda la ruta OR-Tools
    LaunchedEffect(simulandoRecorrido) {
        if (!simulandoRecorrido) return@LaunchedEffect
        // Desde donde vaya la simulación (la última entrega); solo la primera vez, desde la purificadora.
        val base = posicionSimulada ?: ubicacionPurificadora
        posicionSimulada = base
        var recalculoListo = false
        repartidorViewModel.cargarPlanDesdeUbicacion(base.first, base.second) {
            recalculoListo = true
        }
        var espera = 0
        while (!recalculoListo && espera < 12) {
            delay(200)
            espera++
        }
        val rActual = repartidorViewModel.rutaOptima.value ?: ruta
        val puntosRuta = rActual?.coordenadas?.map { it.lat to it.lon }.orEmpty()
        if (puntosRuta.size < 2) {
            simulandoRecorrido = false
            mostrarAviso("No hay coordenadas de ruta para simular")
            return@LaunchedEffect
        }
        val ultimoPunto = puntosRuta.last()
        val pasos = com.htogo.app.data.location.interpolarPasosRuta(
            ruta = puntosRuta,
            destLat = ultimoPunto.first,
            destLon = ultimoPunto.second,
            pasoMetros = 45.0
        )
        totalPasosSimulados = pasos.size
        for ((index, pto) in pasos.withIndex()) {
            if (!simulandoRecorrido) break
            pasoSimulado = index + 1
            posicionSimulada = pto
            repartidorViewModel.reportarUbicacionSimulada(pto.first, pto.second)
            delay(1_200L)
        }
        if (simulandoRecorrido) {
            simulandoRecorrido = false
            mostrarAviso("🏁 FakeGPS: Recorrido completo de la ruta del día finalizado", largo = true)
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
                    IconButton(
                        onClick = {
                            val sim = posicionSimulada
                            if (sim != null) {
                                repartidorViewModel.cargarPlanDesdeUbicacion(sim.first, sim.second)
                            } else {
                                repartidorViewModel.cargarPlan()
                            }
                        },
                        enabled = !calculando
                    ) {
                        if (calculando) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Refresh, "Recalcular", tint = Color.White)
                        }
                    }
                    Box {
                        IconButton(onClick = { menuFakeGps = true }) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "Opciones FakeGPS",
                                tint = if (posicionSimulada != null) Color(0xFFC4B5FD) else Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = menuFakeGps,
                            onDismissRequest = { menuFakeGps = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (simulandoRecorrido) "⏸️ Pausar recorrido OR-Tools (FakeGPS)"
                                        else if (posicionSimulada != null) "▶️ Simular ruta desde aquí (FakeGPS)"
                                        else "▶️ Simular ruta desde purificadora (FakeGPS)",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                onClick = {
                                    menuFakeGps = false
                                    simulandoRecorrido = !simulandoRecorrido
                                    if (simulandoRecorrido) {
                                        mostrarAviso("🛰️ Saliendo de $nombrePurificadora y siguiendo ruta OR-Tools…")
                                    } else {
                                        mostrarAviso("⏸️ Simulación FakeGPS en pausa")
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🏪 Situar en la purificadora (inicio de ruta)") },
                                onClick = {
                                    menuFakeGps = false
                                    simulandoRecorrido = false
                                    val base = ubicacionPurificadora
                                    posicionSimulada = base
                                    repartidorViewModel.cargarPlanDesdeUbicacion(base.first, base.second)
                                    mostrarAviso("📍 FakeGPS situado en $nombrePurificadora")
                                }
                            )
                            if (posicionSimulada != null) {
                                DropdownMenuItem(
                                    text = { Text("📍 Volver al GPS real del teléfono") },
                                    onClick = {
                                        menuFakeGps = false
                                        simulandoRecorrido = false
                                        posicionSimulada = null
                                        repartidorViewModel.cargarPlan()
                                        mostrarAviso("Volviendo al GPS real")
                                    }
                                )
                            }
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
                item {
                    MapaRuta(
                        r = r,
                        posicionSimulada = posicionSimulada,
                        simulandoRecorrido = simulandoRecorrido,
                        pasoSimulado = pasoSimulado,
                        totalPasosSimulados = totalPasosSimulados,
                        baseLat = ubicacionPurificadora.first,
                        baseLon = ubicacionPurificadora.second,
                        nombrePurificadora = nombrePurificadora,
                        onToggleSimulacion = { simulandoRecorrido = !simulandoRecorrido },
                        onCerrarSimulacion = {
                            simulandoRecorrido = false
                            posicionSimulada = null
                            repartidorViewModel.cargarPlan()
                        }
                    )
                }
                item { LeyendaColoresTramos(r, nombrePurificadora) }
                item { ResumenRuta(r) }
                item {
                    val siguiente = r.paradas.first()
                    val numActual = entregasCompletadasRuta + 1
                    val numTotal = entregasCompletadasRuta + r.paradas.size
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
                            "Ir a la entrega $numActual de $numTotal · ${siguiente.nombreCliente ?: "Cliente"}",
                            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                item {
                    val totalRuta = entregasCompletadasRuta + r.paradas.size
                    val subtituloOrden = if (entregasCompletadasRuta > 0) {
                        "${r.paradas.size} restantes ($entregasCompletadasRuta de $totalRuta entregadas)"
                    } else {
                        "${r.paradas.size} ${if (r.paradas.size == 1) "entrega" else "entregas"}"
                    }
                    Titulo("Orden de visita", subtituloOrden)
                }
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
private fun MapaRuta(
    r: RutaOptimaDto,
    posicionSimulada: Pair<Double, Double>?,
    simulandoRecorrido: Boolean,
    pasoSimulado: Int,
    totalPasosSimulados: Int,
    baseLat: Double,
    baseLon: Double,
    nombrePurificadora: String,
    onToggleSimulacion: () -> Unit,
    onCerrarSimulacion: () -> Unit
) {
    val primera = r.paradas.first()
    val originLatEfectivo = posicionSimulada?.first ?: r.origenLat
    val originLonEfectivo = posicionSimulada?.second ?: r.origenLon
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(290.dp)
            .clip(RoundedCornerShape(16.dp)).background(HToGoColors.OutlineSoft)
    ) {
        OsmRouteMapView(
            originLat = originLatEfectivo,
            originLon = originLonEfectivo,
            destLat = primera.lat,
            destLon = primera.lon,
            routePoints = r.coordenadas.map { it.lat to it.lon },
            repartidorLabel = if (posicionSimulada != null) "Tú (FakeGPS)" else "Tú",
            avisoSinUbicacion = null,
            paradas = r.paradas.map { it.lat to it.lon },
            baseLat = baseLat,
            baseLon = baseLon,
            baseLabel = nombrePurificadora,
            modifier = Modifier.fillMaxSize()
        )

        if (posicionSimulada != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF7C3AED).copy(alpha = 0.95f),
                shadowElevation = 4.dp,
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (simulandoRecorrido) "🛰️ FakeGPS ($pasoSimulado/$totalPasosSimulados)"
                        else "🧪 FakeGPS en $nombrePurificadora",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onToggleSimulacion() }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (simulandoRecorrido) "⏸" else "▶",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onToggleSimulacion() }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "✕",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onCerrarSimulacion() }
                    )
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.88f),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .clickable { onToggleSimulacion() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Storefront, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "▶ Probar FakeGPS desde $nombrePurificadora",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun LeyendaColoresTramos(r: RutaOptimaDto, nombrePurificadora: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(99.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.08f),
            border = BorderStroke(1.dp, Color(0xFF0F172A).copy(alpha = 0.2f))
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🏪", fontSize = 11.sp)
                Spacer(Modifier.width(4.dp))
                Text(
                    "Base: $nombrePurificadora",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HToGoColors.TextPrimary
                )
            }
        }
        r.paradas.forEach { p ->
            val color = colorTramo(p.orden)
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = color.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Tramo ${p.orden} → Parada ${p.orden}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
        }
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
    val colorParada = colorTramo(p.orden)
    Tarjeta(borde = colorParada.copy(alpha = if (p.orden == 1) 0.85f else 0.45f)) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(30.dp).clip(CircleShape)
                    .background(colorParada),
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
                    Chip("Tramo ${p.orden}", colorParada)
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
