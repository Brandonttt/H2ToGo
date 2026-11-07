package com.htogo.app.ui.screens

import androidx.compose.ui.platform.LocalUriHandler

import com.htogo.app.plataforma.mostrarAviso
import com.htogo.app.plataforma.ahoraMs
import com.htogo.app.util.diaMesHoraLegible
import com.htogo.app.util.fechaCdmx
import com.htogo.app.util.format

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.htogo.app.data.dto.PedidoResponse
import com.htogo.app.ui.ClienteViewModel
import com.htogo.app.ui.components.EstadoPedido
import com.htogo.app.ui.components.EstadoPedidoChip
import com.htogo.app.ui.components.OsmRouteMapView
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme

data class TrackingStep(
    val title: String,
    val time: String,
    val state: StepState
) { enum class StepState { DONE, ACTIVE, PENDING } }

data class TrackedOrder(
    val id: String,
    val estado: EstadoPedido,
    val driverName: String,
    val driverInitials: String,
    val purificadoraName: String,
    val driverVehicle: String,
    val items: String,
    val address: String,
    val total: Double,
    /** null cuando no hay repartidor o el pedido ya se cerró: oculta el botón de llamar. */
    val driverPhone: String? = null
)

@Composable
private fun MapMock(modifier: Modifier = Modifier, purificadoraName: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "pulse-scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "pulse-alpha"
    )

    Box(
        modifier.background(
            Brush.linearGradient(
                listOf(Color(0xFFDCEEF7), Color(0xFFE8F5FA), Color(0xFFD6EAF4))
            )
        )
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            drawLine(Color.White,
                start = androidx.compose.ui.geometry.Offset(-50f, h * 0.30f),
                end   = androidx.compose.ui.geometry.Offset(w + 50f, h * 0.36f),
                strokeWidth = 14f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(Color.White,
                start = androidx.compose.ui.geometry.Offset(-50f, h * 0.62f),
                end   = androidx.compose.ui.geometry.Offset(w + 50f, h * 0.74f),
                strokeWidth = 12f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(Color.White,
                start = androidx.compose.ui.geometry.Offset(w * 0.18f, -30f),
                end   = androidx.compose.ui.geometry.Offset(w * 0.30f, h + 30f),
                strokeWidth = 12f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(Color.White,
                start = androidx.compose.ui.geometry.Offset(w * 0.50f, -30f),
                end   = androidx.compose.ui.geometry.Offset(w * 0.66f, h + 30f),
                strokeWidth = 14f, cap = androidx.compose.ui.graphics.StrokeCap.Round)

            drawRoundRect(
                color = Color(0xFFBFE3CB),
                topLeft = androidx.compose.ui.geometry.Offset(w * 0.55f, h * 0.18f),
                size = androidx.compose.ui.geometry.Size(w * 0.22f, h * 0.10f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f)
            )

            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.30f, h * 0.60f)
                quadraticBezierTo(w * 0.48f, h * 0.50f, w * 0.55f, h * 0.40f)
                quadraticBezierTo(w * 0.62f, h * 0.34f, w * 0.72f, h * 0.32f)
            }
            drawPath(
                path = path,
                color = HToGoColors.Primary.copy(alpha = 0.25f),
                style = Stroke(width = 12f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            drawPath(
                path = path,
                color = HToGoColors.Primary,
                style = Stroke(
                    width = 8f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 18f))
                )
            )
        }

        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (412 * 0.30f).dp - 23.dp, y = (520 * 0.60f).dp - 60.dp)
            ) {
                Box(
                    Modifier
                        .size(46.dp)
                        .graphicsLayer { scaleX = pulse; scaleY = pulse }
                        .clip(CircleShape)
                        .background(HToGoColors.Primary.copy(alpha = pulseAlpha * 0.4f))
                )
            }
            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (412 * 0.30f).dp - 36.dp, y = (520 * 0.60f).dp - 70.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Text("Repartidor",
                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = HToGoColors.TextPrimary)
                }
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = CircleShape,
                    color = HToGoColors.Primary,
                    border = androidx.compose.foundation.BorderStroke(3.dp, Color.White),
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.LocalShipping, null, tint = Color.White,
                            modifier = Modifier.size(24.dp))
                    }
                }
            }

            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (412 * 0.72f).dp - 18.dp, y = (520 * 0.32f).dp - 36.dp)
                    .size(36.dp)
                    .rotate(-45f)
                    .background(HToGoColors.StatusEntregado, RoundedCornerShape(50, 50, 50, 0))
                    .border(3.dp, Color.White, RoundedCornerShape(50, 50, 50, 0)),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier
                    .size(14.dp)
                    .rotate(45f)
                    .clip(CircleShape)
                    .background(Color.White))
            }
        }

        Surface(
            shape = RoundedCornerShape(99.dp),
            color = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 70.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(HToGoColors.StatusEnCamino))
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.Storefront, null, tint = HToGoColors.StatusEnCamino,
                    modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(purificadoraName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = HToGoColors.TextPrimary)
                Text(" · Tu repartidor viene en camino",
                    fontSize = 13.sp, color = HToGoColors.TextSecondary)
            }
        }

        Column(
            Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 70.dp, end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MapFab(Icons.Filled.MyLocation)
        }
    }
}

