package com.htogo.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.window.Dialog
import com.htogo.app.ui.components.RepartidorBottomBar
import com.htogo.app.ui.components.RepartidorTab
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme

private enum class TabInventario { EN_BASE, EN_VEHICULO, VEHICULO }

private data class MarcaBaseStock(
    val id: String,
    val codigo: String,
    val nombre: String,
    val capacidad: String,
    val precio: Double,
    val proveedor: String,
    val enBase: Int,
    val maximoBase: Int,
    val accent: Color,
    val lotes: List<Lote> = emptyList()
)

private data class Lote(
    val codigo: String,
    val cantidad: Int,
    val fechaCaducidad: String,
    val diasParaCaducar: Int
)

private data class MarcaVehiculoStock(
    val id: String,
    val codigo: String,
    val nombre: String,
    val capacidad: String,
    val precio: Double,
    val cargados: Int,
    val disponibles: Int,
    val apartados: Int,
    val accent: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioVehiculoScreen(
    onBack: () -> Unit = {},
    onInicio: () -> Unit = {},
    onIngresos: () -> Unit = {},
    onPerfil: () -> Unit = {},
    onProductosPrecios: () -> Unit = {}
) {
    var tab by remember { mutableStateOf(TabInventario.EN_BASE) }
    var showRegistrarEntrada by remember { mutableStateOf(false) }
    var showCargarVehiculo by remember { mutableStateOf(false) }
    var showSalidaManual by remember { mutableStateOf(false) }
    var showSolicitarCambio by remember { mutableStateOf(false) }

    val marcasBase = remember {
        listOf(
            MarcaBaseStock(
                "a", "CIE", "Marca A", "20 L", 45.0, "Proveedor 1", 18, 50, HToGoColors.Primary,
                lotes = listOf(
                    Lote("L-2026-04-12", 6, "12 oct 2026", 159),
                    Lote("L-2026-04-22", 12, "22 oct 2026", 169)
                )
            ),
            MarcaBaseStock(
                "b", "BON", "Marca B", "20 L", 48.0, "Proveedor 2", 12, 50, HToGoColors.AccentEmerald,
                lotes = listOf(
                    Lote("L-2026-03-30", 4, "30 may 2026", 24),
                    Lote("L-2026-04-18", 8, "18 sep 2026", 135)
                )
            ),
            MarcaBaseStock(
                "c", "EPU", "Marca C", "20 L", 42.0, "Proveedor 3", 8, 50, HToGoColors.AccentAmber,
                lotes = listOf(
                    Lote("L-2026-04-05", 8, "20 may 2026", 14)
                )
            )
        )
    }

    val marcasVehiculo = remember {
        listOf(
            MarcaVehiculoStock("a", "CIE", "Marca A", "20 L", 45.0, 12, 8, 4, HToGoColors.Primary),
            MarcaVehiculoStock("b", "BON", "Marca B", "20 L", 48.0, 7, 5, 2, HToGoColors.AccentEmerald),
            MarcaVehiculoStock("c", "EPU", "Marca C", "20 L", 42.0, 3, 1, 2, HToGoColors.AccentAmber)
        )
    }

    Scaffold(
        containerColor = HToGoColors.Background,
        topBar = {
            Column(
                Modifier
                    .background(Brush.linearGradient(listOf(HToGoColors.PrimaryDark, HToGoColors.Primary)))
                    .padding(horizontal = 8.dp)
            ) {
                TopAppBar(
                    title = { Text("Mi negocio", color = Color.White, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Filled.HelpOutline, null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
                TabsInventario(tab) { tab = it }
                Spacer(Modifier.height(14.dp))
            }
        },
        bottomBar = {
            RepartidorBottomBar(
                selected = RepartidorTab.NEGOCIO,
                onInicio = onInicio,
                onNegocio = {},
                onIngresos = onIngresos,
                onPerfil = onPerfil
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (tab) {
                TabInventario.EN_BASE -> {
                    item { ResumenBaseCard() }
                    item { ProductosPreciosShortcut(onProductosPrecios) }
                    item { SectionTitle("Marcas en base · 3") }
                    items(marcasBase) { m -> MarcaBaseCard(m) }
                    item {
                        Button(
                            onClick = { showRegistrarEntrada = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary)
                        ) {
                            Icon(Icons.Filled.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Registrar entrada", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = { showSalidaManual = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, HToGoColors.AccentRose)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, null, tint = HToGoColors.AccentRose)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Registrar salida manual",
                                color = HToGoColors.AccentRose,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    item {
                        Text(
                            "Usa salida manual para mermas, roturas o entregas que no pasaron por la app.",
                            fontSize = 11.sp,
                            color = HToGoColors.TextTertiary,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
                TabInventario.EN_VEHICULO -> {
                    item { ResumenVehiculoCard() }
                    item {
                        Button(
                            onClick = { showCargarVehiculo = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary)
                        ) {
                            Icon(Icons.Filled.MoveToInbox, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Cargar desde la base", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    item { SectionTitle("Carga actual · 3 marcas") }
                    items(marcasVehiculo) { m -> MarcaVehiculoCard(m) }
                }
                TabInventario.VEHICULO -> {
                    item { VehiculoDetalleCard(onSolicitarCambio = { showSolicitarCambio = true }) }
                }
            }
        }
    }

    if (showRegistrarEntrada) {
        RegistrarEntradaDialog(
            marcas = marcasBase,
            onDismiss = { showRegistrarEntrada = false }
        )
    }
    if (showCargarVehiculo) {
        CargarVehiculoDialog(
            marcas = marcasBase,
            onDismiss = { showCargarVehiculo = false }
        )
    }
    if (showSalidaManual) {
        SalidaManualDialog(
            marcas = marcasBase,
            onDismiss = { showSalidaManual = false }
        )
    }
    if (showSolicitarCambio) {
        SolicitarCambioVehiculoDialog(onDismiss = { showSolicitarCambio = false })
    }
}

@Composable
private fun TabsInventario(actual: TabInventario, onChange: (TabInventario) -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = .12f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(4.dp)) {
            TabPill("En base", Icons.Filled.Warehouse, actual == TabInventario.EN_BASE, Modifier.weight(1f)) {
                onChange(TabInventario.EN_BASE)
            }
            TabPill("En mi vehículo", Icons.Filled.LocalShipping, actual == TabInventario.EN_VEHICULO, Modifier.weight(1f)) {
                onChange(TabInventario.EN_VEHICULO)
            }
            TabPill("Vehículo", Icons.Filled.DirectionsCar, actual == TabInventario.VEHICULO, Modifier.weight(1f)) {
                onChange(TabInventario.VEHICULO)
            }
        }
    }
}

@Composable
private fun TabPill(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon, null,
                tint = if (selected) HToGoColors.PrimaryDark else Color.White,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                color = if (selected) HToGoColors.PrimaryDark else Color.White,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ProductosPreciosShortcut(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HToGoColors.OutlineSoft),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(HToGoColors.PrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.AttachMoney, null,
                    tint = HToGoColors.Primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Productos y precios",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HToGoColors.TextPrimary
                )
                Text(
                    "Gestiona el catálogo del negocio · CU-23",
                    fontSize = 11.sp,
                    color = HToGoColors.TextSecondary
                )
            }
            Icon(
                Icons.Filled.ChevronRight, null,
                tint = HToGoColors.TextTertiary
            )
        }
    }
}

@Composable
private fun ResumenBaseCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = HToGoColors.PrimaryDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(HToGoColors.PrimaryDark, HToGoColors.Primary)))
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = .15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Warehouse, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Stock en base · compartido", color = Color.White.copy(alpha = .8f), fontSize = 12.sp)
                        Text("38 garrafones", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text("3 marcas · capacidad 50", color = Color.White.copy(alpha = .85f), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = .18f))
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    HeroStat("76%", "Capacidad ocupada")
                    HeroStat("12", "Entradas hoy")
                    HeroStat("8", "Cargados a vehículos")
                }
            }
        }
    }
}

@Composable
private fun ResumenVehiculoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = HToGoColors.Primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF1E3A8A), HToGoColors.Primary)))
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = .15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.LocalShipping, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("En mi vehículo · hoy", color = Color.White.copy(alpha = .8f), fontSize = 12.sp)
                        Text("22 garrafones", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text("3 marcas cargadas · 25 cap.", color = Color.White.copy(alpha = .85f), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = .18f))
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    HeroStat("14", "Disponibles")
                    HeroStat("8", "Apartados")
                    HeroStat("$1,260", "Ingresos hoy")
                }
            }
        }
    }
}

