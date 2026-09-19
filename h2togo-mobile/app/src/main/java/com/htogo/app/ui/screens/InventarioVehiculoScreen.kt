package com.htogo.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.htogo.app.ui.RepartidorViewModel
import com.htogo.app.data.dto.CargaItemDto
import com.htogo.app.data.dto.LoteEntradaRequest

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
    onProductosPrecios: () -> Unit = {},
    repartidorViewModel: RepartidorViewModel = viewModel()
) {
    var tab by remember { mutableStateOf(TabInventario.EN_BASE) }
    var showRegistrarEntrada by remember { mutableStateOf(false) }
    var showCargarVehiculo by remember { mutableStateOf(false) }
    var showSalidaManual by remember { mutableStateOf(false) }
    var showSolicitarCambio by remember { mutableStateOf(false) }

    val liveBase by repartidorViewModel.inventarioBase.collectAsState()
    val liveVehiculo by repartidorViewModel.inventarioVehiculo.collectAsState()
    val marcasCatalogo by repartidorViewModel.marcas.collectAsState()
    val liveMiNegocio by repartidorViewModel.miNegocio.collectAsState()

    val marcasBase = remember(liveBase, marcasCatalogo, liveMiNegocio) {
        val lotes = liveBase?.lotes
        if (!lotes.isNullOrEmpty()) {
            lotes.groupBy { it.idMarca }.map { (idMarca, items) ->
                val primer = items.first()
                val totalBase = items.sumOf { it.cantidadActual }
                MarcaBaseStock(
                    id = idMarca.toString(),
                    codigo = primer.marca.take(3).uppercase(),
                    nombre = primer.marca,
                    capacidad = "20 L",
                    precio = 45.0,
                    proveedor = "Proveedor oficial",
                    enBase = totalBase,
                    maximoBase = 50,
                    accent = when (idMarca % 3) {
                        0 -> HToGoColors.Primary
                        1 -> HToGoColors.AccentEmerald
                        else -> HToGoColors.AccentAmber
                    },
                    lotes = items.map { item ->
                        Lote(
                            codigo = "L-${item.idLote}",
                            cantidad = item.cantidadActual,
                            fechaCaducidad = item.fechaCaducidad,
                            diasParaCaducar = 90
                        )
                    }
                )
            }
        } else if (!liveMiNegocio?.productos.isNullOrEmpty()) {
            liveMiNegocio!!.productos!!.mapIndexed { idx, p ->
                MarcaBaseStock(
                    id = p.idMarca.toString(),
                    codigo = p.marca.take(3).uppercase(),
                    nombre = p.marca,
                    capacidad = "20 L",
                    precio = p.precio,
                    proveedor = "Proveedor oficial",
                    enBase = p.stockDisponible.toInt(),
                    maximoBase = p.capacidadMaxima,
                    accent = if (idx % 2 == 0) HToGoColors.Primary else HToGoColors.AccentEmerald
                )
            }
        } else if (marcasCatalogo.isNotEmpty()) {
            marcasCatalogo.mapIndexed { idx, m ->
                MarcaBaseStock(
                    id = m.id.toString(),
                    codigo = m.nombre.take(3).uppercase(),
                    nombre = m.nombre,
                    capacidad = "20 L",
                    precio = 45.0,
                    proveedor = "Proveedor base",
                    enBase = 0,
                    maximoBase = 50,
                    accent = if (idx % 2 == 0) HToGoColors.Primary else HToGoColors.AccentEmerald
                )
            }
        } else {
            emptyList()
        }
    }

    val marcasVehiculo = remember(liveVehiculo) {
        val lotes = liveVehiculo?.lotes
        if (!lotes.isNullOrEmpty()) {
            lotes.groupBy { it.idMarca }.map { (idMarca, items) ->
                val primer = items.first()
                val totalCargados = items.sumOf { it.cantidadActual }
                val totalApartados = items.sumOf { it.cantidadApartada ?: 0 }
                MarcaVehiculoStock(
                    id = idMarca.toString(),
                    codigo = primer.marca.take(3).uppercase(),
                    nombre = primer.marca,
                    capacidad = "20 L",
                    precio = 45.0,
                    cargados = totalCargados,
                    disponibles = maxOf(0, totalCargados - totalApartados),
                    apartados = totalApartados,
                    accent = when (idMarca % 3) {
                        0 -> HToGoColors.Primary
                        1 -> HToGoColors.AccentEmerald
                        else -> HToGoColors.AccentAmber
                    }
                )
            }
        } else {
            emptyList()
        }
    }

    val vehiculoActual = liveMiNegocio?.vehiculoPrincipal
    val capacidadVehiculo = vehiculoActual?.capacidadGarrafones ?: 30
    val totalBaseStock = remember(marcasBase) { marcasBase.sumOf { it.enBase } }
    val totalVehiculoStock = remember(marcasVehiculo) { marcasVehiculo.sumOf { it.cargados } }
    val totalApartadosVehiculo = remember(marcasVehiculo) { marcasVehiculo.sumOf { it.apartados } }
    val totalDisponiblesVehiculo = remember(marcasVehiculo) { marcasVehiculo.sumOf { it.disponibles } }

    Scaffold(
        containerColor = HToGoColors.Background,
        topBar = {
            Column(
                Modifier
                    .background(Brush.linearGradient(listOf(HToGoColors.PrimaryDark, HToGoColors.Primary)))
                    .padding(horizontal = 8.dp)
            ) {
                TopAppBar(
                    title = {
                        val nom = liveMiNegocio?.nombreComercial ?: ""
                        Text(if (nom.isNotBlank()) "Mi negocio · $nom" else "Mi negocio", color = Color.White, fontWeight = FontWeight.SemiBold)
                    },
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
                    item { ResumenBaseCard(totalGarrafones = totalBaseStock, numMarcas = marcasBase.size) }
                    item { ProductosPreciosShortcut(onProductosPrecios) }
                    item { SectionTitle("Marcas en base · ${marcasBase.size}") }
                    if (marcasBase.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, HToGoColors.OutlineSoft)
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        Modifier.size(52.dp).clip(CircleShape).background(HToGoColors.PrimarySoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Warehouse, null, tint = HToGoColors.Primary, modifier = Modifier.size(26.dp))
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text("Sin marcas registradas", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = HToGoColors.TextPrimary)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Entra a 'Productos y precios' para configurar el catálogo de tu negocio.",
                                        fontSize = 12.sp,
                                        color = HToGoColors.TextSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(marcasBase) { m -> MarcaBaseCard(m) }
                    }
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
                    item {
                        ResumenVehiculoCard(
                            cargados = totalVehiculoStock,
                            disponibles = totalDisponiblesVehiculo,
                            apartados = totalApartadosVehiculo,
                            capacidad = capacidadVehiculo,
                            numMarcas = marcasVehiculo.size
                        )
                    }
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
                    if (marcasVehiculo.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, HToGoColors.OutlineSoft)
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        Modifier.size(52.dp).clip(CircleShape).background(HToGoColors.PrimarySoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.LocalShipping, null, tint = HToGoColors.Primary, modifier = Modifier.size(26.dp))
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text("Vehículo sin garrafones", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = HToGoColors.TextPrimary)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Carga garrafones desde la base para salir a ruta y repartir pedidos.",
                                        fontSize = 12.sp,
                                        color = HToGoColors.TextSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        item { SectionTitle("Carga actual · ${marcasVehiculo.size} marcas") }
                        items(marcasVehiculo) { m -> MarcaVehiculoCard(m) }
                    }
                }
                TabInventario.VEHICULO -> {
                    item {
                        VehiculoDetalleCard(
                            vehiculo = vehiculoActual,
                            onSolicitarCambio = { showSolicitarCambio = true }
                        )
                    }
                }
            }
        }
    }

    if (showRegistrarEntrada) {
        RegistrarEntradaDialog(
            marcas = marcasBase,
            onDismiss = { showRegistrarEntrada = false },
            onRegistrar = { idMarca, cant, fecha ->
                repartidorViewModel.registrarLote(
                    request = LoteEntradaRequest(
                        idMarca = idMarca,
                        cantidad = cant,
                        fechaCaducidad = fecha,
                        proveedor = "Proveedor oficial"
                    ),
                    onSuccess = { showRegistrarEntrada = false },
                    onError = { showRegistrarEntrada = false }
                )
            }
        )
    }
    if (showCargarVehiculo) {
        CargarVehiculoDialog(
            marcas = marcasBase,
            onDismiss = { showCargarVehiculo = false },
            onCargar = { idMarca, cant ->
                repartidorViewModel.cargarVehiculo(
                    cargas = listOf(CargaItemDto(idMarca = idMarca, cantidad = cant)),
                    onSuccess = { showCargarVehiculo = false },
                    onError = { showCargarVehiculo = false }
                )
            }
        )
    }
    if (showSalidaManual) {
        SalidaManualDialog(
            marcas = marcasBase,
            onDismiss = { showSalidaManual = false },
            onSalida = {
                repartidorViewModel.devolverABase(
                    onSuccess = { showSalidaManual = false },
                    onError = { showSalidaManual = false }
                )
            }
        )
    }
    if (showSolicitarCambio) {
        SolicitarCambioVehiculoDialog(
            vehiculo = vehiculoActual,
            onDismiss = { showSolicitarCambio = false },
            onGuardar = { req ->
                repartidorViewModel.actualizarVehiculo(
                    idVehiculo = vehiculoActual?.id,
                    request = req,
                    onSuccess = { showSolicitarCambio = false },
                    onError = { showSolicitarCambio = false }
                )
            }
        )
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
private fun ResumenBaseCard(totalGarrafones: Int = 38, capacidad: Int = 50, numMarcas: Int = 3) {
    val pct = if (capacidad > 0) "${(totalGarrafones * 100) / capacidad}%" else "0%"
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
                        Text("$totalGarrafones garrafones", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text("$numMarcas marcas · capacidad $capacidad", color = Color.White.copy(alpha = .85f), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = .18f))
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    HeroStat(pct, "Capacidad ocupada")
                    HeroStat("$totalGarrafones", "Disponibles")
                    HeroStat("${maxOf(0, capacidad - totalGarrafones)}", "Espacios libres")
                }
            }
        }
    }
}

