package com.htogo.ui.screens.cliente

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ----------------------------- Theme tokens ----------------------------- */

private object HtgColor {
    val Primary       = Color(0xFF0077B6)
    val PrimaryDark   = Color(0xFF03045E)
    val PrimarySoft   = Color(0xFFCAF0F8)
    val Bg            = Color(0xFFF8FAFC)
    val Surface       = Color(0xFFFFFFFF)
    val Text          = Color(0xFF0F172A)
    val Text2         = Color(0xFF64748B)
    val Text3         = Color(0xFF94A3B8)
    val Outline       = Color(0xFFE2E8F0)
    val Pendiente     = Color(0xFF94A3B8)
    val Asignado      = Color(0xFFF59E0B)
    val EnCamino      = Color(0xFF0077B6)
    val Entregado     = Color(0xFF10B981)
    val Cancelado     = Color(0xFFEF4444)
}

/* ------------------------------- Models -------------------------------- */

enum class PedidoEstado(val label: String, val color: Color) {
    PENDIENTE("Pendiente",  HtgColor.Pendiente),
    ASIGNADO ("Asignado",   HtgColor.Asignado),
    EN_CAMINO("En camino",  HtgColor.EnCamino),
    ENTREGADO("Entregado",  HtgColor.Entregado),
    CANCELADO("Cancelado",  HtgColor.Cancelado)
}

data class TrackingStep(
    val title: String,
    val time: String,
    val state: StepState
) { enum class StepState { DONE, ACTIVE, PENDING } }

data class TrackedOrder(
    val id: String,
    val estado: PedidoEstado,
    val driverName: String,
    val driverInitials: String,
    val driverRating: Float,
    val driverCompany: String,
    val driverVehicle: String,
    val items: String,
    val address: String,
    val total: Int,
    val billPaid: Int
)

private val SAMPLE_ORDER = TrackedOrder(
    id = "HG-1287", estado = PedidoEstado.EN_CAMINO,
    driverName = "Carlos Mendoza", driverInitials = "CM",
    driverRating = 4.9f, driverCompany = "Aguas Del Valle",
    driverVehicle = "Bicicleta de carga · Placa BJ-238",
    items = "3 × Ciel 20 L",
    address = "Casa · Insurgentes Sur 1234",
    total = 125, billPaid = 200
)

private val SAMPLE_STEPS = listOf(
    TrackingStep("Pedido recibido",        "9:32 AM · Aguas Del Valle confirmó",     TrackingStep.StepState.DONE),
    TrackingStep("Repartidor asignado",    "9:35 AM · Carlos cargó 3 garrafones",   TrackingStep.StepState.DONE),
    TrackingStep("En camino a tu domicilio","En curso",                              TrackingStep.StepState.ACTIVE),
    TrackingStep("Entregado",              "Pendiente",                              TrackingStep.StepState.PENDING)
)

/* ------------------------------ Map mock ------------------------------- */