@Composable
private fun HeroStat(value: String, label: String) {
    Column {
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = .75f), fontSize = 11.sp)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = HToGoColors.TextSecondary,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, start = 4.dp)
    )
}

@Composable
private fun MarcaBaseCard(m: MarcaBaseStock) {
    val porcentaje = m.enBase.toFloat() / m.maximoBase
    val (estadoTxt, estadoColor, estadoIcon) = when {
        porcentaje < 0.25f -> Triple("Stock bajo", HToGoColors.AccentRose, Icons.Filled.Error)
        porcentaje < 0.5f -> Triple("Stock medio", HToGoColors.AccentAmber, Icons.Filled.Warning)
        else -> Triple("Disponible", HToGoColors.AccentEmerald, Icons.Filled.CheckCircle)
    }
    val barColor = when {
        porcentaje < 0.25f -> HToGoColors.AccentRose
        porcentaje < 0.5f -> HToGoColors.AccentAmber
        else -> HToGoColors.AccentEmerald
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, HToGoColors.OutlineSoft)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(m.accent.copy(alpha = .12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(m.codigo, color = m.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("${m.nombre} · ${m.capacidad}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "$%.0f / pieza · ${m.proveedor}".format(m.precio),
                        fontSize = 12.sp, color = HToGoColors.TextSecondary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${m.enBase}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("en base", fontSize = 11.sp, color = HToGoColors.TextSecondary)
                }
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { porcentaje },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = barColor,
                trackColor = HToGoColors.Background
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${m.enBase} / ${m.maximoBase} capacidad de la base",
                    fontSize = 11.sp, color = HToGoColors.TextTertiary,
                    modifier = Modifier.weight(1f)
                )
                Icon(estadoIcon, null, tint = estadoColor, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(estadoTxt, fontSize = 11.sp, color = estadoColor, fontWeight = FontWeight.SemiBold)
            }
            if (m.lotes.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                LotesResumen(m.lotes)
            }
        }
    }
}

