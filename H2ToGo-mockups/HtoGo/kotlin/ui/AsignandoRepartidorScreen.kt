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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ----------------------------- Theme tokens ----------------------------- */

private object HtgColor {
    val Primary    = Color(0xFF0077B6)
    val PrimarySoft= Color(0xFFCAF0F8)
    val Bg         = Color(0xFFF8FAFC)
    val Surface    = Color(0xFFFFFFFF)
    val Text       = Color(0xFF0F172A)
    val Text2      = Color(0xFF64748B)
    val Text3      = Color(0xFF94A3B8)
    val Outline    = Color(0xFFE2E8F0)
    val Asignado   = Color(0xFFF59E0B)
    val Entregado  = Color(0xFF10B981)
    val Cancelado  = Color(0xFFEF4444)
}

/* ------------------------------- Screen -------------------------------- */

@Composable
fun AsignandoRepartidorScreen(
    orderId: String = "HG-1287",
    items: String = "3 × Ciel 20 L",
    address: String = "Casa · Insurgentes Sur 1234",
    total: Int = 125,
    onBack: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    Box(Modifier.fillMaxSize().background(HtgColor.Bg)) {
        // Mapa de fondo con animación de búsqueda
        SearchingMap(Modifier.fillMaxSize())

        // Top bar flotante
        Row(
            Modifier.fillMaxWidth().align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconCircle(Icons.Filled.ArrowBack, onClick = onBack)
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = Color.White, shadowElevation = 4.dp,
                modifier = Modifier.weight(1f)
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text("Tu pedido ", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = HtgColor.Text)
                    Text("#$orderId", fontSize = 13.sp, color = HtgColor.Text2)
                }
            }
            IconCircle(Icons.Filled.Share, onClick = {})
        }

        // Bottom sheet
        AssignSheet(
            items = items, address = address, total = total,
            onCancel = onCancel,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun IconCircle(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        shape = CircleShape, color = Color.White, shadowElevation = 4.dp,
        modifier = Modifier.size(40.dp).clickable(onClick = onClick)
    ) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = HtgColor.Text) } }
}

/* ----------------------------- Searching map --------------------------- */

@Composable
private fun SearchingMap(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "search")
    val r1 by infinite.animateFloat(
        0.4f, 2f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "r1"
    )
    val r1a by infinite.animateFloat(
        0.7f, 0f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "r1a"
    )

    Box(
        modifier.background(
            Brush.linearGradient(listOf(Color(0xFFDCEEF7), Color(0xFFE8F5FA), Color(0xFFD6EAF4)))
        )
    ) {
        // Banner "Buscando repartidor"
        Surface(
            shape = RoundedCornerShape(99.dp),
            color = Color.White, shadowElevation = 6.dp,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 110.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    Modifier.size(14.dp), strokeWidth = 2.dp, color = HtgColor.Primary
                )
                Spacer(Modifier.width(10.dp))
                Text("Buscando repartidor cerca de ti…",
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = HtgColor.Text)
            }
        }

        // Anillo expansivo + pin del cliente al centro
        Box(Modifier.align(Alignment.Center).offset(y = (-40).dp)) {
            Box(
                Modifier
                    .size(80.dp)
                    .graphicsLayer { scaleX = r1; scaleY = r1; alpha = r1a }
                    .clip(CircleShape)
                    .border(3.dp, HtgColor.Primary, CircleShape)
            )
            Surface(
                shape = CircleShape, color = Color.White, shadowElevation = 12.dp,
                modifier = Modifier.size(64.dp).align(Alignment.Center)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.WaterDrop, null, tint = HtgColor.Primary,
                        modifier = Modifier.size(32.dp))
                }
            }
        }

        // FAB de centrar ubicación
        Surface(
            shape = RoundedCornerShape(12.dp), color = Color.White, shadowElevation = 4.dp,
            modifier = Modifier.align(Alignment.TopEnd)
                .padding(top = 200.dp, end = 14.dp).size(42.dp).clickable {}
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.MyLocation, null, tint = HtgColor.Text,
                    modifier = Modifier.size(20.dp))
            }
        }
    }
}

/* ------------------------------ Bottom sheet --------------------------- */

@Composable
private fun AssignSheet(
    items: String, address: String, total: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = HtgColor.Surface,
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth().fillMaxHeight(0.58f)
    ) {
        Column(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 20.dp)) {
            Box(
                Modifier.align(Alignment.CenterHorizontally)
                    .width(44.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(HtgColor.Outline)
            )
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
            ) {
                // Hero estado
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = HtgColor.Asignado.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, HtgColor.Asignado.copy(alpha = 0.20f)
                    )
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                                .background(HtgColor.Asignado.copy(alpha = 0.13f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Search, null, tint = HtgColor.Asignado)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Asignando repartidor…",
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                color = HtgColor.Text)
                            Text("Buscando entre repartidores cercanos en Benito Juárez",
                                fontSize = 12.sp, color = HtgColor.Text2)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Timeline (paso 2 activo)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = HtgColor.Bg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        AssignTLStep("Pedido recibido", "Hace unos segundos · Aguas Del Valle",
                            done = true, last = false)
                        AssignTLStep("Asignando repartidor", "En curso",
                            active = true, last = false)
                        AssignTLStep("En camino a tu domicilio", "Pendiente",
                            pending = true, last = false)
                        AssignTLStep("Entregado", "Pendiente",
                            pending = true, last = true)
                    }
                }

                Spacer(Modifier.height(14.dp))
                OrderRow("Pedido", items)
                OrderRow("Domicilio", address)
                Divider(Modifier.padding(vertical = 8.dp), color = HtgColor.Outline)
                OrderRow("Total a pagar", "$$total MXN",
                    valueColor = HtgColor.Primary, bold = true)

                Spacer(Modifier.height(10.dp))
                // Botón cancelar HABILITADO
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, HtgColor.Cancelado),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HtgColor.Cancelado)
                ) {
                    Icon(Icons.Filled.Close, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Cancelar pedido",
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Verified, null, tint = HtgColor.Entregado,
                        modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Puedes cancelar sin costo mientras buscamos repartidor",
                        fontSize = 11.sp, color = HtgColor.Text2)
                }
            }
        }
    }
}

@Composable
private fun AssignTLStep(
    title: String, time: String,
    done: Boolean = false, active: Boolean = false, pending: Boolean = false,
    last: Boolean
) {
    Row {
        Column(
            Modifier.width(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                done -> Box(
                    Modifier.size(18.dp).clip(CircleShape).background(HtgColor.Entregado),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(11.dp)) }
                active -> Box(
                    Modifier.size(18.dp).clip(CircleShape).background(HtgColor.Asignado)
                        .border(4.dp, HtgColor.Asignado.copy(alpha = 0.25f), CircleShape)
                )
                else -> Box(Modifier.size(18.dp).clip(CircleShape).background(HtgColor.Outline))
            }
            if (!last) {
                Box(Modifier.width(2.dp).height(28.dp)
                    .background(if (done) HtgColor.Entregado else HtgColor.Outline))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.padding(bottom = if (last) 0.dp else 14.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = if (pending) HtgColor.Text3 else HtgColor.Text)
            Text(time, fontSize = 11.sp,
                color = if (active) HtgColor.Asignado else HtgColor.Text2,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
        }
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

@Preview(showBackground = true, widthDp = 412, heightDp = 900, name = "07a · Asignando repartidor")
@Composable
fun AsignandoRepartidorScreenPreview() {
    MaterialTheme { AsignandoRepartidorScreen() }
}
