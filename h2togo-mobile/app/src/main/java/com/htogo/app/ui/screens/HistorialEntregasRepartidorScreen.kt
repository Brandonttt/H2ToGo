package com.htogo.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.htogo.app.data.dto.HistorialResponse
import com.htogo.app.data.dto.PedidoResponse
import com.htogo.app.data.dto.PedidoResumenDto
import com.htogo.app.ui.RepartidorViewModel
import com.htogo.app.ui.components.RepartidorBottomBar
import com.htogo.app.ui.components.RepartidorTab
import com.htogo.app.ui.theme.HToGoColors

private enum class FiltroHistorial(val label: String) {
    TODAS("Todas"),
    ENTREGADAS("Entregadas"),
    NO_ENTREGADAS("No entregadas"),
    CANCELADAS("Canceladas"),
    EN_CURSO("En curso")
}

/**
 * CU-013: Historial de entregas
 * Consulta del historial de entregas del repartidor autenticado (RN-016), ordenadas
 * de más reciente a más antiguo, con visualización de estados, montos, garrafones y detalle
 * con timestamps del ciclo de vida. Consulta de solo lectura.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialEntregasRepartidorScreen(
    onBack: () -> Unit = {},
    onInicio: () -> Unit = {},
    onInventario: () -> Unit = {},
    onIngresos: () -> Unit = {},
    onPerfil: () -> Unit = {},
    onVerDisponibles: () -> Unit = {},
    repartidorViewModel: RepartidorViewModel = viewModel()
) {
    var filtroSeleccionado by remember { mutableStateOf(FiltroHistorial.TODAS) }
    var textoBusqueda by remember { mutableStateOf("") }
    var pedidoSeleccionadoId by remember { mutableStateOf<Int?>(null) }
    var detallePedido by remember { mutableStateOf<PedidoResponse?>(null) }
    var cargandoDetalle by remember { mutableStateOf(false) }

    val misEntregasRaw by repartidorViewModel.misEntregas.collectAsState()

    LaunchedEffect(Unit) {
        repartidorViewModel.cargarMisEntregas()
    }

    // CU-013 Paso 3: Ordenar de más reciente a más antiguo
    val entregasOrdenadas = remember(misEntregasRaw) {
        misEntregasRaw.sortedWith(
            compareByDescending<PedidoResumenDto> { it.fechaCreacion ?: "" }
                .thenByDescending { it.id }
        )
    }

    // Filtrar por tab y búsqueda
    val entregasFiltradas = remember(entregasOrdenadas, filtroSeleccionado, textoBusqueda) {
        entregasOrdenadas.filter { p ->
            val coincideFiltro = when (filtroSeleccionado) {
                FiltroHistorial.TODAS -> true
                FiltroHistorial.ENTREGADAS -> p.estado.equals("entregado", ignoreCase = true)
                FiltroHistorial.NO_ENTREGADAS -> p.estado.equals("no_entregado", ignoreCase = true)
                FiltroHistorial.CANCELADAS -> p.estado.equals("cancelado", ignoreCase = true)
                FiltroHistorial.EN_CURSO -> p.estado.equals("en_camino", ignoreCase = true) ||
                        p.estado.equals("asignado", ignoreCase = true) ||
                        p.estado.equals("pendiente", ignoreCase = true)
            }
            val q = textoBusqueda.trim().lowercase()
            val coincideBusqueda = if (q.isBlank()) {
                true
            } else {
                p.id.toString().contains(q) ||
                        (p.nombreCliente?.lowercase()?.contains(q) == true) ||
                        (p.direccionTexto?.lowercase()?.contains(q) == true) ||
                        p.estado.lowercase().contains(q)
            }
            coincideFiltro && coincideBusqueda
        }
    }

    // Contadores para las pestañas
    val totalCount = entregasOrdenadas.size
    val entregadasCount = entregasOrdenadas.count { it.estado.equals("entregado", ignoreCase = true) }
    val noEntregadasCount = entregasOrdenadas.count { it.estado.equals("no_entregado", ignoreCase = true) }
    val canceladasCount = entregasOrdenadas.count { it.estado.equals("cancelado", ignoreCase = true) }
    val enCursoCount = entregasOrdenadas.count {
        it.estado.equals("en_camino", ignoreCase = true) ||
                it.estado.equals("asignado", ignoreCase = true) ||
                it.estado.equals("pendiente", ignoreCase = true)
    }

    // Cargar detalle cuando se selecciona un pedido
    LaunchedEffect(pedidoSeleccionadoId) {
        val id = pedidoSeleccionadoId
        if (id != null) {
            cargandoDetalle = true
            repartidorViewModel.cargarDetallePedido(id) { resp ->
                detallePedido = resp
                cargandoDetalle = false
            }
        } else {
            detallePedido = null
            cargandoDetalle = false
        }
    }

    Scaffold(
        containerColor = HToGoColors.Background,
        bottomBar = {
            RepartidorBottomBar(
                selected = RepartidorTab.INICIO,
                onInicio = onInicio,
                onNegocio = onInventario,
                onIngresos = onIngresos,
                onPerfil = onPerfil
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            HeaderHistorial(
                totalEntregas = totalCount,
                textoBusqueda = textoBusqueda,
                onTextoBusquedaChange = { textoBusqueda = it },
                onBack = onBack
            )

            // Pestañas de filtro
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.background(HToGoColors.Background)
            ) {
                items(FiltroHistorial.values()) { f ->
                    val cantidad = when (f) {
                        FiltroHistorial.TODAS -> totalCount
                        FiltroHistorial.ENTREGADAS -> entregadasCount
                        FiltroHistorial.NO_ENTREGADAS -> noEntregadasCount
                        FiltroHistorial.CANCELADAS -> canceladasCount
                        FiltroHistorial.EN_CURSO -> enCursoCount
                    }
                    val activo = f == filtroSeleccionado
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = if (activo) HToGoColors.Primary else Color.White,
                        border = BorderStroke(1.dp, if (activo) HToGoColors.Primary else HToGoColors.OutlineSoft),
                        modifier = Modifier.clickable { filtroSeleccionado = f }
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                f.label,
                                fontSize = 13.sp,
                                fontWeight = if (activo) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (activo) Color.White else HToGoColors.TextPrimary
                            )
                            Surface(
                                shape = CircleShape,
                                color = if (activo) Color.White.copy(alpha = 0.25f) else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    "$cantidad",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activo) Color.White else HToGoColors.TextSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Lista de pedidos o estado vacío (Flujo alterno S1)
            if (entregasFiltradas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = HToGoColors.PrimarySoft.copy(alpha = 0.4f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.History,
                                    contentDescription = null,
                                    tint = HToGoColors.Primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (totalCount == 0) "Sin entregas registradas" else "No se encontraron entregas",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = HToGoColors.TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (totalCount == 0)
                                "Aún no tienes entregas en tu historial. Acepta pedidos disponibles para comenzar a trabajar."
                            else
                                "No hay entregas que coincidan con el filtro seleccionado.",
                            fontSize = 14.sp,
                            color = HToGoColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        if (totalCount == 0) {
                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = onVerDisponibles,
                                colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.DeliveryDining, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Ver pedidos disponibles", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(entregasFiltradas, key = { it.id }) { entrega ->
                        EntregaCard(
                            entrega = entrega,
                            onClick = { pedidoSeleccionadoId = entrega.id }
                        )
                    }
                }
            }
        }
    }

    // Modal de Detalle con Timestamps del ciclo de vida (CU-013 Paso 5)
    if (pedidoSeleccionadoId != null) {
        ModalDetalleEntrega(
            idPedido = pedidoSeleccionadoId!!,
            cargando = cargandoDetalle,
            detalle = detallePedido,
            onDismiss = {
                pedidoSeleccionadoId = null
                detallePedido = null
            }
        )
    }
}

@Composable
private fun HeaderHistorial(
    totalEntregas: Int,
    textoBusqueda: String,
    onTextoBusquedaChange: (String) -> Unit,
    onBack: () -> Unit
) {
    Surface(color = HToGoColors.PrimaryDark) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onBack)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = Color.White
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Mi historial de entregas",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "$totalEntregas ${if (totalEntregas == 1) "entrega registrada" else "entregas registradas"}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = textoBusqueda,
                        onValueChange = onTextoBusquedaChange,
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                        cursorBrush = SolidColor(Color.White),
                        decorationBox = { inner ->
                            if (textoBusqueda.isEmpty()) {
                                Text(
                                    "Buscar por #, cliente o dirección…",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            inner()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    if (textoBusqueda.isNotEmpty()) {
                        IconButton(
                            onClick = { onTextoBusquedaChange("") },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Limpiar", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta individual de entrega para CU-013 Paso 4:
 * Muestra: fecha, cliente, dirección, cantidad de garrafones, estado final, y monto.
 */
