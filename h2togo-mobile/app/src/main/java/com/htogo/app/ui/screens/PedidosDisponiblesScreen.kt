package com.htogo.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.htogo.app.ui.RepartidorViewModel
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme

//enum class TipoPedido { DIRECTO, ABIERTO }

data class PedidoDisponible(
    val id: String,
    val cliente: String,
    val direccion: String,
    val colonia: String,
    val productos: String,
    val total: Double,
    val precioMaximo: Double? = null,
    val distanciaKm: Double,
    val tiempoEstimadoMin: Int,
    val tipo: TipoPedido = TipoPedido.DIRECTO,
    val esPrioritario: Boolean = false
)

enum class StockStatus { SUFICIENTE_VEHICULO, REQUIERE_BASE, INSUFICIENTE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidosDisponiblesScreen(
    onBack: () -> Unit = {},
    onAceptar: () -> Unit = {},
    repartidorViewModel: RepartidorViewModel = viewModel()
) {
    val liveDisponibles by repartidorViewModel.pedidosDisponibles.collectAsState()
    val isLoading by repartidorViewModel.isLoading.collectAsState()
    val errorMsg by repartidorViewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        repartidorViewModel.cargarPedidosDisponibles()
    }

    val pedidos = remember(liveDisponibles) {
        if (liveDisponibles.isNotEmpty()) {
            liveDisponibles.map { p ->
                PedidoDisponible(
                    id = p.id.toString(),
                    cliente = "Cliente H2ToGo",
                    direccion = p.direccionResumen ?: "Benito Juárez, CDMX",
                    colonia = "Zona Cobertura",
                    productos = "${p.garrafonesTotales} × Garrafón 20 L",
                    total = p.totalEstimado ?: 45.0,
                    distanciaKm = p.distanciaKm ?: 1.5,
                    tiempoEstimadoMin = p.tiempoEstimadoMinutos ?: 10,
                    tipo = TipoPedido.DIRECTO,
                    esPrioritario = false
                )
            }
        } else {
            emptyList()
        }
    }

    var pedidoSeleccionado by remember { mutableStateOf<PedidoDisponible?>(null) }
    var stockSimulado by remember { mutableStateOf(StockStatus.SUFICIENTE_VEHICULO) }

    Scaffold(
        containerColor = HToGoColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Pedidos disponibles",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${pedidos.size} disponibles",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = .8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { repartidorViewModel.cargarPedidosDisponibles() }) {
                        Icon(Icons.Filled.FilterList, "Refrescar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HToGoColors.PrimaryDark,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (errorMsg != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        errorMsg!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (isLoading && pedidos.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = HToGoColors.Primary)
                }
            } else if (pedidos.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Warehouse,
                            null,
                            tint = HToGoColors.TextTertiary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No hay pedidos disponibles en este momento",
                            color = HToGoColors.TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { repartidorViewModel.cargarPedidosDisponibles() }) {
                            Text("Actualizar")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(pedidos) { pedido ->
                        PedidoDisponibleCard(
                            pedido = pedido,
                            onClick = { pedidoSeleccionado = pedido }
                        )
                    }
                }
            }
        }
    }

    pedidoSeleccionado?.let { pedido ->
        AceptarPedidoDialog(
            pedido = pedido,
            stock = stockSimulado,
            onCambiarStock = {
                stockSimulado = when (stockSimulado) {
                    StockStatus.SUFICIENTE_VEHICULO -> StockStatus.REQUIERE_BASE
                    StockStatus.REQUIERE_BASE -> StockStatus.INSUFICIENTE
                    StockStatus.INSUFICIENTE -> StockStatus.SUFICIENTE_VEHICULO
                }
            },
            onDismiss = { pedidoSeleccionado = null },
            onConfirm = {
                val pedidoIdInt = pedido.id.toIntOrNull()
                if (pedidoIdInt != null) {
                    repartidorViewModel.aceptarPedido(
                        id = pedidoIdInt,
                        onSuccess = {
                            pedidoSeleccionado = null
                            onAceptar()
                        },
                        onError = { }
                    )
                } else {
                    pedidoSeleccionado = null
                    onAceptar()
                }
            }
        )
    }
}