@Composable
private fun MapFab(icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier.size(42.dp).clickable { }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = HToGoColors.TextPrimary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun FloatingTopBar(
    orderId: String,
    onBack: () -> Unit,
    enModoDemo: Boolean = false,
    simulandoRecorrido: Boolean = false,
    onAlternarRecorridoDemo: () -> Unit = {},
    onSaltarInicioDemo: () -> Unit = {},
    onSaltarLlegadaDemo: () -> Unit = {},
    onDesactivarDemo: () -> Unit = {}
) {
    var menuAbierto by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = CircleShape, color = Color.White, shadowElevation = 4.dp,
            modifier = Modifier.size(40.dp).clickable(onClick = onBack)
        ) { Box(contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = HToGoColors.TextPrimary)
        } }
        Surface(
            shape = RoundedCornerShape(99.dp), color = Color.White, shadowElevation = 4.dp,
            modifier = Modifier.weight(1f)
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tu pedido ",
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = HToGoColors.TextPrimary)
                Text("#$orderId",
                    fontSize = 13.sp, color = HToGoColors.TextSecondary)
            }
        }
        Box {
            Surface(
                shape = CircleShape,
                color = if (enModoDemo) Color(0xFF7C3AED) else Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.size(40.dp).clickable { menuAbierto = true }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Simulador FakeGPS",
                        tint = if (enModoDemo) Color.White else HToGoColors.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                DropdownMenuItem(
                    text = {
                        Text(
                            if (simulandoRecorrido) "⏸️ Pausar recorrido en vivo (FakeGPS)"
                            else "▶️ Simular recorrido completo (FakeGPS)",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    onClick = { menuAbierto = false; onAlternarRecorridoDemo() }
                )
                DropdownMenuItem(
                    text = { Text("🏪 Saltar a la purificadora (inicio de ruta)") },
                    onClick = { menuAbierto = false; onSaltarInicioDemo() }
                )
                DropdownMenuItem(
                    text = { Text("🏁 Saltar al domicilio (llegada)") },
                    onClick = { menuAbierto = false; onSaltarLlegadaDemo() }
                )
                if (enModoDemo) {
                    DropdownMenuItem(
                        text = { Text("📍 Volver al rastreo real") },
                        onClick = { menuAbierto = false; onDesactivarDemo() }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackingSheet(
    order: TrackedOrder,
    steps: List<TrackingStep>,
    etaResumen: String? = null,
    maxScreenHeight: androidx.compose.ui.unit.Dp = 800.dp,
    isMinimized: Boolean = false,
    onMinimizedChange: (Boolean) -> Unit = {},
    onPedidoEntregado: () -> Unit,
    onAbrirPurificadora: () -> Unit,
    onCancelarPedido: () -> Unit = {}
) {
    val isEntregado = order.estado == EstadoPedido.ENTREGADO
    val scrollState = rememberScrollState()

    // Al marcarse como entregado (o cerrado), expande la pestaña automáticamente y muestra el botón de confirmación.
    LaunchedEffect(order.estado) {
        if (order.estado in setOf(EstadoPedido.ENTREGADO, EstadoPedido.NO_ENTREGADO, EstadoPedido.CANCELADO)) {
            onMinimizedChange(false)
            kotlinx.coroutines.delay(120)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val sheetMaxHeight = maxScreenHeight * 0.65f + navBarBottom
    val sheetMinHeight = 88.dp + navBarBottom
    val animatedHeight by animateDpAsState(
        targetValue = if (isMinimized) sheetMinHeight else sheetMaxHeight,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "trackingSheetHeight"
    )

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = HToGoColors.Surface,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(animatedHeight)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = if (isMinimized) 0.dp else 16.dp)
        ) {
            // Cabecera interactiva y arrastrable (Handle + Chip + Resumen + Chevron)
            Column(
                Modifier
                    .fillMaxWidth()
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            if (delta > 10f && !isMinimized) {
                                onMinimizedChange(true)
                            } else if (delta < -10f && isMinimized) {
                                onMinimizedChange(false)
                            }
                        }
                    )
                    .clickable { onMinimizedChange(!isMinimized) }
                    .padding(top = 10.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .width(44.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(HToGoColors.OutlineSoft)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EstadoPedidoChip(order.estado)
                    Spacer(Modifier.width(10.dp))
                    if (isMinimized) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                order.driverName.takeIf { it != "Por asignar" } ?: order.purificadoraName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HToGoColors.TextPrimary,
                                maxLines = 1
                            )
                            Text(
                                listOfNotNull(
                                    etaResumen,
                                    (if (order.total % 1.0 == 0.0) "$%.0f MXN" else "$%.2f MXN").format(order.total),
                                    order.items
                                ).joinToString(" · "),
                                fontSize = 11.5.sp,
                                color = HToGoColors.TextSecondary,
                                maxLines = 1
                            )
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                        if (etaResumen != null) {
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = HToGoColors.PrimarySoft
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Schedule,
                                        contentDescription = null,
                                        tint = HToGoColors.Primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        etaResumen,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = HToGoColors.Primary
                                    )
                                }
                            }
                            Spacer(Modifier.width(6.dp))
                        }
                    }
                    IconButton(
                        onClick = { onMinimizedChange(!isMinimized) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (isMinimized) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (isMinimized) "Expandir desglose" else "Minimizar desglose",
                            tint = HToGoColors.TextSecondary
                        )
                    }
                }
            }

            if (!isMinimized) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 18.dp)
                ) {
                    DriverCard(order, onAbrirPurificadora)
                    Spacer(Modifier.height(14.dp))

                    Timeline(steps)
                    Spacer(Modifier.height(14.dp))

                    OrderRow("Pedido", order.items)
                    OrderRow("Domicilio", order.address)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = HToGoColors.OutlineSoft)
                    OrderRow(
                        "Total a pagar",
                        (if (order.total % 1.0 == 0.0) "$%.0f MXN" else "$%.2f MXN").format(order.total),
                        valueColor = HToGoColors.Primary,
                        bold = true
                    )
                    Spacer(Modifier.height(8.dp))

                    if (isEntregado) {
                        Button(
                            onClick = onPedidoEntregado,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.StatusEntregado)
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Confirmar pedido entregado", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    } else if (order.estado == EstadoPedido.PENDIENTE) {
                        Button(
                            onClick = onCancelarPedido,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Filled.Cancel, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Cancelar pedido", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Puedes cancelar mientras ningún repartidor haya tomado tu pedido.",
                            fontSize = 11.sp, color = HToGoColors.TextTertiary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    } else if (order.estado == EstadoPedido.CANCELADO || order.estado == EstadoPedido.NO_ENTREGADO) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (order.estado == EstadoPedido.NO_ENTREGADO) "El repartidor no pudo entregar este pedido"
                                else "Este pedido fue cancelado",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, HToGoColors.OutlineSoft),
                            colors = ButtonDefaults.outlinedButtonColors(
                                disabledContainerColor = HToGoColors.Background,
                                disabledContentColor = HToGoColors.TextTertiary
                            )
                        ) {
                            Text("Cancelar pedido", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Lock, null, tint = HToGoColors.TextTertiary, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (order.estado == EstadoPedido.EN_CAMINO) "Ya no puedes cancelar: tu pedido está en camino"
                                else "Ya no puedes cancelar: un repartidor ya tomó tu pedido",
                                fontSize = 11.sp,
                                color = HToGoColors.TextTertiary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DriverCard(order: TrackedOrder, onAbrirPurificadora: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = HToGoColors.Background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(HToGoColors.PrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Text(order.driverInitials, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = HToGoColors.Primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${order.purificadoraName} · ${order.driverName.substringBefore(' ')}",
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = HToGoColors.TextSecondary
                )
                Text(order.driverName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = HToGoColors.TextPrimary)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onAbrirPurificadora)
                ) {
                    Icon(Icons.Filled.Storefront, null, tint = HToGoColors.Primary,
                        modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(order.purificadoraName,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HToGoColors.Primary)
                }
                Text(order.driverVehicle, fontSize = 11.sp, color = HToGoColors.TextTertiary)
            }
            if (order.driverPhone != null) {
                val uriHandler = LocalUriHandler.current
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = CircleShape, color = HToGoColors.Primary,
                    modifier = Modifier.size(42.dp).clickable {
                        runCatching { uriHandler.openUri("tel:${order.driverPhone}") }
                    }
                ) { Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Call, "Llamar al repartidor", tint = Color.White, modifier = Modifier.size(20.dp))
                } }
            }
        }
    }
}

