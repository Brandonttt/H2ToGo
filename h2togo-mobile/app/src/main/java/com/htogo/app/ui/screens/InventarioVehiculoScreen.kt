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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Schedule
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
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

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
    var showEditarVehiculoDialog by remember { mutableStateOf(false) }
    var vehiculoParaEditar by remember { mutableStateOf<com.htogo.app.data.dto.VehiculoDto?>(null) }
    var vehiculoParaEliminar by remember { mutableStateOf<com.htogo.app.data.dto.VehiculoDto?>(null) }
    var showIniciarJornadaDialog by remember { mutableStateOf(false) }
    var vehiculoParaJornada by remember { mutableStateOf<com.htogo.app.data.dto.VehiculoDto?>(null) }
    val context = LocalContext.current

    val liveBase by repartidorViewModel.inventarioBase.collectAsState()
    val liveVehiculo by repartidorViewModel.inventarioVehiculo.collectAsState()
    val liveMiNegocio by repartidorViewModel.miNegocio.collectAsState()
    val solicitudes by repartidorViewModel.solicitudes.collectAsState()

    LaunchedEffect(Unit) {
        repartidorViewModel.cargarSolicitudes()
        repartidorViewModel.cargarMiNegocio()
    }

    val solicitudesVehiculo = remember(solicitudes) {
        solicitudes.filter {
            it.estado.equals("pendiente", ignoreCase = true) &&
            (it.codigoCambio == "AGREGAR_VEHICULO" || it.codigoCambio == "DATOS_VEHICULO" || it.codigoCambio == "ELIMINAR_VEHICULO")
        }
    }

    val marcasBase = remember(liveBase, liveMiNegocio) {
        val productos = liveMiNegocio?.productos?.filter { it.activo }
        if (!productos.isNullOrEmpty()) {
            productos.mapIndexed { idx, p ->
                val lotes = liveBase?.lotes.orEmpty().filter { it.idMarca == p.idMarca && it.cantidadActual > 0 }
                val totalEnLotes = lotes.sumOf { it.cantidadActual }
                val totalBase = if (lotes.isNotEmpty()) totalEnLotes else p.stockDisponible.toInt()
                MarcaBaseStock(
                    id = p.idMarca.toString(),
                    codigo = p.marca.take(3).uppercase(),
                    nombre = p.marca,
                    capacidad = "20 L",
                    precio = p.precio,
                    proveedor = "Proveedor oficial",
                    enBase = totalBase,
                    maximoBase = p.capacidadMaxima,
                    accent = if (idx % 2 == 0) HToGoColors.Primary else HToGoColors.AccentEmerald,
                    lotes = lotes.sortedBy { it.fechaCaducidad }.map { item ->
                        Lote(
                            codigo = "L-${item.idLote}",
                            cantidad = item.cantidadActual,
                            fechaCaducidad = item.fechaCaducidad,
                            diasParaCaducar = diasHasta(item.fechaCaducidad)
                        )
                    }
                )
            }
        } else {
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
            } else {
                emptyList()
            }
        }
    }

    // Para registrar lotes: solo los productos activos del negocio. Si el negocio es nuevo
    // y no tiene productos registrados en Productos y precios, la lista queda vacía.
    val opcionesLote = remember(liveMiNegocio, liveBase) {
        val productos = liveMiNegocio?.productos?.filter { it.activo }
        if (productos.isNullOrEmpty()) {
            emptyList()
        } else {
            productos.mapIndexed { idx, p ->
                val lotes = liveBase?.lotes.orEmpty().filter { it.idMarca == p.idMarca && it.cantidadActual > 0 }
                MarcaBaseStock(
                    id = p.idMarca.toString(),
                    codigo = p.marca.take(3).uppercase(),
                    nombre = p.marca,
                    capacidad = "20 L",
                    precio = p.precio,
                    proveedor = "",
                    enBase = lotes.sumOf { it.cantidadActual },
                    maximoBase = p.capacidadMaxima,
                    accent = if (idx % 2 == 0) HToGoColors.Primary else HToGoColors.AccentEmerald,
                    lotes = lotes.sortedBy { it.fechaCaducidad }.map { item ->
                        Lote(
                            codigo = "L-${item.idLote}",
                            cantidad = item.cantidadActual,
                            fechaCaducidad = item.fechaCaducidad,
                            diasParaCaducar = diasHasta(item.fechaCaducidad)
                        )
                    }
                )
            }
        }
    }
    var registrandoLote by remember { mutableStateOf(false) }

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
    val totalCapacidadBase = remember(marcasBase) { marcasBase.sumOf { it.maximoBase } }
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
                    item { ResumenBaseCard(totalGarrafones = totalBaseStock, capacidad = totalCapacidadBase, numMarcas = marcasBase.size) }
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
                    val vehiculos = liveMiNegocio?.vehiculos.orEmpty()
                    val listaAMostrar = if (vehiculos.isNotEmpty()) vehiculos else listOfNotNull(vehiculoActual)

                    // Solicitudes de vehículo pendientes de aprobación del admin (RN-019 / CU-009)
                    if (solicitudesVehiculo.isNotEmpty()) {
                        item {
                            Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp)) {
                                Text(
                                    "Solicitudes pendientes de aprobación (${solicitudesVehiculo.size})",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HToGoColors.PrimaryDark
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Toda alta, modificación o baja requiere aprobación del administrador (RN-019).",
                                    fontSize = 12.sp,
                                    color = HToGoColors.TextSecondary
                                )
                            }
                        }
                        items(solicitudesVehiculo) { sol ->
                            SolicitudVehiculoCard(
                                solicitud = sol,
                                onCancelar = {
                                    repartidorViewModel.cancelarSolicitud(
                                        id = sol.id,
                                        onSuccess = {
                                            Toast.makeText(context, "Solicitud cancelada", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { err ->
                                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            )
                        }
                        item {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 6.dp),
                                color = HToGoColors.OutlineSoft
                            )
                        }
                    }

                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Vehículos registrados",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HToGoColors.TextPrimary
                                )
                                Text(
                                    "${listaAMostrar.size} vehículo(s) en tu negocio · RN-014",
                                    fontSize = 12.sp,
                                    color = HToGoColors.TextSecondary
                                )
                            }
                            Button(
                                onClick = {
                                    vehiculoParaEditar = null
                                    showEditarVehiculoDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Agregar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (listaAMostrar.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, HToGoColors.OutlineSoft)
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        Modifier.size(56.dp).clip(CircleShape).background(HToGoColors.PrimarySoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.DirectionsCar, null, tint = HToGoColors.Primary, modifier = Modifier.size(28.dp))
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Text("No tienes vehículos registrados", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = HToGoColors.TextPrimary)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "Para iniciar jornada y recibir pedidos necesitas tener al menos un vehículo registrado (RN-014).",
                                        fontSize = 12.sp,
                                        color = HToGoColors.TextSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            vehiculoParaEditar = null
                                            showEditarVehiculoDialog = true
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary)
                                    ) {
                                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Registrar primer vehículo", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    } else {
                        items(listaAMostrar) { v ->
                            val esActivo = liveVehiculo?.idVehiculo != null && liveVehiculo?.idVehiculo == v.id
                            val solPendiente = solicitudesVehiculo.firstOrNull { it.idVehiculo == v.id }
                            VehiculoDetalleCard(
                                vehiculo = v,
                                esJornadaActiva = esActivo,
                                solicitudPendiente = solPendiente,
                                onIniciarJornada = {
                                    vehiculoParaJornada = v
                                    showIniciarJornadaDialog = true
                                },
                                onSolicitarCambio = {
                                    vehiculoParaEditar = v
                                    showEditarVehiculoDialog = true
                                },
                                onEliminar = {
                                    vehiculoParaEliminar = v
                                }
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }

    if (showRegistrarEntrada) {
        RegistrarEntradaDialog(
            marcas = opcionesLote,
            registrando = registrandoLote,
            onDismiss = { showRegistrarEntrada = false },
            onRegistrar = { idMarca, cant, fecha, codigo ->
                registrandoLote = true
                repartidorViewModel.registrarLote(
                    request = LoteEntradaRequest(
                        idMarca = idMarca,
                        cantidad = cant,
                        fechaCaducidad = fecha,
                        // El backend no tiene columna de código: se conserva en las notas del movimiento.
                        notas = codigo.takeIf { it.isNotBlank() }?.let { "Código de lote del proveedor: $it" }
                    ),
                    onSuccess = {
                        registrandoLote = false
                        showRegistrarEntrada = false
                        Toast.makeText(context, "Lote registrado", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        // Se queda abierto para corregir (p. ej. caducidad o capacidad).
                        registrandoLote = false
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }
    if (showCargarVehiculo) {
        CargarVehiculoDialog(
            marcas = marcasBase,
            capacidadVehiculo = capacidadVehiculo,
            ocupadoVehiculo = totalVehiculoStock,
            onDismiss = { showCargarVehiculo = false },
            onCargar = { idMarca, cant ->
                repartidorViewModel.cargarVehiculo(
                    cargas = listOf(CargaItemDto(idMarca = idMarca, cantidad = cant)),
                    onSuccess = {
                        Toast.makeText(context, "Se cargaron $cant garrafones al vehículo exitosamente", Toast.LENGTH_SHORT).show()
                        showCargarVehiculo = false
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }
    if (showSalidaManual) {
        SalidaManualDialog(
            marcas = marcasBase,
            onDismiss = { showSalidaManual = false },
            onSalida = { idMarca, cantidad, motivo ->
                repartidorViewModel.registrarSalidaManual(
                    idMarca = idMarca,
                    cantidad = cantidad,
                    motivo = motivo,
                    onSuccess = {
                        showSalidaManual = false
                        Toast.makeText(
                            context,
                            "Salida manual registrada: -$cantidad garrafones ($motivo)",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }
    if (showEditarVehiculoDialog) {
        SolicitarCambioVehiculoDialog(
            vehiculo = vehiculoParaEditar,
            onDismiss = {
                showEditarVehiculoDialog = false
                vehiculoParaEditar = null
            },
            onGuardar = { req ->
                val vEditar = vehiculoParaEditar
                if (vEditar == null) {
                    repartidorViewModel.solicitarAgregarVehiculo(
                        request = req,
                        onSuccess = {
                            Toast.makeText(
                                context,
                                "Solicitud de alta enviada al Administrador para su aprobación (RN-019).",
                                Toast.LENGTH_LONG
                            ).show()
                            showEditarVehiculoDialog = false
                            vehiculoParaEditar = null
                        },
                        onError = { err ->
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    val idVeh = vEditar.id
                    if (idVeh != null) {
                        repartidorViewModel.solicitarModificarVehiculo(
                            idVehiculo = idVeh,
                            request = req,
                            onSuccess = {
                                Toast.makeText(
                                    context,
                                    "Solicitud enviada al Administrador. El vehículo conserva sus datos actuales hasta la aprobación (RN-019).",
                                    Toast.LENGTH_LONG
                                ).show()
                                showEditarVehiculoDialog = false
                                vehiculoParaEditar = null
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            }
        )
    }

    if (vehiculoParaEliminar != null) {
        val vElim = vehiculoParaEliminar!!
        AlertDialog(
            onDismissRequest = { vehiculoParaEliminar = null },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White,
            icon = {
                Icon(Icons.Filled.Warning, null, tint = HToGoColors.AccentRose, modifier = Modifier.size(32.dp))
            },
            title = {
                Text("¿Solicitar baja de vehículo?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = HToGoColors.TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Se enviará una solicitud al Administrador para dar de baja el siguiente vehículo:",
                        fontSize = 13.sp,
                        color = HToGoColors.TextSecondary
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = HToGoColors.PrimaryWash,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "${vElim.marca ?: ""} ${vElim.modelo ?: ""} · ${vElim.placas ?: ""}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = HToGoColors.PrimaryDark
                            )
                            Text(
                                "Capacidad: ${vElim.capacidadGarrafones ?: 30} garrafones",
                                fontSize = 12.sp,
                                color = HToGoColors.TextSecondary
                            )
                        }
                    }
                    Text(
                        "De acuerdo con RN-019 y CU-009, el vehículo seguirá funcionando con normalidad hasta que el Administrador apruebe la solicitud.",
                        fontSize = 12.sp,
                        color = HToGoColors.TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idVeh = vElim.id
                        vehiculoParaEliminar = null
                        if (idVeh != null) {
                            repartidorViewModel.solicitarEliminarVehiculo(
                                idVehiculo = idVeh,
                                onSuccess = {
                                    Toast.makeText(
                                        context,
                                        "Solicitud de baja enviada al Administrador para su aprobación.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.AccentRose),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Confirmar baja", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { vehiculoParaEliminar = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showIniciarJornadaDialog && vehiculoParaJornada != null) {
        val vehiculoSel = vehiculoParaJornada!!
        IniciarJornadaDialog(
            vehiculo = vehiculoSel,
            marcasBase = marcasBase,
            onDismiss = {
                showIniciarJornadaDialog = false
                vehiculoParaJornada = null
            },
            onIniciar = { idMarca, cant ->
                val idVeh = vehiculoSel.id
                if (idVeh != null) {
                    repartidorViewModel.iniciarJornada(
                        idVehiculo = idVeh,
                        onSuccess = {
                            if (cant > 0 && idMarca != null) {
                                repartidorViewModel.cargarVehiculo(
                                    cargas = listOf(CargaItemDto(idMarca = idMarca, cantidad = cant)),
                                    onSuccess = {
                                        Toast.makeText(context, "Jornada iniciada con $cant garrafones cargados", Toast.LENGTH_SHORT).show()
                                        showIniciarJornadaDialog = false
                                        vehiculoParaJornada = null
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, "Jornada iniciada pero error al cargar inventario: $err", Toast.LENGTH_LONG).show()
                                        showIniciarJornadaDialog = false
                                        vehiculoParaJornada = null
                                    }
                                )
                            } else {
                                Toast.makeText(context, "Jornada iniciada sin carga inicial", Toast.LENGTH_SHORT).show()
                                showIniciarJornadaDialog = false
                                vehiculoParaJornada = null
                            }
                        },
                        onError = { err ->
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                    )
                }
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
private fun SolicitudVehiculoCard(
    solicitud: com.htogo.app.data.dto.SolicitudResponse,
    onCancelar: () -> Unit
) {
    val titulo = when (solicitud.codigoCambio) {
        "AGREGAR_VEHICULO" -> "Solicitud: Alta de nuevo vehículo"
        "DATOS_VEHICULO" -> "Solicitud: Modificación de vehículo"
        "ELIMINAR_VEHICULO" -> "Solicitud: Baja de vehículo"
        else -> "Solicitud: Vehículo"
    }
    val detalle = remember(solicitud.valorNuevo) {
        try {
            val json = org.json.JSONObject(solicitud.valorNuevo)
            val marca = json.optString("marca", "")
            val modelo = json.optString("modelo", "")
            val placas = json.optString("placas", "")
            val cap = json.optInt("capacidadGarrafones", 0)
            buildString {
                if (marca.isNotBlank() || modelo.isNotBlank()) append("$marca $modelo".trim())
                if (placas.isNotBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append("Placas: $placas")
                }
                if (cap > 0) {
                    if (isNotEmpty()) append(" · ")
                    append("Capacidad: $cap garrafones")
                }
            }
        } catch (_: Exception) {
            ""
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, HToGoColors.AccentAmber.copy(alpha = 0.5f))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = HToGoColors.AccentAmber.copy(alpha = 0.15f)
                ) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFB45309)))
                        Spacer(Modifier.width(6.dp))
                        Text("Pendiente de aprobación", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                    }
                }
                Text("RN-019", fontSize = 11.sp, color = HToGoColors.TextSecondary, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(8.dp))
            Text(titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HToGoColors.TextPrimary)
            if (detalle.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(detalle, fontSize = 12.5.sp, color = HToGoColors.TextSecondary)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Enviada: ${solicitud.fechaSolicitud.take(16).replace('T', ' ')}",
                fontSize = 11.sp,
                color = HToGoColors.TextSecondary
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                    onClick = onCancelar,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.Close, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Cancelar solicitud", fontSize = 11.5.sp)
                }
            }
        }
    }
}

@Composable
private fun VehiculoDetalleCard(
    vehiculo: com.htogo.app.data.dto.VehiculoDto?,
    esJornadaActiva: Boolean = false,
    solicitudPendiente: com.htogo.app.data.dto.SolicitudResponse? = null,
    onIniciarJornada: () -> Unit = {},
    onSolicitarCambio: () -> Unit,
    onEliminar: () -> Unit = {}
) {
    val tipoNormalizado = (vehiculo?.tipoVehiculo ?: "motocicleta").lowercase()
    val tipoDisplay = when (tipoNormalizado) {
        "motocicleta" -> "Motocicleta"
        "automovil" -> "Automóvil"
        "camioneta" -> "Camioneta"
        "bicicleta", "bicicleta_carga" -> "Bicicleta de carga"
        "triciclo_carga" -> "Triciclo de carga"
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
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = HToGoColors.AccentRose.copy(alpha = 0.12f),
                        modifier = Modifier.clickable(onClick = onEliminar)
                    ) {
                        Icon(
                            Icons.Filled.DeleteOutline,
                            contentDescription = "Dar de baja",
                            tint = HToGoColors.AccentRose,
                            modifier = Modifier.padding(6.dp).size(18.dp)
                        )
                    }
                }
                if (solicitudPendiente != null) {
                    Spacer(Modifier.height(10.dp))
                    val esBaja = solicitudPendiente.codigoCambio == "ELIMINAR_VEHICULO"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (esBaja) HToGoColors.AccentRose.copy(alpha = 0.12f) else HToGoColors.AccentAmber.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (esBaja) HToGoColors.AccentRose.copy(alpha = 0.4f) else HToGoColors.AccentAmber.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (esBaja) Icons.Filled.Delete else Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = if (esBaja) HToGoColors.AccentRose else Color(0xFFB45309),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (esBaja) "Solicitud de baja en revisión por el Administrador. Sigue activo hasta su resolución (CU-009, RN-019)."
                                else "Solicitud de cambio en revisión por el Administrador. Conserva sus datos actuales hasta la aprobación (CU-009, RN-019).",
                                fontSize = 11.5.sp,
                                color = if (esBaja) HToGoColors.AccentRose else Color(0xFFB45309),
                                fontWeight = FontWeight.Medium
                            )
                        }
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
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onIniciarJornada,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (esJornadaActiva) HToGoColors.AccentEmerald else HToGoColors.Primary
                    )
                ) {
                    Icon(
                        if (esJornadaActiva) Icons.Filled.CheckCircle else Icons.Filled.DirectionsCar,
                        null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (esJornadaActiva) "Jornada activa con este vehículo" else "Iniciar jornada con este vehículo",
                        fontWeight = FontWeight.SemiBold
                    )
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

/** Días desde hoy hasta una fecha ISO (yyyy-MM-dd); 0 si no se puede leer. */
private fun diasHasta(fechaIso: String): Int = try {
    val fecha = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(fechaIso.take(10))
    if (fecha == null) 0 else ((fecha.time - System.currentTimeMillis()) / 86_400_000L).toInt()
} catch (e: Exception) {
    0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegistrarEntradaDialog(
    marcas: List<MarcaBaseStock>,
    registrando: Boolean,
    onDismiss: () -> Unit,
    onRegistrar: (idMarca: Int, cantidad: Int, fechaCaducidadIso: String, codigoLote: String) -> Unit
) {
    var seleccionada by remember { mutableStateOf(marcas.firstOrNull()?.id) }
    var menuAbierto by remember { mutableStateOf(false) }
    val seleccionMarca = marcas.firstOrNull { it.id == seleccionada }
    val libres = seleccionMarca?.let { maxOf(0, it.maximoBase - it.enBase) } ?: 0
    var cantidad by remember { mutableStateOf(1) }
    LaunchedEffect(seleccionada, libres) { cantidad = cantidad.coerceIn(minOf(1, libres), maxOf(libres, 0)) }
    var codigoLote by remember { mutableStateOf("") }
    var fechaCaducidadValue by remember {
        mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(""))
    }
    val fechaCaducidad = fechaCaducidadValue.text
    // RN-030: la caducidad debe ser posterior a hoy + 7 días (el backend también lo valida).
    val fechaIso = fechaCaducidad.split("/").takeIf { it.size == 3 && fechaCaducidad.length == 10 }
        ?.let { (d, m, y) -> "$y-$m-$d" }
    val diasCaducidad = fechaIso?.let(::diasHasta)
    val caducidadValida = diasCaducidad != null && diasCaducidad > 7
    val context = androidx.compose.ui.platform.LocalContext.current

    val motivoDeshabilitado = when {
        seleccionMarca == null -> "Elige una marca del catálogo"
        libres <= 0 -> "La base ya está llena para esta marca"
        fechaIso == null -> "Indica la fecha de caducidad"
        !caducidadValida -> "La caducidad debe ser de más de 7 días"
        else -> null
    }

    Dialog(onDismissRequest = { if (!registrando) onDismiss() }) {
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
                Text("MARCA (CATÁLOGO)", fontSize = 11.sp, color = HToGoColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                if (marcas.isEmpty()) {
                    Text(
                        "Tu negocio aún no tiene productos en el catálogo. Agrégalos en Productos y precios.",
                        fontSize = 12.sp, color = HToGoColors.AccentRose
                    )
                } else {
                    ExposedDropdownMenuBox(expanded = menuAbierto, onExpandedChange = { menuAbierto = it }) {
                        OutlinedTextField(
                            value = seleccionMarca?.let { "${it.nombre} · ${it.capacidad}" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Elige una marca") },
                            leadingIcon = { Icon(Icons.Filled.WaterDrop, null, tint = HToGoColors.Primary) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuAbierto) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                            marcas.forEach { m ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("${m.nombre} · ${m.capacidad}", fontWeight = FontWeight.Medium)
                                            Text(
                                                "${m.enBase} / ${m.maximoBase} en base",
                                                fontSize = 11.sp, color = HToGoColors.TextSecondary
                                            )
                                        }
                                    },
                                    onClick = { seleccionada = m.id; menuAbierto = false }
                                )
                            }
                        }
                    }
                }
                if (seleccionMarca != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (libres > 0) HToGoColors.PrimarySoft else HToGoColors.AccentRose.copy(alpha = .12f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warehouse, null, tint = if (libres > 0) HToGoColors.Primary else HToGoColors.AccentRose)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Destino: Base del negocio", fontSize = 13.sp, color = HToGoColors.PrimaryDark, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${seleccionMarca.enBase} / ${seleccionMarca.maximoBase} ocupados · $libres espacios libres",
                                fontSize = 11.sp, color = HToGoColors.TextSecondary
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("DATOS DEL LOTE", fontSize = 11.sp, color = HToGoColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = codigoLote,
                    onValueChange = { codigoLote = it.take(30) },
                    label = { Text("Código de lote (opcional)") },
                    placeholder = { Text("Ej. L-2026-05-001") },
                    leadingIcon = { Icon(Icons.Filled.QrCode2, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    supportingText = { Text("Identificador impreso por el proveedor") }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = fechaCaducidadValue,
                    onValueChange = { tfv ->
                        val digits = tfv.text.filter(Char::isDigit).take(8)
                        val formatted = buildString {
                            digits.forEachIndexed { i, c ->
                                if (i == 2 || i == 4) append('/')
                                append(c)
                            }
                        }
                        fechaCaducidadValue = androidx.compose.ui.text.input.TextFieldValue(
                            text = formatted,
                            selection = androidx.compose.ui.text.TextRange(formatted.length)
                        )
                    },
                    label = { Text("Fecha de caducidad") },
                    trailingIcon = {
                        IconButton(onClick = {
                            val cal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_MONTH, 30) }
                            android.app.DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val formatted = String.format("%02d/%02d/%04d", d, m + 1, y)
                                    fechaCaducidadValue = androidx.compose.ui.text.input.TextFieldValue(
                                        text = formatted,
                                        selection = androidx.compose.ui.text.TextRange(formatted.length)
                                    )
                                },
                                cal.get(java.util.Calendar.YEAR),
                                cal.get(java.util.Calendar.MONTH),
                                cal.get(java.util.Calendar.DAY_OF_MONTH)
                            ).apply {
                                datePicker.minDate = System.currentTimeMillis() + 8 * 86_400_000L
                            }.show()
                        }) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = "Seleccionar fecha", tint = HToGoColors.Primary)
                        }
                    },
                    placeholder = { Text("DD/MM/AAAA") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    isError = fechaCaducidad.length == 10 && !caducidadValida,
                    supportingText = {
                        Text(
                            if (fechaCaducidad.length == 10 && !caducidadValida) "Debe caducar en más de 7 días (RN-030)"
                            else "Escribe DD/MM/AAAA o toca el calendario"
                        )
                    }
                )
                if (seleccionMarca != null && seleccionMarca.lotes.isNotEmpty()) {
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
                if (libres > 0) {
                    Spacer(Modifier.height(14.dp))
                    Text("CANTIDAD DEL LOTE", fontSize = 11.sp, color = HToGoColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                    QtyStepper(cantidad, max = libres) { cantidad = it.coerceAtLeast(1) }
                    Text(
                        "Máximo $libres (espacio disponible en base para esta marca)",
                        fontSize = 11.sp, color = HToGoColors.TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(Modifier.height(18.dp))
                // Botones apilados: con letra grande el texto no se parte en dos líneas.
                Button(
                    onClick = {
                        val m = seleccionMarca ?: return@Button
                        onRegistrar(m.id.toInt(), cantidad, fechaIso ?: return@Button, codigoLote.trim())
                    },
                    enabled = motivoDeshabilitado == null && !registrando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary)
                ) {
                    if (registrando) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (registrando) "Registrando…" else "Registrar lote", fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
                if (motivoDeshabilitado != null) {
                    Text(
                        motivoDeshabilitado,
                        fontSize = 11.sp, color = HToGoColors.TextSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                TextButton(
                    onClick = onDismiss,
                    enabled = !registrando,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancelar") }
            }
        }
    }
}

@Composable
private fun CargarVehiculoDialog(
    marcas: List<MarcaBaseStock>,
    capacidadVehiculo: Int = 30,
    ocupadoVehiculo: Int = 0,
    onDismiss: () -> Unit,
    onCargar: (idMarca: Int, cantidad: Int) -> Unit = { _, _ -> }
) {
    var seleccionada by remember { mutableStateOf(marcas.firstOrNull()?.id ?: "") }
    val seleccionMarca = marcas.firstOrNull { it.id == seleccionada } ?: marcas.firstOrNull()
    val espacioLibre = maxOf(0, capacidadVehiculo - ocupadoVehiculo)
    val stockEnBase = seleccionMarca?.enBase ?: 0
    val maxCarga = minOf(stockEnBase, espacioLibre)
    var cantidad by remember(seleccionada, maxCarga) {
        mutableStateOf(if (maxCarga > 0) minOf(maxCarga, 5) else 0)
    }

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
                        Text("Vehículo: $ocupadoVehiculo / $capacidadVehiculo cap. · libre $espacioLibre espacios", fontSize = 11.sp, color = HToGoColors.TextSecondary)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("MARCA (DISPONIBLE EN BASE)", fontSize = 11.sp, color = HToGoColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                if (marcas.isEmpty()) {
                    Text("No hay marcas disponibles en la base", fontSize = 13.sp, color = HToGoColors.TextSecondary)
                } else {
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
                }
                Spacer(Modifier.height(14.dp))
                Text("CANTIDAD A CARGAR", fontSize = 11.sp, color = HToGoColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                QtyStepper(cantidad, max = maxCarga) { cantidad = it }
                Text(
                    if (maxCarga <= 0) {
                        if (espacioLibre <= 0) "Vehículo lleno (capacidad $capacidadVehiculo alcanzada)"
                        else "Sin stock disponible de esta marca en la base"
                    } else {
                        "Máx. $maxCarga — limitado por espacio disponible ($espacioLibre libres en vehículo, $stockEnBase en base)"
                    },
                    fontSize = 11.sp,
                    color = if (maxCarga <= 0) HToGoColors.AccentRose else HToGoColors.TextSecondary,
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
                            Text("Base: $stockEnBase → ", fontSize = 13.sp)
                            Text("${maxOf(0, stockEnBase - cantidad)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HToGoColors.AccentRose)
                            Text("  ·  ", fontSize = 13.sp, color = HToGoColors.TextTertiary)
                            Icon(Icons.Filled.LocalShipping, null, tint = HToGoColors.Primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Vehículo: $ocupadoVehiculo → ", fontSize = 13.sp)
                            Text("${ocupadoVehiculo + cantidad}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HToGoColors.AccentEmerald)
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
                            val idMarcaInt = seleccionMarca?.id?.toIntOrNull() ?: 1
                            onCargar(idMarcaInt, cantidad)
                        },
                        enabled = cantidad > 0 && cantidad <= maxCarga,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(23.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HToGoColors.Primary,
                            disabledContainerColor = HToGoColors.OutlineSoft,
                            disabledContentColor = HToGoColors.TextTertiary
                        )
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
    onSalida: (idMarca: Int, cantidad: Int, motivo: String) -> Unit
) {
    val marcasDisponibles = remember(marcas) { marcas.filter { it.enBase > 0 } }
    val listaMarcas = if (marcasDisponibles.isNotEmpty()) marcasDisponibles else marcas
    var seleccionada by remember { mutableStateOf(listaMarcas.firstOrNull()?.id ?: "") }
    val seleccionMarca = listaMarcas.firstOrNull { it.id == seleccionada } ?: listaMarcas.firstOrNull()
    var cantidad by remember { mutableStateOf(1) }
    var motivo by remember { mutableStateOf("Merma / rotura") }
    var enviando by remember { mutableStateOf(false) }
    val motivos = listOf("Merma / rotura", "Entrega fuera de la app", "Devolución a proveedor", "Ajuste de inventario")

    Dialog(onDismissRequest = { if (!enviando) onDismiss() }) {
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
                if (listaMarcas.isEmpty()) {
                    Text("No hay marcas configuradas en el negocio.", fontSize = 13.sp, color = HToGoColors.TextSecondary)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listaMarcas.forEach { m ->
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
                                        cantidad = minOf(cantidad, maxOf(1, m.enBase))
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
                }
                Spacer(Modifier.height(14.dp))
                Text("CANTIDAD A RESTAR", fontSize = 11.sp, color = HToGoColors.TextSecondary,
                    fontWeight = FontWeight.SemiBold)
                val maxDisponible = seleccionMarca?.enBase ?: 0
                QtyStepper(cantidad, max = maxOf(1, maxDisponible)) { cantidad = it }
                if (maxDisponible <= 0) {
                    Text(
                        "Sin stock disponible en base para esta marca",
                        fontSize = 11.sp,
                        color = HToGoColors.AccentRose,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
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
                        enabled = !enviando,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(23.dp)
                    ) { Text("Cancelar") }
                    Button(
                        onClick = {
                            val idMarcaInt = seleccionMarca?.id?.toIntOrNull()
                            if (idMarcaInt != null && cantidad > 0 && maxDisponible >= cantidad) {
                                enviando = true
                                onSalida(idMarcaInt, cantidad, motivo)
                            }
                        },
                        enabled = !enviando && seleccionMarca != null && maxDisponible >= cantidad && cantidad > 0,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(23.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.AccentRose)
                    ) {
                        if (enviando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Confirmar salida", fontWeight = FontWeight.SemiBold)
                        }
                    }
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
    val esNuevo = vehiculo == null
    val tipoInicial = when (vehiculo?.tipoVehiculo?.lowercase()) {
        "camioneta" -> "Camioneta"
        "automovil" -> "Automóvil"
        "bicicleta", "bicicleta_carga" -> "Bicicleta de carga"
        "triciclo_carga" -> "Triciclo de carga"
        else -> "Motocicleta"
    }
    var tipoNuevo by remember { mutableStateOf(tipoInicial) }
    var marca by remember { mutableStateOf(vehiculo?.marca ?: "") }
    var modelo by remember { mutableStateOf(vehiculo?.modelo ?: "") }
    var placas by remember { mutableStateOf(vehiculo?.placas ?: "") }
    var color by remember { mutableStateOf(vehiculo?.color ?: "") }
    var capacidad by remember { mutableStateOf((vehiculo?.capacidadGarrafones ?: 30).toString()) }
    var motivo by remember { mutableStateOf("") }
    val tipos = listOf("Motocicleta", "Camioneta", "Automóvil", "Bicicleta de carga", "Triciclo de carga")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    if (esNuevo) "Solicitar alta de nuevo vehículo" else "Solicitar cambio de vehículo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Toda alta o modificación requiere aprobación del Administrador antes de aplicarse (RN-019, CU-009).",
                    fontSize = 13.sp, color = HToGoColors.TextSecondary
                )
                Spacer(Modifier.height(14.dp))
                if (!esNuevo) {
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
                                "Capacidad actual: ${vehiculo?.capacidadGarrafones ?: 30} garrafones",
                                fontSize = 11.sp, color = HToGoColors.TextSecondary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Seguirá funcionando con estos datos hasta la resolución del Administrador.",
                                fontSize = 11.sp, color = HToGoColors.PrimaryDark, fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
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
                        placeholder = { Text("Ej. Italika / Nissan") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = modelo,
                        onValueChange = { modelo = it },
                        label = { Text("Modelo") },
                        placeholder = { Text("Ej. FT150 / NP300") },
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
                        placeholder = { Text("ABC1234") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = color,
                        onValueChange = { color = it },
                        label = { Text("Color") },
                        placeholder = { Text("Rojo / Blanco") },
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
                    label = { Text(if (esNuevo) "Notas adicionales (opcional)" else "Motivo / Notas del cambio (opcional)") },
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
                                "Bicicleta de carga" -> "bicicleta_carga"
                                "Triciclo de carga" -> "triciclo_carga"
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
                        enabled = marca.isNotBlank() && modelo.isNotBlank() && placas.isNotBlank(),
                        modifier = Modifier.weight(1.2f).height(46.dp),
                        shape = RoundedCornerShape(23.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary)
                    ) {
                        Text(if (esNuevo) "Enviar a aprobación" else "Solicitar cambio", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun IniciarJornadaDialog(
    vehiculo: com.htogo.app.data.dto.VehiculoDto,
    marcasBase: List<MarcaBaseStock>,
    onDismiss: () -> Unit,
    onIniciar: (idMarca: Int?, cantidad: Int) -> Unit
) {
    val capacidadVehiculo = vehiculo.capacidadGarrafones ?: 30
    var seleccionada by remember {
        mutableStateOf(marcasBase.firstOrNull { it.enBase > 0 }?.id ?: marcasBase.firstOrNull()?.id ?: "")
    }
    val marcaElegida = marcasBase.firstOrNull { it.id == seleccionada }
    val stockBase = marcaElegida?.enBase ?: 0
    var cantidad by remember { mutableStateOf(minOf(stockBase, capacidadVehiculo)) }

    val errorExcedeCapacidad = cantidad > capacidadVehiculo
    val errorSinStock = cantidad > stockBase

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(HToGoColors.PrimarySoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.DirectionsCar, null, tint = HToGoColors.Primary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Iniciar jornada", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HToGoColors.TextPrimary)
                        Text("CU-008: Activar disponibilidad", fontSize = 12.sp, color = HToGoColors.TextSecondary)
                    }
                }
                Spacer(Modifier.height(14.dp))
                // Info vehiculo seleccionado
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = HToGoColors.Background,
                    border = BorderStroke(1.dp, HToGoColors.OutlineSoft),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DirectionsCar, null, tint = HToGoColors.Primary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${vehiculo.marca ?: "Vehículo"} ${vehiculo.modelo ?: ""} · ${vehiculo.placas ?: ""}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                "Capacidad máxima: $capacidadVehiculo garrafones (RN-008)",
                                fontSize = 12.sp,
                                color = HToGoColors.TextSecondary
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("CARGA INICIAL DESDE BASE", fontSize = 11.sp, color = HToGoColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Carga garrafones al vehículo para habilitar la disponibilidad operativa (RN-008, RN-013).",
                    fontSize = 12.sp, color = HToGoColors.TextSecondary
                )
                Spacer(Modifier.height(10.dp))
                if (marcasBase.isNotEmpty()) {
                    Text("Marca a cargar:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        marcasBase.forEach { m ->
                            val sel = m.id == seleccionada
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, if (sel) HToGoColors.Primary else HToGoColors.OutlineSoft, RoundedCornerShape(10.dp))
                                    .background(if (sel) HToGoColors.PrimaryWash else Color.White)
                                    .clickable {
                                        seleccionada = m.id
                                        cantidad = minOf(m.enBase, capacidadVehiculo)
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${m.nombre} (${m.enBase} en base)", fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                if (sel) Icon(Icons.Filled.CheckCircle, null, tint = HToGoColors.Primary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Text("Cantidad a cargar:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                QtyStepper(value = cantidad, max = maxOf(capacidadVehiculo, stockBase)) {
                    cantidad = it
                }
                if (cantidad == 0) {
                    Text(
                        "ℹ Iniciarás jornada sin garrafones en el vehículo (Flujo S3). Podrás cargar después.",
                        fontSize = 11.sp,
                        color = HToGoColors.TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else if (errorExcedeCapacidad) {
                    Text(
                        "⚠ Excede la capacidad máxima ($capacidadVehiculo garrafones) · Flujo S1",
                        fontSize = 11.sp,
                        color = HToGoColors.AccentRose,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else if (errorSinStock) {
                    Text(
                        "⚠ Stock insuficiente en base (disponibles: $stockBase) · Flujo S2",
                        fontSize = 11.sp,
                        color = HToGoColors.AccentRose,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text(
                        "✓ Se registrará salida en base + entrada en vehículo con ID enlazado (RN-013).",
                        fontSize = 11.sp,
                        color = HToGoColors.AccentEmerald,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(23.dp)
                    ) { Text("Cancelar") }
                    Button(
                        onClick = {
                            val idMarcaInt = marcaElegida?.id?.toIntOrNull()
                            onIniciar(idMarcaInt, cantidad)
                        },
                        enabled = !errorExcedeCapacidad && (!errorSinStock || cantidad == 0),
                        modifier = Modifier.weight(1.3f).height(46.dp),
                        shape = RoundedCornerShape(23.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary)
                    ) {
                        Text("Iniciar jornada", fontWeight = FontWeight.SemiBold)
                    }
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