@Composable
private fun EntregaCard(
    entrega: PedidoResumenDto,
    onClick: () -> Unit
) {
    val estadoInfo = obtenerEstadoEntrega(entrega.estado)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, HToGoColors.OutlineSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            // Fila superior: ID y Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Pedido #${entrega.id}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = HToGoColors.TextPrimary
                    )
                    if (entrega.esProgramado) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = HToGoColors.PrimarySoft.copy(alpha = 0.5f)
                        ) {
                            Text(
                                "PROGRAMADO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = HToGoColors.PrimaryDark,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Badge de Estado
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = estadoInfo.color.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, estadoInfo.color.copy(alpha = 0.3f))
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = estadoInfo.icono,
                            contentDescription = null,
                            tint = estadoInfo.color,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = estadoInfo.titulo,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = estadoInfo.color
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Cliente
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = HToGoColors.TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = entrega.nombreCliente ?: "Cliente H2ToGo",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = HToGoColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(4.dp))

            // Dirección
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = HToGoColors.TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = entrega.direccionTexto ?: "Dirección registrada",
                    fontSize = 12.sp,
                    color = HToGoColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = HToGoColors.OutlineSoft)
            Spacer(Modifier.height(8.dp))

            // Pie de tarjeta: Garrafones, Total y Fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "${entrega.garrafonesTotales ?: 1} garrafón(es)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HToGoColors.TextPrimary
                    )
                    Text(
                        formatearFechaCorta(entrega.fechaCreacion),
                        fontSize = 11.sp,
                        color = HToGoColors.TextTertiary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (entrega.totalPagar != null) {
                        Text(
                            "$${String.format(java.util.Locale.US, "%.2f", entrega.totalPagar)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = HToGoColors.Primary
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = "Ver detalle",
                        tint = HToGoColors.TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Modal para CU-013 Paso 5:
 * El repartidor puede seleccionar un pedido para ver el detalle y los timestamps del ciclo de vida.
 * Solo lectura.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModalDetalleEntrega(
    idPedido: Int,
    cargando: Boolean,
    detalle: PedidoResponse?,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Cabecera del modal
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Detalle de Entrega #$idPedido",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = HToGoColors.TextPrimary
                    )
                    Text(
                        "Consulta de solo lectura",
                        fontSize = 12.sp,
                        color = HToGoColors.TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = HToGoColors.TextSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))

            if (cargando) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = HToGoColors.Primary)
                }
            } else if (detalle == null) {
                Text(
                    "No se pudo cargar la información detallada del pedido.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Estado e información clave
                    item {
                        val estadoInfo = obtenerEstadoEntrega(detalle.estado)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = estadoInfo.color.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, estadoInfo.color.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = estadoInfo.icono,
                                        contentDescription = null,
                                        tint = estadoInfo.color,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            estadoInfo.titulo,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = estadoInfo.color
                                        )
                                        Text(
                                            "Tipo: ${detalle.tipoSolicitud.replace('_', ' ').lowercase()}",
                                            fontSize = 12.sp,
                                            color = HToGoColors.TextSecondary
                                        )
                                    }
                                }

                                if (detalle.totalPagar != null) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "Total cobrado",
                                            fontSize = 11.sp,
                                            color = HToGoColors.TextSecondary
                                        )
                                        Text(
                                            "$${String.format(java.util.Locale.US, "%.2f", detalle.totalPagar)}",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HToGoColors.TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Datos del cliente y dirección
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = HToGoColors.Background),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "INFORMACIÓN DEL CLIENTE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HToGoColors.TextSecondary
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Person, null, tint = HToGoColors.Primary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        detalle.nombreCliente ?: "Cliente H2ToGo",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = HToGoColors.TextPrimary
                                    )
                                }
                                if (!detalle.telefonoCliente.isNullOrBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Phone, null, tint = HToGoColors.Primary, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            detalle.telefonoCliente,
                                            fontSize = 13.sp,
                                            color = HToGoColors.TextSecondary
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Filled.LocationOn, null, tint = HToGoColors.Primary, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        detalle.direccionTexto ?: "Dirección de entrega",
                                        fontSize = 13.sp,
                                        color = HToGoColors.TextPrimary
                                    )
                                }
                                if (!detalle.indicaciones.isNullOrBlank()) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(Icons.Filled.Info, null, tint = HToGoColors.TextSecondary, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Indicaciones: ${detalle.indicaciones}",
                                            fontSize = 12.sp,
                                            color = HToGoColors.TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Artículos / Garrafones solicitados
                    if (!detalle.detalles.isNullOrEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = HToGoColors.Background),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        "PRODUCTOS ENTREGADOS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HToGoColors.TextSecondary
                                    )
                                    detalle.detalles.forEach { d ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "${d.cantidad} × ${d.nombreMarca ?: "Garrafón"} ${if (d.tieneEnvase) "(con envase)" else "(refill)"}",
                                                fontSize = 13.sp,
                                                color = HToGoColors.TextPrimary
                                            )
                                            if (d.subtotal != null) {
                                                Text(
                                                    "$${String.format(java.util.Locale.US, "%.2f", d.subtotal)}",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = HToGoColors.TextPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TIMESTAMPS DEL CICLO DE VIDA (CU-013 Paso 5)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, HToGoColors.OutlineSoft),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Schedule,
                                        contentDescription = null,
                                        tint = HToGoColors.Primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "TIMESTAMPS DEL CICLO DE VIDA",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HToGoColors.TextSecondary
                                    )
                                }
                                Spacer(Modifier.height(12.dp))

                                val historial = detalle.historial ?: emptyList()
                                if (historial.isEmpty()) {
                                    // Fallback: al menos fecha de creación
                                    ItemTimestamp(
                                        titulo = "Pedido creado",
                                        fechaIso = detalle.fechaCreacion,
                                        notas = null,
                                        esUltimo = true
                                    )
                                } else {
                                    historial.forEachIndexed { index, h ->
                                        ItemTimestamp(
                                            titulo = mapearEstadoHistorial(h.estado),
                                            fechaIso = h.fechaCambio ?: h.fecha,
                                            notas = h.notas,
                                            esUltimo = index == historial.lastIndex
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemTimestamp(
    titulo: String,
    fechaIso: String?,
    notas: String?,
    esUltimo: Boolean
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(HToGoColors.Primary)
            )
            if (!esUltimo) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(HToGoColors.OutlineSoft)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (esUltimo) 0.dp else 12.dp)
        ) {
            Text(
                titulo,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = HToGoColors.TextPrimary
            )
            Text(
                formatearFechaLarga(fechaIso),
                fontSize = 11.sp,
                color = HToGoColors.TextSecondary
            )
            if (!notas.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    notas,
                    fontSize = 11.sp,
                    color = HToGoColors.TextTertiary,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

private data class InfoEstado(
    val titulo: String,
    val color: Color,
    val icono: ImageVector
)

private fun obtenerEstadoEntrega(estadoRaw: String?): InfoEstado {
    return when (estadoRaw?.lowercase()) {
        "entregado" -> InfoEstado("Entregado", HToGoColors.StatusEntregado, Icons.Filled.CheckCircle)
        "no_entregado" -> InfoEstado("No entregado", Color(0xFFE65100), Icons.Filled.Warning)
        "cancelado" -> InfoEstado("Cancelado", HToGoColors.StatusCancelado, Icons.Filled.Cancel)
        "en_camino" -> InfoEstado("En camino", HToGoColors.StatusEnCamino, Icons.Filled.DeliveryDining)
        "asignado" -> InfoEstado("Asignado", HToGoColors.Primary, Icons.Filled.AssignmentTurnedIn)
        "pendiente" -> InfoEstado("Pendiente", HToGoColors.StatusPendiente, Icons.Filled.HourglassEmpty)
        else -> InfoEstado(
            estadoRaw?.replace('_', ' ')?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() } ?: "Registrado",
            HToGoColors.TextSecondary,
            Icons.Filled.Info
        )
    }
}

private fun mapearEstadoHistorial(estado: String?): String {
    return when (estado?.lowercase()) {
        "pendiente" -> "1. Pedido solicitado"
        "asignado" -> "2. Repartidor asignado"
        "en_camino" -> "3. Pedido en ruta"
        "entregado" -> "4. Pedido entregado exitosamente"
        "no_entregado" -> "4. Pedido marcado como no entregado"
        "cancelado" -> "Pedido cancelado"
        else -> estado?.replace('_', ' ')?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() } ?: "Cambio de estado"
    }
}

private fun formatearFechaCorta(isoString: String?): String {
    if (isoString.isNullOrBlank()) return "Reciente"
    return try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val odt = java.time.OffsetDateTime.parse(isoString)
            val dtf = java.time.format.DateTimeFormatter.ofPattern("d MMM, HH:mm", java.util.Locale("es", "MX"))
            odt.format(dtf)
        } else {
            isoString.take(16).replace("T", " ")
        }
    } catch (_: Exception) {
        isoString.take(16).replace("T", " ")
    }
}

private fun formatearFechaLarga(isoString: String?): String {
    if (isoString.isNullOrBlank()) return "Fecha no registrada"
    return try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val odt = java.time.OffsetDateTime.parse(isoString)
            val dtf = java.time.format.DateTimeFormatter.ofPattern("d 'de' MMMM yyyy, HH:mm:ss", java.util.Locale("es", "MX"))
            odt.format(dtf)
        } else {
            isoString.replace("T", " ")
        }
    } catch (_: Exception) {
        isoString.replace("T", " ")
    }
}