@Composable
private fun MapMock(modifier: Modifier = Modifier) {
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
        modifier
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFDCEEF7), Color(0xFFE8F5FA), Color(0xFFD6EAF4))
                )
            )
    ) {
        // Calles + bloques en Canvas
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            // Streets
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

            // Park
            drawRoundRect(
                color = Color(0xFFBFE3CB),
                topLeft = androidx.compose.ui.geometry.Offset(w * 0.55f, h * 0.18f),
                size = androidx.compose.ui.geometry.Size(w * 0.22f, h * 0.10f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f)
            )

            // Route (dashed)
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.30f, h * 0.60f)
                quadraticBezierTo(w * 0.48f, h * 0.50f, w * 0.55f, h * 0.40f)
                quadraticBezierTo(w * 0.62f, h * 0.34f, w * 0.72f, h * 0.32f)
            }
            drawPath(
                path = path,
                color = HtgColor.Primary.copy(alpha = 0.25f),
                style = Stroke(width = 12f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            drawPath(
                path = path,
                color = HtgColor.Primary,
                style = Stroke(
                    width = 8f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 18f))
                )
            )
        }

        // Driver marker @ (~30%, 60%)
        Box(
            Modifier.fillMaxSize()
        ) {
            // Posicionar relativamente con offsets
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (412 * 0.30f).dp - 23.dp, y = (520 * 0.60f).dp - 60.dp)
            ) {
                // Pulse
                Box(
                    Modifier
                        .size(46.dp)
                        .graphicsScale(pulse)
                        .clip(CircleShape)
                        .background(HtgColor.Primary.copy(alpha = pulseAlpha * 0.4f))
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
                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = HtgColor.Text)
                }
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = CircleShape,
                    color = HtgColor.Primary,
                    border = androidx.compose.foundation.BorderStroke(3.dp, Color.White),
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.DeliveryDining, null, tint = Color.White,
                            modifier = Modifier.size(24.dp))
                    }
                }
            }

            // Destination marker @ (~72%, 32%)
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (412 * 0.72f).dp - 18.dp, y = (520 * 0.32f).dp - 36.dp)
                    .size(36.dp)
                    .rotate(-45f)
                    .background(HtgColor.Entregado, RoundedCornerShape(50, 50, 50, 0))
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

        // ETA floating banner
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
                Box(Modifier.size(8.dp).clip(CircleShape).background(HtgColor.EnCamino))
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.DeliveryDining, null, tint = HtgColor.EnCamino,
                    modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Carlos viene en camino", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = HtgColor.Text)
            }
        }

        // Map fabs
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
            Icon(icon, null, tint = HtgColor.Text, modifier = Modifier.size(20.dp))
        }
    }
}

// Helper para escalado animado (evita import de graphicsLayer en cada lugar)
private fun Modifier.graphicsScale(scale: Float): Modifier =
    this.then(Modifier.graphicsLayerScale(scale))

private fun Modifier.graphicsLayerScale(scale: Float): Modifier =
    androidx.compose.ui.graphics.graphicsLayer { scaleX = scale; scaleY = scale } as Modifier

/* ------------------------------ Top bar -------------------------------- */

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
            Icon(Icons.Filled.ArrowBack, null, tint = HtgColor.Text)
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
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = HtgColor.Text)
                Text("#$orderId",
                    fontSize = 13.sp, color = HtgColor.Text2)
            }
        }
        Surface(
            shape = CircleShape, color = Color.White, shadowElevation = 4.dp,
            modifier = Modifier.size(40.dp).clickable { }
        ) { Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Share, null, tint = HtgColor.Text)
        } }
    }
}

/* ----------------------------- Bottom sheet ---------------------------- */

@Composable
private fun TrackingSheet(order: TrackedOrder, steps: List<TrackingStep>) {
    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = HtgColor.Surface,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.62f)
    ) {
        Column(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 20.dp)) {
            // Handle
            Box(
                Modifier.align(Alignment.CenterHorizontally)
                    .width(44.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(HtgColor.Outline)
            )
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
            ) {
                // Estado (sin ETA, ya viene en camino)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(order.estado)
                }
                Spacer(Modifier.height(14.dp))

                // Driver
                DriverCard(order)
                Spacer(Modifier.height(14.dp))

                // Timeline
                Timeline(steps)
                Spacer(Modifier.height(14.dp))

                // Order details
                OrderRow("Pedido", order.items)
                OrderRow("Domicilio", order.address)
                Divider(Modifier.padding(vertical = 8.dp), color = HtgColor.Outline)
                OrderRow("Total a pagar", "$${order.total} MXN", valueColor = HtgColor.Primary, bold = true)
                if (order.billPaid > order.total) {
                    OrderRow("Pagas con", "$${order.billPaid} · cambio $${order.billPaid - order.total}")
                }
                Spacer(Modifier.height(8.dp))
                // Cancelación bloqueada: el pedido ya viene en camino
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, HtgColor.Outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        disabledContainerColor = HtgColor.Bg,
                        disabledContentColor = HtgColor.Text3
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
                    Icon(Icons.Filled.Lock, null, tint = HtgColor.Text3,
                        modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ya no puedes cancelar: tu pedido está en camino",
                        fontSize = 11.sp, color = HtgColor.Text3)
                }
            }
        }
    }
}