@Composable
private fun LotesResumen(lotes: List<Lote>) {
    val proximo = lotes.minByOrNull { it.diasParaCaducar }!!
    val (color, badgeBg, etiqueta) = when {
        proximo.diasParaCaducar <= 15 -> Triple(HToGoColors.AccentRose, HToGoColors.AccentRose.copy(alpha = .12f), "Próximo a caducar")
        proximo.diasParaCaducar <= 45 -> Triple(HToGoColors.AccentAmber, HToGoColors.AccentAmber.copy(alpha = .14f), "Caducidad cercana")
        else -> Triple(HToGoColors.AccentEmerald, HToGoColors.AccentEmerald.copy(alpha = .12f), "Caducidad OK")
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(HToGoColors.Background)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.EventAvailable, null, tint = HToGoColors.Primary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "${lotes.size} ${if (lotes.size == 1) "lote activo" else "lotes activos"}",
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = HToGoColors.TextSecondary,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = badgeBg
            ) {
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.HourglassBottom, null, tint = color, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(etiqueta, fontSize = 10.sp, color = color, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Próximo: ${proximo.codigo} · vence ${proximo.fechaCaducidad} (${proximo.diasParaCaducar} d) · ${proximo.cantidad} pzas",
            fontSize = 10.sp, color = HToGoColors.TextTertiary
        )
    }
}

