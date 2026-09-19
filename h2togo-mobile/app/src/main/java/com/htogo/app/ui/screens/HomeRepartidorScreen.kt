package com.htogo.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warehouse
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htogo.app.ui.components.EstadoPedido
import com.htogo.app.ui.components.EstadoPedidoChip
import com.htogo.app.ui.components.RepartidorBottomBar
import com.htogo.app.ui.components.RepartidorTab
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.htogo.app.ui.RepartidorViewModel

@Composable
fun HomeRepartidorScreen(
    onInventario: () -> Unit = {},
    onIngresos: () -> Unit = {},
    onPerfil: () -> Unit = {},
    onRuta: () -> Unit = {},
    onPedidoProgramado: () -> Unit = {},
    onSwitchRol: () -> Unit = {},
    onPedidosDisponibles: () -> Unit = {},
    repartidorViewModel: RepartidorViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { com.htogo.app.data.local.SessionManager.getInstance(context) }
    val nombre = remember { sessionManager.obtenerNombre()?.substringBefore(" ")?.ifBlank { "Repartidor" } ?: "Repartidor" }
    var enLinea by remember { mutableStateOf(true) }

    val livePedidosDisponibles by repartidorViewModel.pedidosDisponibles.collectAsState()
    val livePedidoEnRuta by repartidorViewModel.pedidoEnRuta.collectAsState()
    val liveVehiculo by repartidorViewModel.inventarioVehiculo.collectAsState()
    val liveBase by repartidorViewModel.inventarioBase.collectAsState()
    val liveMiNegocio by repartidorViewModel.miNegocio.collectAsState()
    val totalEntregas by repartidorViewModel.totalEntregas.collectAsState()
    val liveMisEntregas by repartidorViewModel.misEntregas.collectAsState()

    val pedidosApartados = remember(liveMisEntregas) {
        liveMisEntregas.filter {
            it.estado.equals("asignado", ignoreCase = true) ||
            it.estado.equals("en_camino", ignoreCase = true)
        }
    }
    val pedidosEntregados = remember(liveMisEntregas) {
        liveMisEntregas.filter { it.estado.equals("entregado", ignoreCase = true) }
    }
    val totalIngresosHoy = remember(pedidosEntregados) {
        pedidosEntregados.sumOf { it.totalPagar ?: 0.0 }
    }
    val entregasHoyCount = remember(pedidosEntregados) {
        pedidosEntregados.size
    }
    val enRutaCount = remember(liveMisEntregas, livePedidoEnRuta) {
        if (livePedidoEnRuta != null) 1 else liveMisEntregas.count { it.estado.equals("en_camino", ignoreCase = true) }
    }

    val nombreNegocio = liveMiNegocio?.nombreComercial ?: sessionManager.obtenerNombreNegocio() ?: "Purificadora"
    val fechaHoy = remember {
        val sdf = java.text.SimpleDateFormat("EEEE d 'de' MMMM", java.util.Locale("es", "MX"))
        sdf.format(java.util.Date()).replaceFirstChar { it.uppercase() }
    }

    LaunchedEffect(Unit) {
        repartidorViewModel.cargarMiNegocio()
        repartidorViewModel.cargarMisEntregas()
    }

    Scaffold(
        containerColor = HToGoColors.Background,
        bottomBar = {
            RepartidorBottomBar(
                selected = RepartidorTab.INICIO,
                onInicio = {},
                onNegocio = onInventario,
                onIngresos = onIngresos,
                onPerfil = onPerfil
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Box(
                Modifier.fillMaxWidth().background(
                    Brush.verticalGradient(listOf(HToGoColors.PrimaryDark, HToGoColors.Primary))
                ).statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = .2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                nombre.firstOrNull()?.toString()?.uppercase() ?: "R",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Hola, $nombre",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Storefront,
                                    null,
                                    tint = HToGoColors.PrimarySoft,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    nombreNegocio,
                                    color = HToGoColors.PrimarySoft,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                fechaHoy,
                                color = HToGoColors.PrimarySoft.copy(alpha = .85f),
                                fontSize = 12.sp
                            )
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(Color.White.copy(alpha = .18f))
                                .clickable { onSwitchRol() }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.SwapHoriz,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Rol",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = {}) {
                            Icon(Icons.Filled.Notifications, null, tint = Color.White)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(10.dp).clip(CircleShape)
                                    .background(if (enLinea) HToGoColors.StatusEntregado else HToGoColors.StatusPendiente)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (enLinea) "En línea" else "Fuera de línea",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HToGoColors.TextPrimary
                                )
                                Text(
                                    if (enLinea) "Recibiendo pedidos" else "Activa para recibir pedidos",
                                    fontSize = 12.sp,
                                    color = HToGoColors.TextSecondary
                                )
                            }
                            Switch(
                                checked = enLinea,
                                onCheckedChange = { enLinea = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = HToGoColors.Primary
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Tu jornada de hoy",
                style = MaterialTheme.typography.titleMedium,
                color = HToGoColors.TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("Entregas", "$entregasHoyCount", "/ ${liveMisEntregas.size}", Icons.Filled.CheckCircle, HToGoColors.StatusEntregado, Modifier.weight(1f))
                StatCard("En tu vehículo", "${liveVehiculo?.ocupado ?: 0}", "garrafones", Icons.Filled.DirectionsCar, HToGoColors.Primary, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("En base", "${liveBase?.lotes?.sumOf { it.cantidadActual } ?: 0}", "garrafones", Icons.Filled.Warehouse, HToGoColors.AccentPurple, Modifier.weight(1f))
                StatCard("Ingresos", "$${totalIngresosHoy.toInt()}", "MXN", Icons.Filled.Payments, HToGoColors.StatusAsignado, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("En ruta", "$enRutaCount", if (enRutaCount == 1) "pedido activo" else "pedidos", Icons.Filled.Schedule, HToGoColors.PrimaryDark, Modifier.weight(1f))
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))
            if (livePedidoEnRuta != null) {
                val pedido = livePedidoEnRuta!!
                val cant = pedido.garrafonesTotales ?: (pedido.detalles?.sumOf { it.cantidad } ?: 1)
                val direccionText = pedido.indicaciones?.ifBlank { null } ?: "Dirección de entrega asignada"
                Text(
                    "Entrega en curso",
                    style = MaterialTheme.typography.titleMedium,
                    color = HToGoColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(8.dp))
                Card(
                    onClick = onRuta,
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
                            ) { Icon(Icons.Filled.WaterDrop, null, tint = HToGoColors.Primary) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Pedido #${pedido.id}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HToGoColors.TextPrimary
                                )
                                Text(
                                    "$cant garrafones",
                                    fontSize = 13.sp,
                                    color = HToGoColors.TextSecondary
                                )
                            }
                            EstadoPedidoChip(EstadoPedido.EN_CAMINO)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Map, null, tint = HToGoColors.TextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(direccionText, fontSize = 13.sp, color = HToGoColors.TextSecondary)
                        }
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = onRuta,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary)
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ver ruta de entrega", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                Text(
                    "Pedidos disponibles (${livePedidosDisponibles.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = HToGoColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(8.dp))
                Card(
                    onClick = onPedidosDisponibles,
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
                            ) { Icon(Icons.Filled.Storefront, null, tint = HToGoColors.Primary) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (livePedidosDisponibles.isNotEmpty())
                                        "${livePedidosDisponibles.size} pedidos listos para entrega"
                                    else
                                        "No hay pedidos pendientes",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HToGoColors.TextPrimary
                                )
                                Text(
                                    "En tu zona de cobertura",
                                    fontSize = 13.sp,
                                    color = HToGoColors.TextSecondary
                                )
                            }
                            EstadoPedidoChip(EstadoPedido.PENDIENTE)
                        }
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = onPedidosDisponibles,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary)
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ver pedidos disponibles", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Pedidos apartados aceptados (${pedidosApartados.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = HToGoColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                "Pedidos programados o asignados que te corresponden",
                fontSize = 12.sp,
                color = HToGoColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
            )
            Spacer(Modifier.height(8.dp))

            if (pedidosApartados.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HToGoColors.OutlineSoft)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Schedule, null, tint = HToGoColors.TextTertiary, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No tienes pedidos apartados", fontWeight = FontWeight.SemiBold, color = HToGoColors.TextPrimary, fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Cuando te asignes o apartes un pedido aparecerá aquí.", fontSize = 12.sp, color = HToGoColors.TextSecondary, textAlign = TextAlign.Center)
                    }
                }
            } else {
                pedidosApartados.forEach { p ->
                    val chipEstado = when (p.estado.lowercase()) {
                        "asignado" -> EstadoPedido.ASIGNADO
                        "en_camino" -> EstadoPedido.EN_CAMINO
                        "entregado" -> EstadoPedido.ENTREGADO
                        "cancelado" -> EstadoPedido.CANCELADO
                        else -> EstadoPedido.PENDIENTE
                    }
                    val formattedFecha = p.fechaCreacion?.take(10) ?: "Hoy"
                    ColaItem(
                        id = "${p.id}",
                        cliente = if (p.esProgramado) "Pedido programado" else "Entrega #${p.id}",
                        colonia = formattedFecha,
                        cant = "${p.garrafonesTotales ?: 1} garrafones",
                        monto = "$${p.totalPagar?.toInt() ?: 0}",
                        estado = chipEstado,
                        onClick = onPedidoProgramado
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String, big: String, small: String, icon: ImageVector,
    accent: Color, modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(accent.copy(alpha = .14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp)) }
                Spacer(Modifier.width(8.dp))
                Text(label, fontSize = 12.sp, color = HToGoColors.TextSecondary)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(big, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = HToGoColors.TextPrimary)
                Spacer(Modifier.width(4.dp))
                Text(
                    small, fontSize = 12.sp, color = HToGoColors.TextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ColaItem(
    id: String, cliente: String, colonia: String, cant: String, monto: String,
    estado: EstadoPedido, onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "#$id", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = HToGoColors.TextPrimary
                    )
                    Spacer(Modifier.width(8.dp)); EstadoPedidoChip(estado)
                }
                Text("$cliente · $colonia", fontSize = 12.sp, color = HToGoColors.TextSecondary)
                Text(cant, fontSize = 12.sp, color = HToGoColors.TextSecondary)
            }
            Text(monto, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HToGoColors.PrimaryDark)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun HomeRepartidorPreview() { HToGoTheme { HomeRepartidorScreen() } }