@Composable
private fun StatusChip(estado: PedidoEstado) {
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = estado.color.copy(alpha = 0.12f)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(estado.color))
            Spacer(Modifier.width(6.dp))
            Text(estado.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = estado.color)
        }
    }
}

@Composable
private fun DriverCard(order: TrackedOrder) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = HtgColor.Bg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(HtgColor.PrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Text(order.driverInitials, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = HtgColor.Primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(order.driverName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = HtgColor.Text)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, null, tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("${order.driverRating} · ${order.driverCompany}",
                        fontSize = 12.sp, color = HtgColor.Text2)
                }
                Text(order.driverVehicle, fontSize = 11.sp, color = HtgColor.Text3)
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = CircleShape, color = HtgColor.Primary,
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
        color = HtgColor.Bg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            steps.forEachIndexed { i, step ->
                Row {
                    // Dot column
                    Column(
                        Modifier.width(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TimelineDot(step.state)
                        if (i < steps.lastIndex) {
                            Box(
                                Modifier.width(2.dp).height(28.dp)
                                    .background(
                                        if (step.state == TrackingStep.StepState.DONE) HtgColor.Entregado
                                        else HtgColor.Outline
                                    )
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.padding(bottom = if (i < steps.lastIndex) 14.dp else 0.dp)) {
                        Text(step.title,
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = if (step.state == TrackingStep.StepState.PENDING) HtgColor.Text3 else HtgColor.Text)
                        Text(step.time,
                            fontSize = 11.sp,
                            color = when (step.state) {
                                TrackingStep.StepState.ACTIVE -> HtgColor.EnCamino
                                else -> HtgColor.Text2
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
            Modifier.size(18.dp).clip(CircleShape).background(HtgColor.Entregado),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(11.dp)) }
        TrackingStep.StepState.ACTIVE -> Box(
            Modifier.size(18.dp).clip(CircleShape).background(HtgColor.EnCamino)
                .border(3.dp, HtgColor.EnCamino.copy(alpha = 0.25f), CircleShape)
        )
        TrackingStep.StepState.PENDING -> Box(
            Modifier.size(18.dp).clip(CircleShape).background(HtgColor.Outline)
        )
    }
}

@Composable
private fun OrderRow(label: String, value: String,
                    valueColor: Color = HtgColor.Text, bold: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = HtgColor.Text2)
        Text(value, fontSize = 13.sp, color = valueColor,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)
    }
}

/* ------------------------------- Screen -------------------------------- */

@Composable
fun SeguimientoPedidoScreen(
    order: TrackedOrder = SAMPLE_ORDER,
    steps: List<TrackingStep> = SAMPLE_STEPS,
    onBack: () -> Unit = {}
) {
    Box(Modifier.fillMaxSize().background(HtgColor.Bg)) {
        // Mapa fullscreen
        MapMock(Modifier.fillMaxSize())

        // Top bar flotante
        Box(Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
            FloatingTopBar(order.id, onBack)
        }

        // Bottom sheet
        Box(Modifier.align(Alignment.BottomCenter)) {
            TrackingSheet(order, steps)
        }
    }
}

/* ------------------------------- Preview ------------------------------- */

@Preview(showBackground = true, widthDp = 412, heightDp = 900, name = "07 · Seguimiento del pedido")
@Composable
fun SeguimientoPedidoScreenPreview() {
    MaterialTheme { SeguimientoPedidoScreen() }
}