@Composable
private fun MarcaVehiculoCard(m: MarcaVehiculoStock) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, HToGoColors.OutlineSoft)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(m.accent.copy(alpha = .12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(m.codigo, color = m.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("${m.nombre} · ${m.capacidad}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("$%.0f / pieza".format(m.precio), fontSize = 12.sp, color = HToGoColors.TextSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${m.cargados}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("cargados", fontSize = 11.sp, color = HToGoColors.TextSecondary)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(HToGoColors.Background)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(HToGoColors.AccentEmerald)
                )
                Spacer(Modifier.width(6.dp))
                Text("${m.disponibles}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(" disponibles", fontSize = 11.sp, color = HToGoColors.TextSecondary)
                Text(" · ", fontSize = 11.sp, color = HToGoColors.TextTertiary)
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(HToGoColors.AccentAmber)
                )
                Spacer(Modifier.width(6.dp))
                Text("${m.apartados}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(" apartados para pedidos", fontSize = 11.sp, color = HToGoColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun VehiculoDetalleCard(onSolicitarCambio: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, HToGoColors.OutlineSoft)
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF475569)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.LocalShipping, null, tint = Color.White.copy(alpha = .4f), modifier = Modifier.size(80.dp))
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = HToGoColors.AccentEmerald,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Verified, null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Verificado", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Camioneta · Estaquitas",
                        fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = HToGoColors.PrimarySoft,
                        modifier = Modifier.clickable(onClick = onSolicitarCambio)
                    ) {
                        Text(
                            "Solicitar cambio",
                            color = HToGoColors.Primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VField("Tipo", "Camioneta", Modifier.weight(1f), Icons.Filled.LocalShipping)
                    VField("Año", "2018", Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                VField("Placas", "PJU-432-A · CDMX", Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VField("Color", "Blanco", Modifier.weight(1f))
                    VField("Combustible", "Gasolina", Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(HToGoColors.PrimarySoft, HToGoColors.PrimaryWash)))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.WaterDrop, null, tint = HToGoColors.Primary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "CAPACIDAD MÁXIMA",
                            fontSize = 11.sp, color = HToGoColors.TextSecondary, fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "25 garrafones",
                            fontSize = 18.sp, color = HToGoColors.PrimaryDark, fontWeight = FontWeight.Bold
                        )
                        Text(
                            "de 20 L cada uno · ~500 kg total",
                            fontSize = 12.sp, color = HToGoColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VField(label: String, value: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = HToGoColors.Background,
        modifier = modifier
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label.uppercase(), fontSize = 11.sp, color = HToGoColors.TextSecondary)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                if (icon != null) {
                    Icon(icon, null, tint = HToGoColors.Primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun RegistrarEntradaDialog(marcas: List<MarcaBaseStock>, onDismiss: () -> Unit) {
    var seleccionada by remember { mutableStateOf(marcas.firstOrNull()?.id ?: "") }
    val seleccionMarca = marcas.firstOrNull { it.id == seleccionada } ?: marcas.first()
    var cantidad by remember { mutableStateOf(10) }
    var codigoLote by remember { mutableStateOf("L-2026-05-001") }
    var fechaCaducidad by remember { mutableStateOf("") }
    val caducidadValida = fechaCaducidad.length == 10
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Registrar entrada por lote", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Cada entrada se registra como un lote con su propia caducidad para llevar control PEPS.",
                    fontSize = 13.sp, color = HToGoColors.TextSecondary
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(HToGoColors.PrimarySoft)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Warehouse, null, tint = HToGoColors.Primary)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Destino: Base del negocio", fontSize = 13.sp, color = HToGoColors.PrimaryDark, fontWeight = FontWeight.SemiBold)
                        Text("38 / 50 ocupados · 12 espacios libres", fontSize = 11.sp, color = HToGoColors.TextSecondary)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("MARCA (CATÁLOGO)", fontSize = 11.sp, color = HToGoColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    marcas.forEach { m ->
                        val sel = m.id == seleccionada
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    1.dp,
                                    if (sel) HToGoColors.Primary else HToGoColors.OutlineSoft,
                                    RoundedCornerShape(10.dp)
                                )
                                .background(if (sel) HToGoColors.PrimaryWash else Color.White)
                                .clickable { seleccionada = m.id }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${m.nombre} · ${m.capacidad}", fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            if (sel) Icon(Icons.Filled.CheckCircle, null, tint = HToGoColors.Primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("DATOS DEL LOTE", fontSize = 11.sp, color = HToGoColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = codigoLote,
                    onValueChange = { codigoLote = it.take(20) },
                    label = { Text("Código de lote") },
                    leadingIcon = { Icon(Icons.Filled.QrCode2, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    supportingText = { Text("Identificador del proveedor (ej. L-2026-05-001)") }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = fechaCaducidad,
                    onValueChange = { raw ->
                        val digits = raw.filter(Char::isDigit).take(8)
                        fechaCaducidad = buildString {
                            digits.forEachIndexed { i, c ->
                                if (i == 2 || i == 4) append('/')
                                append(c)
                            }
                        }
                    },
                    label = { Text("Fecha de caducidad") },
                    leadingIcon = { Icon(Icons.Filled.CalendarMonth, null) },
                    placeholder = { Text("DD/MM/AAAA") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    isError = fechaCaducidad.isNotEmpty() && !caducidadValida,
                    supportingText = {
                        Text(
                            if (fechaCaducidad.isNotEmpty() && !caducidadValida)
                                "Formato: DD/MM/AAAA"
                            else
                                "Se registrará como un lote independiente"
                        )
                    }
                )
                if (seleccionMarca.lotes.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = HToGoColors.Background,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "LOTES ACTIVOS DE ${seleccionMarca.nombre.uppercase()}",
                                fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                                color = HToGoColors.TextSecondary
                            )
                            Spacer(Modifier.height(6.dp))
                            seleccionMarca.lotes.forEach { lote ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.HourglassBottom, null,
                                        tint = if (lote.diasParaCaducar <= 15) HToGoColors.AccentRose
                                        else if (lote.diasParaCaducar <= 45) HToGoColors.AccentAmber
                                        else HToGoColors.AccentEmerald,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(lote.codigo, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                    Text("${lote.cantidad} pzas", fontSize = 11.sp, color = HToGoColors.TextSecondary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(lote.fechaCaducidad, fontSize = 11.sp, color = HToGoColors.TextTertiary)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("CANTIDAD DEL LOTE", fontSize = 11.sp, color = HToGoColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                QtyStepper(cantidad, max = 12) { cantidad = it }
                Text(
                    "Máximo 12 (espacio disponible en base)",
                    fontSize = 11.sp, color = HToGoColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(23.dp)
                    ) { Text("Cancelar") }
                    Button(
                        onClick = onDismiss,
                        enabled = caducidadValida && codigoLote.isNotBlank() && cantidad > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(23.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary)
                    ) { Text("Registrar lote", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
private fun CargarVehiculoDialog(marcas: List<MarcaBaseStock>, onDismiss: () -> Unit) {
    var seleccionada by remember { mutableStateOf(marcas.firstOrNull()?.id ?: "") }
    val seleccionMarca = marcas.firstOrNull { it.id == seleccionada } ?: marcas.first()
    var cantidad by remember { mutableStateOf(3) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Cargar al vehículo", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Mueve garrafones de la base a tu vehículo. Se descontarán del stock compartido.",
                    fontSize = 13.sp, color = HToGoColors.TextSecondary
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(HToGoColors.PrimarySoft)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.SwapHoriz, null, tint = HToGoColors.Primary)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Base → Vehículo", fontSize = 13.sp, color = HToGoColors.PrimaryDark, fontWeight = FontWeight.SemiBold)
                        Text("Vehículo: 22 / 25 cap. · libre 3 espacios", fontSize = 11.sp, color = HToGoColors.TextSecondary)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("MARCA (DISPONIBLE EN BASE)", fontSize = 11.sp, color = HToGoColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    marcas.forEach { m ->
                        val sel = m.id == seleccionada
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    1.dp,
                                    if (sel) HToGoColors.Primary else HToGoColors.OutlineSoft,
                                    RoundedCornerShape(10.dp)
                                )
                                .background(if (sel) HToGoColors.PrimaryWash else Color.White)
                                .clickable {
                                    seleccionada = m.id
                                    cantidad = minOf(cantidad, m.enBase)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${m.nombre} · ${m.capacidad} — ${m.enBase} disp.",
                                fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f)
                            )
                            if (sel) Icon(Icons.Filled.CheckCircle, null, tint = HToGoColors.Primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("CANTIDAD A CARGAR", fontSize = 11.sp, color = HToGoColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                val maxCarga = minOf(seleccionMarca.enBase, 3)
                QtyStepper(cantidad, max = maxCarga) { cantidad = it }
                Text(
                    "Máx. $maxCarga — limitado por espacio en vehículo (de ${seleccionMarca.enBase} disponibles en base)",
                    fontSize = 11.sp, color = HToGoColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, HToGoColors.OutlineSoft),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("RESUMEN DEL TRASPASO", fontSize = 11.sp, color = HToGoColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                            Icon(Icons.Filled.Warehouse, null, tint = HToGoColors.TextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Base: ${seleccionMarca.enBase} → ", fontSize = 13.sp)
                            Text("${seleccionMarca.enBase - cantidad}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HToGoColors.AccentRose)
                            Text("  ·  ", fontSize = 13.sp, color = HToGoColors.TextTertiary)
                            Icon(Icons.Filled.LocalShipping, null, tint = HToGoColors.Primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Vehículo: 22 → ", fontSize = 13.sp)
                            Text("${22 + cantidad}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HToGoColors.AccentEmerald)
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(23.dp)
                    ) { Text("Cancelar") }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(23.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary)
                    ) { Text("Confirmar carga", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
private fun QtyStepper(value: Int, max: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { if (value > 0) onChange(value - 1) },
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(50))
                .background(HToGoColors.PrimarySoft)
        ) { Icon(Icons.Filled.Remove, null, tint = HToGoColors.Primary) }
        Text(
            "$value",
            fontSize = 26.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier
                .weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        IconButton(
            onClick = { if (value < max) onChange(value + 1) },
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(50))
                .background(HToGoColors.PrimarySoft)
        ) { Icon(Icons.Filled.Add, null, tint = HToGoColors.Primary) }
    }
}

@Composable
private fun SalidaManualDialog(marcas: List<MarcaBaseStock>, onDismiss: () -> Unit) {
    var seleccionada by remember { mutableStateOf(marcas.firstOrNull()?.id ?: "") }
    val seleccionMarca = marcas.firstOrNull { it.id == seleccionada } ?: marcas.first()
    var cantidad by remember { mutableStateOf(1) }
    var motivo by remember { mutableStateOf("Merma / rotura") }
    val motivos = listOf("Merma / rotura", "Entrega fuera de la app", "Devolución a proveedor", "Ajuste de inventario")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Registrar salida manual", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Resta garrafones de la base por una razón distinta a un pedido en la app.",
                    fontSize = 13.sp, color = HToGoColors.TextSecondary
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(HToGoColors.AccentRose.copy(alpha = .12f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, tint = HToGoColors.AccentRose)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Origen: Base del negocio", fontSize = 13.sp,
                            color = HToGoColors.AccentRose, fontWeight = FontWeight.SemiBold)
                        Text("Esta acción queda en bitácora", fontSize = 11.sp, color = HToGoColors.TextSecondary)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("MARCA", fontSize = 11.sp, color = HToGoColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    marcas.forEach { m ->
                        val sel = m.id == seleccionada
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    1.dp,
                                    if (sel) HToGoColors.Primary else HToGoColors.OutlineSoft,
                                    RoundedCornerShape(10.dp)
                                )
                                .background(if (sel) HToGoColors.PrimaryWash else Color.White)
                                .clickable {
                                    seleccionada = m.id
                                    cantidad = minOf(cantidad, m.enBase)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${m.nombre} · ${m.capacidad} — ${m.enBase} en base",
                                fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            if (sel) Icon(Icons.Filled.CheckCircle, null, tint = HToGoColors.Primary,
                                modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("CANTIDAD A RESTAR", fontSize = 11.sp, color = HToGoColors.TextSecondary,
                    fontWeight = FontWeight.SemiBold)
                QtyStepper(cantidad, max = seleccionMarca.enBase) { cantidad = it }
                Spacer(Modifier.height(14.dp))
                Text("MOTIVO", fontSize = 11.sp, color = HToGoColors.TextSecondary,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    motivos.forEach { m ->
                        val sel = m == motivo
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    1.dp,
                                    if (sel) HToGoColors.AccentRose else HToGoColors.OutlineSoft,
                                    RoundedCornerShape(10.dp)
                                )
                                .background(if (sel) HToGoColors.AccentRose.copy(alpha = .06f) else Color.White)
                                .clickable { motivo = m }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(m, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            if (sel) Icon(Icons.Filled.CheckCircle, null, tint = HToGoColors.AccentRose,
                                modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(23.dp)
                    ) { Text("Cancelar") }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(23.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.AccentRose)
                    ) { Text("Confirmar salida", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
private fun SolicitarCambioVehiculoDialog(onDismiss: () -> Unit) {
    var tipoNuevo by remember { mutableStateOf("Camioneta") }
    var placas by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("") }
    val tipos = listOf("Camioneta", "Pickup", "Motocicleta", "Bicicleta de carga")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Solicitar cambio de vehículo", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "El admin revisará tu solicitud y te avisará cuando se apruebe.",
                    fontSize = 13.sp, color = HToGoColors.TextSecondary
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(HToGoColors.PrimarySoft)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.SwapHoriz, null, tint = HToGoColors.Primary)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Vehículo actual: Camioneta · PJU-432-A",
                            fontSize = 13.sp, color = HToGoColors.PrimaryDark,
                            fontWeight = FontWeight.SemiBold)
                        Text("Capacidad 25 garrafones",
                            fontSize = 11.sp, color = HToGoColors.TextSecondary)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("NUEVO TIPO DE VEHÍCULO", fontSize = 11.sp, color = HToGoColors.TextSecondary,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tipos.forEach { t ->
                        val sel = t == tipoNuevo
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    1.dp,
                                    if (sel) HToGoColors.Primary else HToGoColors.OutlineSoft,
                                    RoundedCornerShape(10.dp)
                                )
                                .background(if (sel) HToGoColors.PrimaryWash else Color.White)
                                .clickable { tipoNuevo = t }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(t, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            if (sel) Icon(Icons.Filled.CheckCircle, null, tint = HToGoColors.Primary,
                                modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = placas,
                    onValueChange = { placas = it },
                    label = { Text("Placas del nuevo vehículo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo del cambio") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(23.dp)
                    ) { Text("Cancelar") }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(23.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary)
                    ) { Text("Enviar solicitud", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 868)
@Composable
fun InventarioVehiculoScreenPreview() {
    HToGoTheme { InventarioVehiculoScreen() }
}