@Composable
private fun Timeline(steps: List<TrackingStep>) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = HToGoColors.Background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            steps.forEachIndexed { i, step ->
                Row {
                    Column(
                        Modifier.width(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TimelineDot(step.state)
                        if (i < steps.lastIndex) {
                            Box(
                                Modifier.width(2.dp).height(28.dp)
                                    .background(
                                        if (step.state == TrackingStep.StepState.DONE)
                                            HToGoColors.StatusEntregado
                                        else HToGoColors.OutlineSoft
                                    )
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.padding(bottom = if (i < steps.lastIndex) 14.dp else 0.dp)) {
                        Text(step.title,
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = if (step.state == TrackingStep.StepState.PENDING)
                                HToGoColors.TextTertiary else HToGoColors.TextPrimary)
                        Text(step.time,
                            fontSize = 11.sp,
                            color = when (step.state) {
                                TrackingStep.StepState.ACTIVE -> HToGoColors.StatusEnCamino
                                else -> HToGoColors.TextSecondary
                            },
                            fontWeight = if (step.state == TrackingStep.StepState.ACTIVE)
                                FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineDot(state: TrackingStep.StepState) {
    when (state) {
        TrackingStep.StepState.DONE -> Box(
            Modifier.size(18.dp).clip(CircleShape).background(HToGoColors.StatusEntregado),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(11.dp)) }
        TrackingStep.StepState.ACTIVE -> Box(
            Modifier.size(18.dp).clip(CircleShape).background(HToGoColors.StatusEnCamino)
                .border(3.dp, HToGoColors.StatusEnCamino.copy(alpha = 0.25f), CircleShape)
        )
        TrackingStep.StepState.PENDING -> Box(
            Modifier.size(18.dp).clip(CircleShape).background(HToGoColors.OutlineSoft)
        )
    }
}

@Composable
private fun OrderRow(
    label: String, value: String,
    valueColor: Color = HToGoColors.TextPrimary, bold: Boolean = false
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = HToGoColors.TextSecondary)
        Text(value, fontSize = 13.sp, color = valueColor,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
fun SeguimientoPedidoScreen(
    pedidoId: Int? = null,
    onBack: () -> Unit = {},
    onPedidoEntregado: () -> Unit = {},
    onAbrirPurificadora: () -> Unit = {},
    clienteViewModel: ClienteViewModel = viewModel { ClienteViewModel() }
) {
    val livePedidoActivo by clienteViewModel.pedidoActivo.collectAsState()
    val livePedidoSeguimiento by clienteViewModel.pedidoSeguimiento.collectAsState()
    val ubicacionRepartidor by clienteViewModel.ubicacionRepartidor.collectAsState()
    val rutaSeguimiento by clienteViewModel.rutaSeguimiento.collectAsState()
    val distanciaRutaKm by clienteViewModel.distanciaRutaKm.collectAsState()
    val repartidorLlego by clienteViewModel.repartidorLlego.collectAsState()
    val purificadoras by clienteViewModel.purificadoras.collectAsState()
    val perfilPurificadora by clienteViewModel.perfilPurificadora.collectAsState()

    var ultimoPedidoValido by remember { mutableStateOf<PedidoResponse?>(null) }
    var errorCarga by remember { mutableStateOf(false) }

    val idSeguimiento = pedidoId ?: livePedidoSeguimiento?.id ?: livePedidoActivo?.id ?: ultimoPedidoValido?.id
    LaunchedEffect(idSeguimiento) {
        if (idSeguimiento != null) {
            errorCarga = false
            clienteViewModel.cargarDetallePedido(idSeguimiento) { res ->
                if (res != null) {
                    ultimoPedidoValido = res
                    errorCarga = false
                } else if (ultimoPedidoValido == null) {
                    errorCarga = true
                }
            }
        }
    }
    LaunchedEffect(pedidoId, ultimoPedidoValido) {
        if (pedidoId != null && ultimoPedidoValido == null) {
            kotlinx.coroutines.delay(4500)
            if (ultimoPedidoValido == null && livePedidoSeguimiento?.id != pedidoId && livePedidoActivo?.id != pedidoId) {
                errorCarga = true
            }
        }
    }
    DisposableEffect(idSeguimiento) {
        if (idSeguimiento != null) {
            clienteViewModel.vincularRastreo("seguimiento", idSeguimiento)
        }
        onDispose { clienteViewModel.desvincularRastreo("seguimiento") }
    }

    val marcas by clienteViewModel.marcas.collectAsState()
    val pedidoCandidato = livePedidoSeguimiento?.takeIf { pedidoId == null || it.id == pedidoId }
        ?: livePedidoActivo?.takeIf { pedidoId == null || it.id == pedidoId }

    LaunchedEffect(pedidoCandidato) {
        if (pedidoCandidato != null) {
            ultimoPedidoValido = pedidoCandidato
            errorCarga = false
        }
    }

    // Retener el último pedido válido cargado para que el cierre o refresco en segundo plano nunca vacíe la pantalla
    val pedido = pedidoCandidato ?: ultimoPedidoValido?.takeIf { pedidoId == null || it.id == pedidoId }
    if (pedido == null) {
        Box(Modifier.fillMaxSize().background(HToGoColors.Background)) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.statusBarsPadding().padding(8.dp).align(Alignment.TopStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = HToGoColors.TextPrimary)
            }
            if (pedidoId == null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No tienes un pedido en curso", color = HToGoColors.TextSecondary, fontSize = 15.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onBack,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary)
                    ) {
                        Text("Volver al inicio", color = Color.White)
                    }
                }
            } else if (errorCarga) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.ErrorOutline, null, tint = HToGoColors.TextSecondary, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No se pudo cargar el pedido #$pedidoId", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HToGoColors.TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text("Es posible que ya haya finalizado o no esté disponible.", fontSize = 14.sp, color = HToGoColors.TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                errorCarga = false
                                clienteViewModel.cargarDetallePedido(pedidoId) { res ->
                                    if (res != null) ultimoPedidoValido = res else errorCarga = true
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Reintentar")
                        }
                        Button(
                            onClick = onBack,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary)
                        ) {
                            Text("Volver al inicio", color = Color.White)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = HToGoColors.Primary)
                    Spacer(Modifier.height(16.dp))
                    Text("Cargando información del pedido...", color = HToGoColors.TextSecondary, fontSize = 14.sp)
                }
            }
        }
        return
    }
    val order = remember(pedido, marcas) {
        val p = pedido
        val estEnum = when (p.estado.lowercase()) {
            "asignado" -> EstadoPedido.ASIGNADO
            "en_camino" -> EstadoPedido.EN_CAMINO
            "entregado" -> EstadoPedido.ENTREGADO
            "cancelado" -> EstadoPedido.CANCELADO
            "no_entregado" -> EstadoPedido.NO_ENTREGADO
            else -> EstadoPedido.PENDIENTE
        }
        val rep = p.repartidor
        val nombreRep = rep?.nombre?.takeIf { it.isNotBlank() }
        val nombresMarca = marcas.associate { it.id to it.nombre }
        TrackedOrder(
            id = p.id.toString(),
            estado = estEnum,
            driverName = nombreRep ?: "Por asignar",
            driverInitials = nombreRep?.split(" ")?.filter { it.isNotBlank() }?.take(2)
                ?.joinToString("") { it.first().uppercase() } ?: "H2",
            purificadoraName = rep?.negocio ?: p.nombreNegocio ?: clienteViewModel.purificadoraSeleccionadaNombre ?: "Purificadora",
            driverVehicle = listOfNotNull(rep?.vehiculo, rep?.placas?.takeIf { it.isNotBlank() }?.let { "Placas $it" })
                .joinToString(" · ").ifBlank { if (rep == null) "Esperando que una purificadora acepte" else "Vehículo por confirmar" },
            items = p.detalles.orEmpty().takeIf { it.isNotEmpty() }
                ?.joinToString(", ") { d -> "${d.cantidad} × ${d.nombreMarca ?: nombresMarca[d.idMarca] ?: "Garrafón"} 20 L" }
                ?: "${p.garrafonesTotales ?: 0} × Garrafón 20 L",
            address = p.direccionTexto?.takeIf { it.isNotBlank() } ?: "Domicilio de entrega",
            total = p.totalPagar ?: 0.0,
            driverPhone = rep?.telefono?.takeIf { it.isNotBlank() }
        )
    }

    val historialPedido = pedido.historial
    val pasos = remember(order.estado, repartidorLlego) {
        when (order.estado) {
            EstadoPedido.PENDIENTE -> listOf(
                TrackingStep("Pedido recibido", "En espera de repartidor", TrackingStep.StepState.ACTIVE),
                TrackingStep("Repartidor asignado", "Pendiente", TrackingStep.StepState.PENDING),
                TrackingStep("En camino a tu domicilio", "Pendiente", TrackingStep.StepState.PENDING),
                TrackingStep("Entregado", "Pendiente", TrackingStep.StepState.PENDING)
            )
            EstadoPedido.ASIGNADO -> listOf(
                TrackingStep("Pedido recibido", "Confirmado", TrackingStep.StepState.DONE),
                TrackingStep("Repartidor asignado", "Preparando pedido", TrackingStep.StepState.ACTIVE),
                TrackingStep("En camino a tu domicilio", "Pendiente", TrackingStep.StepState.PENDING),
                TrackingStep("Entregado", "Pendiente", TrackingStep.StepState.PENDING)
            )
            EstadoPedido.EN_CAMINO -> listOf(
                TrackingStep("Pedido recibido", "Confirmado", TrackingStep.StepState.DONE),
                TrackingStep("Repartidor asignado", "Pedido preparado", TrackingStep.StepState.DONE),
                TrackingStep("En camino a tu domicilio", if (repartidorLlego) "¡Llegó! Está afuera de tu domicilio" else "En ruta a tu casa", TrackingStep.StepState.ACTIVE),
                TrackingStep("Entregado", "Pendiente", TrackingStep.StepState.PENDING)
            )
            EstadoPedido.ENTREGADO -> listOf(
                TrackingStep("Pedido recibido", "Confirmado", TrackingStep.StepState.DONE),
                TrackingStep("Repartidor asignado", "Pedido preparado", TrackingStep.StepState.DONE),
                TrackingStep("En camino a tu domicilio", "Llegó al domicilio", TrackingStep.StepState.DONE),
                TrackingStep("Entregado", "Entrega completada", TrackingStep.StepState.DONE)
            )
            EstadoPedido.CANCELADO -> listOf(
                TrackingStep("Pedido recibido", "Recibido", TrackingStep.StepState.DONE),
                TrackingStep("Cancelado", "El pedido ha sido cancelado", TrackingStep.StepState.ACTIVE),
                TrackingStep("En camino", "No aplicable", TrackingStep.StepState.PENDING),
                TrackingStep("Entregado", "No aplicable", TrackingStep.StepState.PENDING)
            )
            EstadoPedido.NO_ENTREGADO -> listOf(
                TrackingStep("Pedido recibido", "Confirmado", TrackingStep.StepState.DONE),
                TrackingStep("Repartidor asignado", "Pedido preparado", TrackingStep.StepState.DONE),
                TrackingStep("En camino a tu domicilio", "Llegó al domicilio", TrackingStep.StepState.DONE),
                TrackingStep("No entregado", "No se pudo completar la entrega", TrackingStep.StepState.ACTIVE)
            )
        }
    }
    // CU-007: cada paso cumplido muestra la hora real registrada en el historial (RN-013).
    val steps = remember(pasos, historialPedido) {
        pasos.map { paso ->
            val estado = when {
                paso.title.startsWith("Pedido recibido") -> listOf("pendiente_programado", "pendiente")
                paso.title.startsWith("Repartidor asignado") -> listOf("asignado")
                paso.title.startsWith("En camino") -> listOf("en_camino")
                paso.title == "Entregado" -> listOf("entregado")
                paso.title == "No entregado" -> listOf("no_entregado")
                paso.title == "Cancelado" -> listOf("cancelado")
                else -> emptyList()
            }
            val fecha = historialPedido.orEmpty().firstOrNull { it.estado.lowercase() in estado }?.fecha
            if (paso.state == TrackingStep.StepState.PENDING || fecha.isNullOrBlank()) paso
            else {
                val fechaTxt = fechaCdmx(fecha)?.diaMesHoraLegible()
                    ?: if (fecha.length >= 16) "${fecha.substring(8, 10)}/${fecha.substring(5, 7)} ${fecha.substring(11, 16)}" else fecha
                paso.copy(time = "$fechaTxt · ${paso.time}")
            }
        }
    }

    // Simulador FakeGPS demostrativo para la vista del cliente.
    var posicionDemo by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var simulandoDemo by remember { mutableStateOf(false) }
    var pasoDemo by remember { mutableStateOf(0) }
    var totalPasosDemo by remember { mutableStateOf(0) }

    LaunchedEffect(order.estado) {
        if (order.estado in setOf(EstadoPedido.ENTREGADO, EstadoPedido.NO_ENTREGADO, EstadoPedido.CANCELADO)) {
            simulandoDemo = false
            posicionDemo = null
        }
    }

    val latDestino = pedido.latEntrega
    val lonDestino = pedido.lonEntrega
    val ubicacionPurificadora = remember(pedido, purificadoras, perfilPurificadora, order.purificadoraName) {
        val idNeg = pedido.idNegocioSolicitado
        val dirPerfil = perfilPurificadora?.takeIf {
            (idNeg != null && it.id == idNeg) || it.nombreComercial.equals(order.purificadoraName, ignoreCase = true)
        }?.direccionObj
        if (dirPerfil?.lat != null && dirPerfil.lon != null && (dirPerfil.lat != 0.0 || dirPerfil.lon != 0.0)) {
            dirPerfil.lat to dirPerfil.lon
        } else {
            val neg = purificadoras.firstOrNull {
                (idNeg != null && it.id == idNeg) || it.nombreComercial.equals(order.purificadoraName, ignoreCase = true)
            } ?: purificadoras.firstOrNull()
            if (neg?.lat != null && neg.lon != null && (neg.lat != 0.0 || neg.lon != 0.0)) {
                neg.lat to neg.lon
            } else null
        }
    }
    var rutaDemoAEstrella by remember(order.id) { mutableStateOf<List<Pair<Double, Double>>>(emptyList()) }
    LaunchedEffect(posicionDemo != null, simulandoDemo, ubicacionPurificadora, order.id) {
        if (!simulandoDemo && posicionDemo == null) return@LaunchedEffect
        val idInt = order.id.toIntOrNull() ?: return@LaunchedEffect
        val origen = ubicacionPurificadora ?: posicionDemo ?: return@LaunchedEffect
        if (rutaDemoAEstrella.size >= 2) {
            val ini = rutaDemoAEstrella.first()
            if (com.htogo.app.data.location.distanciaM(origen.first, origen.second, ini.first, ini.second) <= 240.0) {
                return@LaunchedEffect
            }
        }
        val calculada = clienteViewModel.obtenerRutaSeguimientoDesde(idInt, origen.first, origen.second)
        if (calculada.size >= 2) {
            rutaDemoAEstrella = calculada
        }
    }

    val rutaBaseDemo = remember(rutaDemoAEstrella, rutaSeguimiento, latDestino, lonDestino, ubicacionRepartidor, ubicacionPurificadora) {
        val origenRef = ubicacionPurificadora ?: ubicacionRepartidor?.let { it.lat to it.lon }
        val seguimientoDesdeOrigen = rutaSeguimiento.size >= 2 && (origenRef == null ||
            com.htogo.app.data.location.distanciaM(
                origenRef.first, origenRef.second,
                rutaSeguimiento.first().first, rutaSeguimiento.first().second
            ) <= 240.0)
        when {
            rutaDemoAEstrella.size >= 2 -> rutaDemoAEstrella
            seguimientoDesdeOrigen -> rutaSeguimiento
            latDestino != null && lonDestino != null ->
                com.htogo.app.data.location.generarRutaDemoCalles(
                    destLat = latDestino,
                    destLon = lonDestino,
                    origenLat = origenRef?.first,
                    origenLon = origenRef?.second
                )
            else -> emptyList()
        }
    }

    LaunchedEffect(simulandoDemo, latDestino, lonDestino) {
        if (!simulandoDemo || latDestino == null || lonDestino == null) return@LaunchedEffect
        var baseParaPasos = rutaBaseDemo
        val idInt = order.id.toIntOrNull()
        val origenPurif = ubicacionPurificadora
        if (idInt != null && origenPurif != null && rutaDemoAEstrella.size < 2) {
            val recienCalculada = clienteViewModel.obtenerRutaSeguimientoDesde(idInt, origenPurif.first, origenPurif.second)
            if (recienCalculada.size >= 2) {
                rutaDemoAEstrella = recienCalculada
                baseParaPasos = recienCalculada
            }
        }
        val pasosRuta = com.htogo.app.data.location.interpolarPasosRuta(
            ruta = baseParaPasos,
            destLat = latDestino,
            destLon = lonDestino,
            pasoMetros = 45.0
        )
        totalPasosDemo = pasosRuta.size
        var idx = if (pasoDemo in 0 until pasosRuta.lastIndex) pasoDemo else 0
        while (simulandoDemo && idx < pasosRuta.size) {
            pasoDemo = idx
            posicionDemo = pasosRuta[idx]
            idx++
            if (idx < pasosRuta.size) {
                kotlinx.coroutines.delay(1_400L)
            }
        }
        if (idx >= pasosRuta.size) {
            simulandoDemo = false
            mostrarAviso("Recorrido FakeGPS completado: repartidor en el domicilio")
        }
    }

    var isSheetMinimized by remember { mutableStateOf(false) }
    val navBarBottomMapa = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    BoxWithConstraints(Modifier.fillMaxSize().background(HToGoColors.Background)) {
        val screenHeight = maxHeight
        val margenInferiorMapa by animateDpAsState(
            targetValue = if (isSheetMinimized) (88.dp + navBarBottomMapa) else (screenHeight * 0.65f + navBarBottomMapa),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "margenInferiorCliente"
        )
        val pedidoCerrado = order.estado in setOf(EstadoPedido.ENTREGADO, EstadoPedido.NO_ENTREGADO, EstadoPedido.CANCELADO)
        val latEfectivaRepartidor = if (pedidoCerrado) null else (posicionDemo?.first ?: ubicacionRepartidor?.lat ?: rutaSeguimiento.firstOrNull()?.first)
        val lonEfectivaRepartidor = if (pedidoCerrado) null else (posicionDemo?.second ?: ubicacionRepartidor?.lon ?: rutaSeguimiento.firstOrNull()?.second)
        val puntosRutaMapa = when {
            pedidoCerrado -> emptyList()
            posicionDemo != null && rutaBaseDemo.size >= 2 -> rutaBaseDemo
            order.estado == EstadoPedido.EN_CAMINO || order.estado == EstadoPedido.ASIGNADO -> {
                val seguimientoCercano = rutaSeguimiento.size >= 2 && (
                    latEfectivaRepartidor == null || lonEfectivaRepartidor == null ||
                        rutaSeguimiento.any {
                            com.htogo.app.data.location.distanciaM(latEfectivaRepartidor, lonEfectivaRepartidor, it.first, it.second) <= 450.0
                        }
                    )
                if (seguimientoCercano) {
                    rutaSeguimiento
                } else if (latDestino != null && lonDestino != null && latEfectivaRepartidor != null && lonEfectivaRepartidor != null) {
                    com.htogo.app.data.location.generarRutaDemoCalles(
                        destLat = latDestino,
                        destLon = lonDestino,
                        origenLat = latEfectivaRepartidor,
                        origenLon = lonEfectivaRepartidor
                    )
                } else {
                    rutaSeguimiento
                }
            }
            else -> emptyList()
        }
        if (latDestino != null && lonDestino != null) {
            // Mapa real: domicilio + repartidor en vivo (WebSocket, con consultas de respaldo) o FakeGPS.
            OsmRouteMapView(
                originLat = latEfectivaRepartidor,
                originLon = lonEfectivaRepartidor,
                destLat = latDestino,
                destLon = lonDestino,
                routePoints = puntosRutaMapa,
                seguirRepartidor = true,
                repartidorLabel = if (posicionDemo != null) "Tu repartidor (FakeGPS)" else "Tu repartidor",
                avisoSinUbicacion = null, // el aviso del cliente se muestra abajo (CU-006 S1)
                estadoPedido = if (posicionDemo != null && !pedidoCerrado) "en_camino" else pedido.estado,
                margenInferior = margenInferiorMapa, // centra arriba del panel del pedido (con brújula Norte + Centrar como el repartidor)
                baseLat = if (pedidoCerrado) null else ubicacionPurificadora?.first,
                baseLon = if (pedidoCerrado) null else ubicacionPurificadora?.second,
                baseLabel = order.purificadoraName,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            MapMock(Modifier.fillMaxSize(), purificadoraName = order.purificadoraName)
        }

        Box(Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
            FloatingTopBar(
                orderId = order.id,
                onBack = onBack,
                enModoDemo = posicionDemo != null || simulandoDemo,
                simulandoRecorrido = simulandoDemo,
                onAlternarRecorridoDemo = {
                    if (simulandoDemo) {
                        simulandoDemo = false
                        mostrarAviso("Simulación FakeGPS en pausa")
                    } else {
                        pasoDemo = 0
                        simulandoDemo = true
                        mostrarAviso("Iniciando recorrido FakeGPS por la ruta...")
                    }
                },
                onSaltarInicioDemo = {
                    if (latDestino != null && lonDestino != null) {
                        simulandoDemo = false
                        pasoDemo = 0
                        val inicio = ubicacionPurificadora ?: rutaBaseDemo.firstOrNull() ?: (latDestino + 0.0085 to lonDestino - 0.0065)
                        posicionDemo = inicio
                        mostrarAviso("FakeGPS: repartidor en la purificadora (${order.purificadoraName})")
                    }
                },
                onSaltarLlegadaDemo = {
                    if (latDestino != null && lonDestino != null) {
                        simulandoDemo = false
                        posicionDemo = (latDestino + 0.00010) to (lonDestino + 0.00007)
                        mostrarAviso("FakeGPS: repartidor afuera del domicilio (~15 m)")
                    }
                },
                onDesactivarDemo = {
                    simulandoDemo = false
                    pasoDemo = 0
                    posicionDemo = null
                    mostrarAviso("Volviendo al rastreo en tiempo real")
                }
            )
        }

        if (posicionDemo != null || simulandoDemo) {
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = Color(0xFF7C3AED),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 66.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val etiqueta = if (simulandoDemo && totalPasosDemo > 0) {
                        "🛰️ FakeGPS (${pasoDemo + 1}/$totalPasosDemo)"
                    } else {
                        "🛰️ Modo Demo GPS"
                    }
                    Text(etiqueta, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (simulandoDemo) "⏸" else "▶",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { simulandoDemo = !simulandoDemo }
                    )
                    Text(
                        "✕",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            simulandoDemo = false
                            pasoDemo = 0
                            posicionDemo = null
                        }
                    )
                }
            }
        }

        // CU-006 S1: en camino pero sin posición tras dar margen a la carga inicial, o sin cambios en más de 2 minutos.
        if (order.estado == EstadoPedido.EN_CAMINO && posicionDemo == null) {
            val pantallaAbiertaMs = remember(order.id) { ahoraMs() }
            var ahoraMs by remember(order.id) { mutableStateOf(ahoraMs()) }
            LaunchedEffect(order.id, ubicacionRepartidor == null) {
                while (true) {
                    kotlinx.coroutines.delay(if (ubicacionRepartidor == null) 4_000L else 15_000L)
                    ahoraMs = ahoraMs()
                }
            }
            val u = ubicacionRepartidor
            val aviso = when {
                repartidorLlego -> null // ya está en el domicilio: se muestra el aviso de llegada
                u == null && latEfectivaRepartidor == null && ahoraMs - pantallaAbiertaMs >= 4_000L ->
                    "Ubicación del repartidor no disponible por ahora"
                u != null && ahoraMs - u.recibidaMs > 120_000 ->
                    "Última ubicación hace ${(ahoraMs - u.recibidaMs) / 60_000} min"
                else -> null
            }
            if (aviso != null) {
                com.htogo.app.ui.components.AvisoMapa(
                    aviso,
                    modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 112.dp, start = 16.dp, end = 64.dp)
                )
            }
        }

        var mostrarDialogCancelar by remember { mutableStateOf(false) }

        if (mostrarDialogCancelar) {
            AlertDialog(
                onDismissRequest = { mostrarDialogCancelar = false },
                icon = { Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("¿Cancelar pedido?", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "¿Estás seguro de que deseas cancelar este pedido? Solo puedes cancelarlo mientras ningún repartidor lo haya tomado."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            mostrarDialogCancelar = false
                            val idInt = order.id.toIntOrNull()
                            if (idInt != null) {
                                clienteViewModel.cancelarPedido(
                                    id = idInt,
                                    onSuccess = {
                                        mostrarAviso("Pedido cancelado")
                                    },
                                    // CU-005 S1: p. ej. un repartidor lo tomó mientras se confirmaba.
                                    onError = { msg ->
                                        mostrarAviso(msg, largo = true)
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Sí, cancelar pedido", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarDialogCancelar = false }) {
                        Text("No, mantener pedido")
                    }
                }
            )
        }

        // CU-012: el repartidor pulsó "Llegué al destino". Primero una tarjeta animada; al
        // cerrarla queda un recordatorio arriba que la vuelve a abrir.
        val avisoLlegada = order.estado == EstadoPedido.EN_CAMINO && repartidorLlego
        var llegadaVista by rememberSaveable(order.id) { mutableStateOf(false) }
        if (avisoLlegada && llegadaVista) {
            AvisoRepartidorAfuera(
                Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 70.dp)
                    .clickable { llegadaVista = false }
            )
        }

        Box(Modifier.align(Alignment.BottomCenter)) {
            val kmRestantesVivo = remember(
                pedidoCerrado,
                order.estado,
                posicionDemo,
                latEfectivaRepartidor,
                lonEfectivaRepartidor,
                puntosRutaMapa,
                latDestino,
                lonDestino,
                distanciaRutaKm
            ) {
                if (pedidoCerrado || (order.estado != EstadoPedido.EN_CAMINO && order.estado != EstadoPedido.ASIGNADO && posicionDemo == null)) {
                    null
                } else if (latEfectivaRepartidor != null && lonEfectivaRepartidor != null && latDestino != null && lonDestino != null) {
                    if (puntosRutaMapa.size >= 2) {
                        com.htogo.app.data.location.distanciaRestanteRutaKm(
                            lat = latEfectivaRepartidor,
                            lon = lonEfectivaRepartidor,
                            ruta = puntosRutaMapa,
                            destLat = latDestino,
                            destLon = lonDestino,
                            distanciaTotalOriginalKm = if (posicionDemo != null) null else distanciaRutaKm
                        )
                    } else {
                        (com.htogo.app.data.location.distanciaM(
                            latEfectivaRepartidor, lonEfectivaRepartidor, latDestino, lonDestino
                        ) * 1.25) / 1000.0
                    }
                } else {
                    distanciaRutaKm
                }
            }
            val llegoEfectivo = !pedidoCerrado && (
                repartidorLlego || (posicionDemo != null && kmRestantesVivo != null && kmRestantesVivo <= 0.030)
            )
            val etaResumen = when {
                pedidoCerrado -> null
                llegoEfectivo -> "¡Ya llegó afuera!"
                order.estado == EstadoPedido.ASIGNADO && posicionDemo == null ->
                    kmRestantesVivo?.let { "~${com.htogo.app.data.location.estimarMinutosRuta(it).coerceAtLeast(1) + 2} min" }
                (order.estado == EstadoPedido.EN_CAMINO || posicionDemo != null) && kmRestantesVivo != null ->
                    if (kmRestantesVivo <= 0.08) "< 1 min · ${com.htogo.app.data.location.formatearDistanciaCorta(kmRestantesVivo)}"
                    else "~${com.htogo.app.data.location.estimarMinutosRuta(kmRestantesVivo)} min · ${com.htogo.app.data.location.formatearDistanciaCorta(kmRestantesVivo)}"
                else -> null
            }
            val stepsEnVivo = remember(steps, order.estado, kmRestantesVivo, llegoEfectivo, posicionDemo) {
                steps.map { s ->
                    when {
                        s.title.startsWith("Repartidor asignado") && s.state == TrackingStep.StepState.ACTIVE && kmRestantesVivo != null -> {
                            val minPrep = com.htogo.app.data.location.estimarMinutosRuta(kmRestantesVivo).coerceAtLeast(1) + 2
                            s.copy(time = s.time.replace("Preparando pedido", "Preparando pedido · llega en ~$minPrep min"))
                        }
                        s.title.startsWith("En camino") && (s.state == TrackingStep.StepState.ACTIVE || posicionDemo != null) -> {
                            val detalleRuta = when {
                                llegoEfectivo -> "¡Llegó! Está afuera de tu domicilio"
                                kmRestantesVivo != null && kmRestantesVivo <= 0.08 ->
                                "Llegando a tu domicilio (< 1 min · ${com.htogo.app.data.location.formatearDistanciaCorta(kmRestantesVivo)})"
                                kmRestantesVivo != null ->
                                "En ruta · llega en ~${com.htogo.app.data.location.estimarMinutosRuta(kmRestantesVivo)} min (${com.htogo.app.data.location.formatearDistanciaCorta(kmRestantesVivo)})"
                                else -> "En ruta a tu casa"
                            }
                            val prefijoFecha = s.time.substringBefore(" · ", missingDelimiterValue = "")
                            s.copy(
                                state = if (!pedidoCerrado && posicionDemo != null && order.estado != EstadoPedido.EN_CAMINO)
                                    TrackingStep.StepState.ACTIVE else s.state,
                                time = if (prefijoFecha.isNotBlank() && s.time.contains(" · ")) "$prefijoFecha · $detalleRuta" else detalleRuta
                            )
                        }
                        else -> s
                    }
                }
            }

            TrackingSheet(
                order = order,
                steps = stepsEnVivo,
                etaResumen = etaResumen,
                maxScreenHeight = screenHeight,
                isMinimized = isSheetMinimized,
                onMinimizedChange = { isSheetMinimized = it },
                onPedidoEntregado = onPedidoEntregado,
                onAbrirPurificadora = onAbrirPurificadora,
                onCancelarPedido = { mostrarDialogCancelar = true }
            )
        }

        // Encima de todo: tarjeta animada al llegar el aviso.
        if (avisoLlegada && !llegadaVista) {
            RepartidorLlegoOverlay(
                nombreRepartidor = order.driverName,
                telefono = order.driverPhone,
                onCerrar = { llegadaVista = true }
            )
        }
    }
}