@Composable
private fun ResumenVehiculoCard(
    cargados: Int,
    disponibles: Int,
    apartados: Int,
    capacidad: Int,
    numMarcas: Int
) {
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
                        Text("$cargados garrafones", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text("$numMarcas marcas cargadas · $capacidad cap. máx.", color = Color.White.copy(alpha = .85f), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = .18f))
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    HeroStat("$disponibles", "Disponibles")
                    HeroStat("$apartados", "Apartados")
                    val libres = maxOf(0, capacidad - cargados)
                    HeroStat("$libres", "Espacio libre")
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
private fun VehiculoDetalleCard(
    vehiculo: com.htogo.app.data.dto.VehiculoDto?,
    onSolicitarCambio: () -> Unit
) {
    val tipoNormalizado = (vehiculo?.tipoVehiculo ?: "motocicleta").lowercase()
    val tipoDisplay = when (tipoNormalizado) {
        "motocicleta" -> "Motocicleta"
        "automovil" -> "Automóvil"
        "camioneta" -> "Camioneta"
        "bicicleta" -> "Bicicleta de carga"
        else -> vehiculo?.tipoVehiculo?.replaceFirstChar { it.uppercase() } ?: "Vehículo de reparto"
    }
    val iconVehiculo = when (tipoNormalizado) {
        "camioneta" -> Icons.Filled.LocalShipping
        else -> Icons.Filled.DirectionsCar
    }
    val marcaDisplay = vehiculo?.marca?.takeIf { it.isNotBlank() } ?: "Italika"
    val modeloDisplay = vehiculo?.modelo?.takeIf { it.isNotBlank() } ?: "FT150"
    val placasDisplay = vehiculo?.placas?.takeIf { it.isNotBlank() } ?: "ABC1234"
    val colorDisplay = vehiculo?.color?.takeIf { it.isNotBlank() } ?: "Rojo"
    val capacidadNum = vehiculo?.capacidadGarrafones ?: 30
    val kgTotal = capacidadNum * 20

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
                Icon(iconVehiculo, null, tint = Color.White.copy(alpha = .4f), modifier = Modifier.size(80.dp))
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
                        Text(
                            if (vehiculo?.activo != false) "Verificado" else "Inactivo",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$marcaDisplay · $modeloDisplay",
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
                    VField("Tipo", tipoDisplay, Modifier.weight(1f), iconVehiculo)
                    VField("Modelo", modeloDisplay, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                VField("Placas", "$placasDisplay · CDMX", Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VField("Color", colorDisplay, Modifier.weight(1f))
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
                            "$capacidadNum garrafones",
                            fontSize = 18.sp, color = HToGoColors.PrimaryDark, fontWeight = FontWeight.Bold
                        )
                        Text(
                            "de 20 L cada uno · ~$kgTotal kg total",
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
private fun RegistrarEntradaDialog(
    marcas: List<MarcaBaseStock>,
    onDismiss: () -> Unit,
    onRegistrar: (idMarca: Int, cantidad: Int, fechaCaducidad: String) -> Unit = { _, _, _ -> }
) {
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
                        onClick = {
                            val parts = fechaCaducidad.split("/")
                            val fechaIso = if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else "2026-12-31"
                            onRegistrar(seleccionMarca.id.toIntOrNull() ?: 1, cantidad, fechaIso)
                        },
                        enabled = caducidadValida && cantidad > 0,
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
private fun CargarVehiculoDialog(
    marcas: List<MarcaBaseStock>,
    onDismiss: () -> Unit,
    onCargar: (idMarca: Int, cantidad: Int) -> Unit = { _, _ -> }
) {
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
                        onClick = {
                            onCargar(seleccionMarca.id.toIntOrNull() ?: 1, cantidad)
                        },
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
private fun SalidaManualDialog(
    marcas: List<MarcaBaseStock>,
    onDismiss: () -> Unit,
    onSalida: () -> Unit = {}
) {
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
                        onClick = { onSalida() },
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
private fun SolicitarCambioVehiculoDialog(
    vehiculo: com.htogo.app.data.dto.VehiculoDto?,
    onDismiss: () -> Unit,
    onGuardar: (com.htogo.app.data.dto.ActualizarVehiculoRequest) -> Unit
) {
    val tipoInicial = when (vehiculo?.tipoVehiculo?.lowercase()) {
        "camioneta" -> "Camioneta"
        "automovil" -> "Automóvil"
        "bicicleta" -> "Bicicleta de carga"
        else -> "Motocicleta"
    }
    var tipoNuevo by remember { mutableStateOf(tipoInicial) }
    var marca by remember { mutableStateOf(vehiculo?.marca ?: "Italika") }
    var modelo by remember { mutableStateOf(vehiculo?.modelo ?: "FT150") }
    var placas by remember { mutableStateOf(vehiculo?.placas ?: "ABC1234") }
    var color by remember { mutableStateOf(vehiculo?.color ?: "Rojo") }
    var capacidad by remember { mutableStateOf((vehiculo?.capacidadGarrafones ?: 30).toString()) }
    var motivo by remember { mutableStateOf("") }
    val tipos = listOf("Motocicleta", "Camioneta", "Automóvil", "Bicicleta de carga")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Solicitar cambio de vehículo", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Actualiza los datos del vehículo de tu negocio.",
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
                        Text(
                            "Vehículo actual: ${vehiculo?.marca ?: "Italika"} ${vehiculo?.modelo ?: "FT150"} · ${vehiculo?.placas ?: "ABC1234"}",
                            fontSize = 13.sp, color = HToGoColors.PrimaryDark,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Capacidad: ${vehiculo?.capacidadGarrafones ?: 30} garrafones",
                            fontSize = 11.sp, color = HToGoColors.TextSecondary
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("TIPO DE VEHÍCULO", fontSize = 11.sp, color = HToGoColors.TextSecondary,
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
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = marca,
                        onValueChange = { marca = it },
                        label = { Text("Marca") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = modelo,
                        onValueChange = { modelo = it },
                        label = { Text("Modelo") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = placas,
                        onValueChange = { placas = it },
                        label = { Text("Placas") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = color,
                        onValueChange = { color = it },
                        label = { Text("Color") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = capacidad,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) capacidad = it },
                    label = { Text("Capacidad máxima (garrafones)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo / Notas del cambio (opcional)") },
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
                        onClick = {
                            val tipoEnumStr = when (tipoNuevo) {
                                "Motocicleta" -> "motocicleta"
                                "Camioneta" -> "camioneta"
                                "Automóvil" -> "automovil"
                                "Bicicleta de carga" -> "bicicleta"
                                else -> "motocicleta"
                            }
                            onGuardar(
                                com.htogo.app.data.dto.ActualizarVehiculoRequest(
                                    tipoVehiculo = tipoEnumStr,
                                    marca = marca.trim(),
                                    modelo = modelo.trim(),
                                    color = color.trim(),
                                    placas = placas.trim(),
                                    capacidadGarrafones = capacidad.toIntOrNull() ?: 30
                                )
                            )
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(23.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary)
                    ) { Text("Guardar cambios", fontWeight = FontWeight.SemiBold) }
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
