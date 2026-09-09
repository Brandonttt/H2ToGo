package com.htogo.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htogo.app.ui.components.EstadoPedido
import com.htogo.app.ui.components.EstadoPedidoChip
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
    val total: Int
)

private val SAMPLE_ORDER = TrackedOrder(
    id = "HG-1287", estado = EstadoPedido.EN_CAMINO,
    driverName = "Carlos Mendoza", driverInitials = "CM",
    purificadoraName = "Aguas Del Valle",
    driverVehicle = "Bicicleta de carga · Placa BJ-238",
    items = "3 × Ciel 20 L",
    address = "Casa · Insurgentes Sur 1234",
    total = 125
)

private val SAMPLE_STEPS = listOf(
    TrackingStep("Pedido recibido",         "9:32 AM · Aguas Del Valle confirmó", TrackingStep.StepState.DONE),
    TrackingStep("Repartidor asignado",     "9:35 AM · Carlos cargó 3 garrafones", TrackingStep.StepState.DONE),
    TrackingStep("En camino a tu domicilio","En curso",                            TrackingStep.StepState.ACTIVE),
    TrackingStep("Entregado",               "Pendiente",                           TrackingStep.StepState.PENDING)
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
                    Text("Carlos M.",
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
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 110.dp)
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
                Text(" · Carlos viene en camino",
                    fontSize = 13.sp, color = HToGoColors.TextSecondary)
            }
        }

        Column(
            Modifier.align(Alignment.TopEnd).padding(top = 140.dp, end = 14.dp),
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
private fun FloatingTopBar(orderId: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
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
    }
}

@Composable
private fun TrackingSheet(
    order: TrackedOrder,
    steps: List<TrackingStep>,
    onPedidoEntregado: () -> Unit,
    onAbrirPurificadora: () -> Unit
) {
    val isEntregado = order.estado == EstadoPedido.ENTREGADO
    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = HToGoColors.Surface,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.62f)
    ) {
        Column(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 20.dp)) {
            Box(
                Modifier.align(Alignment.CenterHorizontally)
                    .width(44.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(HToGoColors.OutlineSoft)
            )
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EstadoPedidoChip(order.estado)
                }
                Spacer(Modifier.height(14.dp))

                DriverCard(order, onAbrirPurificadora)
                Spacer(Modifier.height(14.dp))

                Timeline(steps)
                Spacer(Modifier.height(14.dp))

                OrderRow("Pedido", order.items)
                OrderRow("Domicilio", order.address)
                Divider(Modifier.padding(vertical = 8.dp), color = HToGoColors.OutlineSoft)
                OrderRow("Total a pagar", "$${order.total} MXN",
                    valueColor = HToGoColors.Primary, bold = true)
                Spacer(Modifier.height(8.dp))

                if (isEntregado) {
                    Button(
                        onClick = onPedidoEntregado,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.StatusEntregado)
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, tint = Color.White,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Confirmar pedido entregado",
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
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
                        Text("Cancelar pedido",
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Lock, null, tint = HToGoColors.TextTertiary,
                            modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Ya no puedes cancelar: tu pedido está en camino",
                            fontSize = 11.sp, color = HToGoColors.TextTertiary)
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
                    "${order.purificadoraName} · ${order.driverName.substringBefore(' ')} ${order.driverName.substringAfter(' ').first()}.",
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
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = CircleShape, color = HToGoColors.Primary,
                modifier = Modifier.size(42.dp).clickable { }
            ) { Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Call, null, tint = Color.White, modifier = Modifier.size(20.dp))
            } }
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
    onBack: () -> Unit = {},
    onPedidoEntregado: () -> Unit = {},
    onAbrirPurificadora: () -> Unit = {}
) {
    val order = SAMPLE_ORDER
    val steps = SAMPLE_STEPS
    Box(Modifier.fillMaxSize().background(HToGoColors.Background)) {
        MapMock(Modifier.fillMaxSize(), purificadoraName = order.purificadoraName)

        Box(Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
            FloatingTopBar(order.id, onBack)
        }

        Box(Modifier.align(Alignment.BottomCenter)) {
            TrackingSheet(
                order = order,
                steps = steps,
                onPedidoEntregado = onPedidoEntregado,
                onAbrirPurificadora = onAbrirPurificadora
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 900, name = "07 · Seguimiento del pedido")
@Composable
fun SeguimientoPedidoScreenPreview() {
    HToGoTheme { SeguimientoPedidoScreen() }
}