/** CU-012: aviso a pantalla completa cuando el repartidor llega, con anillos que laten. */
@Composable
private fun RepartidorLlegoOverlay(nombreRepartidor: String, telefono: String?, onCerrar: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val escala by animateFloatAsState(
        if (visible) 1f else 0.6f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "escalaLlegada"
    )
    val opacidad by animateFloatAsState(if (visible) 1f else 0f, tween(250), label = "opacidadLlegada")
    val pulso = rememberInfiniteTransition(label = "pulsoLlegada")
    val anillo by pulso.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "anilloLlegada"
    )
    val rebote by pulso.animateFloat(
        0f, -8f, infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "reboteLlegada"
    )
    val uriHandler = LocalUriHandler.current

    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = opacidad }
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onCerrar),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 12.dp,
            modifier = Modifier.padding(horizontal = 28.dp).fillMaxWidth()
                .graphicsLayer { scaleX = escala; scaleY = escala }
                .clickable(enabled = false) {} // los toques en la tarjeta no la cierran
        ) {
            Column(
                Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                    // Dos anillos desfasados que crecen y se desvanecen.
                    listOf(anillo, (anillo + 0.5f) % 1f).forEach { t ->
                        Box(
                            Modifier.size(150.dp)
                                .graphicsLayer { scaleX = 0.45f + 0.55f * t; scaleY = 0.45f + 0.55f * t; alpha = 1f - t }
                                .clip(CircleShape)
                                .background(HToGoColors.AccentEmerald.copy(alpha = 0.35f))
                        )
                    }
                    Box(
                        Modifier.size(78.dp).graphicsLayer { translationY = rebote.dp.toPx() }
                            .clip(CircleShape).background(HToGoColors.AccentEmerald),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.LocalShipping, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "¡Tu repartidor llegó!",
                    fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = HToGoColors.TextPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "$nombreRepartidor ya está afuera de tu domicilio con tu pedido. Sal a recibirlo.",
                    fontSize = 16.sp, color = HToGoColors.TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 22.sp
                )
                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = onCerrar,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.AccentEmerald)
                ) {
                    Text("¡Voy para allá!", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                if (telefono != null) {
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = { runCatching { uriHandler.openUri("tel:$telefono") } }) {
                        Icon(Icons.Filled.Call, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Llamar al repartidor", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/** Recordatorio compacto tras cerrar la tarjeta de llegada; al tocarlo se vuelve a abrir. */
@Composable
private fun AvisoRepartidorAfuera(modifier: Modifier = Modifier) {
    val pulso = rememberInfiniteTransition(label = "pulsoAfuera")
    val alfa by pulso.animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "alfaAfuera"
    )
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = HToGoColors.AccentEmerald,
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).graphicsLayer { alpha = alfa }.clip(CircleShape).background(Color.White))
            Spacer(Modifier.width(10.dp))
            Text("Tu repartidor está afuera", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}