@Composable
private fun PedidoDisponibleCard(
    pedido: PedidoDisponible,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            if (pedido.esPrioritario) 2.dp else 1.dp,
            if (pedido.esPrioritario) HToGoColors.AccentAmber else HToGoColors.OutlineSoft
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (pedido.esPrioritario) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(HToGoColors.AccentAmber.copy(alpha = .15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            Icons.Filled.Bolt, null,
                            tint = HToGoColors.AccentAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "PRIORITARIO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = HToGoColors.AccentAmber
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                }
                if (pedido.tipo == TipoPedido.ABIERTO && pedido.precioMaximo != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(HToGoColors.AccentAmber.copy(alpha = .18f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            Icons.Filled.PriceCheck, null,
                            tint = HToGoColors.AccentAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Precio máx $${pedido.precioMaximo.toInt()}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = HToGoColors.AccentAmber
                        )
                    }
                }
            }
            if (pedido.esPrioritario || pedido.tipo == TipoPedido.ABIERTO) {
                Spacer(Modifier.height(8.dp))
            }
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("#${pedido.id}", fontSize = 11.sp, color = HToGoColors.TextSecondary)
                    Text(
                        pedido.cliente,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = HToGoColors.TextPrimary
                    )
                    Text(
                        "${pedido.direccion} · ${pedido.colonia}",
                        fontSize = 12.sp,
                        color = HToGoColors.TextSecondary,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
                if (pedido.tipo == TipoPedido.DIRECTO) {
                    Text(
                        "$%.0f".format(pedido.total),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = HToGoColors.StatusEntregado
                    )
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Abierto",
                            fontSize = 11.sp,
                            color = HToGoColors.AccentAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Tú propones",
                            fontSize = 11.sp,
                            color = HToGoColors.TextSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.WaterDrop, null,
                        tint = HToGoColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        pedido.productos,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = HToGoColors.TextPrimary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.LocationOn, null,
                    tint = HToGoColors.Primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "%.1f km".format(pedido.distanciaKm),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = HToGoColors.TextPrimary
                )
                Spacer(Modifier.width(12.dp))
                Icon(
                    Icons.Filled.Schedule, null,
                    tint = HToGoColors.Primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "~${pedido.tiempoEstimadoMin} min",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = HToGoColors.TextPrimary
                )
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(23.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary)
            ) {
                Icon(Icons.Filled.Check, null)
                Spacer(Modifier.width(6.dp))
                Text("Aceptar pedido", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AceptarPedidoDialog(
    pedido: PedidoDisponible,
    stock: StockStatus,
    onCambiarStock: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("¿Aceptar este pedido?", fontWeight = FontWeight.SemiBold)
        },
        text = {
            Column {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = HToGoColors.PrimaryWash,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "#${pedido.id} · ${pedido.cliente}",
                            fontSize = 12.sp,
                            color = HToGoColors.TextSecondary
                        )
                        Text(
                            pedido.productos,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HToGoColors.TextPrimary
                        )
                        if (pedido.tipo == TipoPedido.ABIERTO && pedido.precioMaximo != null) {
                            Text(
                                "Pedido abierto · Precio máx $${pedido.precioMaximo.toInt()}",
                                fontSize = 12.sp,
                                color = HToGoColors.AccentAmber,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                "Total $%.0f MXN".format(pedido.total),
                                fontSize = 12.sp,
                                color = HToGoColors.StatusEntregado,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                StockNotice(stock)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onCambiarStock) {
                    Text(
                        "(demo) cambiar estado de stock",
                        fontSize = 11.sp,
                        color = HToGoColors.TextTertiary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = stock != StockStatus.INSUFICIENTE,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HToGoColors.Primary,
                    disabledContainerColor = HToGoColors.OutlineSoft,
                    disabledContentColor = HToGoColors.TextTertiary
                )
            ) {
                Icon(Icons.Filled.Check, null)
                Spacer(Modifier.width(4.dp))
                Text("Aceptar pedido")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun StockNotice(stock: StockStatus) {
    val (bgColor, accent, icon, title, body) = when (stock) {
        StockStatus.SUFICIENTE_VEHICULO -> Quintuple(
            HToGoColors.StatusEntregado.copy(alpha = .10f),
            HToGoColors.StatusEntregado,
            Icons.Filled.CheckCircle,
            "Stock disponible",
            "Tienes 5 garrafones de Ciel en tu vehículo (suficiente)"
        )
        StockStatus.REQUIERE_BASE -> Quintuple(
            HToGoColors.AccentAmber.copy(alpha = .12f),
            HToGoColors.AccentAmber,
            Icons.Filled.Warning,
            "Carga adicional requerida",
            "Solo tienes 2 en tu vehículo. Tendrás que cargar 1 más desde la base antes de salir."
        )
        StockStatus.INSUFICIENTE -> Quintuple(
            HToGoColors.AccentRose.copy(alpha = .12f),
            HToGoColors.AccentRose,
            Icons.Filled.Cancel,
            "Sin stock suficiente",
            "No hay stock suficiente entre tu vehículo y la base"
        )
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accent
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    body,
                    fontSize = 12.sp,
                    color = HToGoColors.TextPrimary
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.DirectionsCar, null,
                        tint = HToGoColors.TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Vehículo",
                        fontSize = 11.sp,
                        color = HToGoColors.TextSecondary
                    )
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        Icons.Filled.Warehouse, null,
                        tint = HToGoColors.TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Base",
                        fontSize = 11.sp,
                        color = HToGoColors.TextSecondary
                    )
                }
            }
        }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val a: A, val b: B, val c: C, val d: D, val e: E
)

@Preview(showBackground = true, widthDp = 412, heightDp = 868)
@Composable
fun PedidosDisponiblesScreenPreview() {
    HToGoTheme { PedidosDisponiblesScreen() }
}
