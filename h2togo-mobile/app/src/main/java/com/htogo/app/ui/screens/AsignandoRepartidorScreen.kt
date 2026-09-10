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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme

@Composable
fun AsignandoRepartidorScreen(
    onCancel: () -> Unit = {},
    onAsignado: () -> Unit = {}
) {
    AsignandoRepartidorScreen(
        modoDirecto = true,
        purificadoraName = "Aguas Del Valle",
        candidatasCount = 4,
        onCancel = onCancel,
        onAsignado = onAsignado
    )
}

@Composable
fun AsignandoRepartidorScreen(
    modoDirecto: Boolean,
    purificadoraName: String = "Aguas Del Valle",
    candidatasCount: Int = 4,
    orderId: String = "HG-1287",
    items: String = "3 × Ciel 20 L",
    address: String = "Casa · Insurgentes Sur 1234",
    total: Int = 125,
    precioMax: Int = 60,
    onBack: () -> Unit = {},
    onCancel: () -> Unit = {},
    onAsignado: () -> Unit = {}
) {
    Box(Modifier.fillMaxSize().background(HToGoColors.Background)) {
        SearchingMap(
            modifier = Modifier.fillMaxSize(),
            modoDirecto = modoDirecto,
            purificadoraName = purificadoraName
        )

        Row(
            Modifier.fillMaxWidth().align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconCircle(Icons.AutoMirrored.Filled.ArrowBack, onClick = onBack)
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = Color.White, shadowElevation = 4.dp,
                modifier = Modifier.weight(1f)
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text("Tu pedido ", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = HToGoColors.TextPrimary)
                    Text("#$orderId", fontSize = 13.sp, color = HToGoColors.TextSecondary)
                }
            }
        }

        AssignSheet(
            modoDirecto = modoDirecto,
            purificadoraName = purificadoraName,
            candidatasCount = candidatasCount,
            items = items,
            address = address,
            total = total,
            precioMax = precioMax,
            onCancel = onCancel,
            onAsignado = onAsignado,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun IconCircle(icon: ImageVector, onClick: () -> Unit) {
    Surface(
        shape = CircleShape, color = Color.White, shadowElevation = 4.dp,
        modifier = Modifier.size(40.dp).clickable(onClick = onClick)
    ) { Box(contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = HToGoColors.TextPrimary)
    } }
}

@Composable
private fun SearchingMap(
    modifier: Modifier = Modifier,
    modoDirecto: Boolean,
    purificadoraName: String
) {
    val infinite = rememberInfiniteTransition(label = "search")
    val r1 by infinite.animateFloat(
        0.4f, 2f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "r1"
    )
    val r1a by infinite.animateFloat(
        0.7f, 0f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "r1a"
    )

    Box(
        modifier.background(Color(0xFFE8EEF3))
    ) {
        GenericMapBackdrop(Modifier.fillMaxSize())

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White, shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 110.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            Column {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        Modifier.size(14.dp), strokeWidth = 2.dp, color = HToGoColors.Primary
                    )
                    Spacer(Modifier.width(10.dp))
                    val bannerText = if (modoDirecto)
                        "Esperando que $purificadoraName acepte..."
                    else
                        "Buscando purificadoras con tu precio máximo..."
                    Text(bannerText,
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = HToGoColors.TextPrimary)
                }
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = HToGoColors.Primary,
                    trackColor = HToGoColors.PrimarySoft
                )
            }
        }

        Box(Modifier.align(Alignment.Center).offset(y = (-40).dp)) {
            Box(
                Modifier
                    .size(80.dp)
                    .graphicsLayer { scaleX = r1; scaleY = r1; alpha = r1a }
                    .clip(CircleShape)
                    .border(3.dp, HToGoColors.Primary, CircleShape)
            )
            Surface(
                shape = CircleShape, color = Color.White, shadowElevation = 12.dp,
                modifier = Modifier.size(64.dp).align(Alignment.Center)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.WaterDrop, null, tint = HToGoColors.Primary,
                        modifier = Modifier.size(32.dp))
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp), color = Color.White, shadowElevation = 4.dp,
            modifier = Modifier.align(Alignment.TopEnd)
                .padding(top = 220.dp, end = 14.dp).size(42.dp).clickable {}
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.MyLocation, null, tint = HToGoColors.TextPrimary,
                    modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun GenericMapBackdrop(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val w = size.width
        val h = size.height
        val land = Color(0xFFE8EEF3)
        val park = Color(0xFFC8E6C9)
        val water = Color(0xFFB3D9E8)
        val road = Color.White
        val roadEdge = Color(0xFFD6DBE0)
        val building = Color(0xFFDDE3E8)

        drawRect(land)

        drawRect(
            color = water,
            topLeft = androidx.compose.ui.geometry.Offset(0f, h * 0.78f),
            size = androidx.compose.ui.geometry.Size(w, h * 0.22f)
        )
        drawRect(
            color = park,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.62f, h * 0.10f),
            size = androidx.compose.ui.geometry.Size(w * 0.30f, h * 0.18f)
        )
        drawRect(
            color = park,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.05f, h * 0.55f),
            size = androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.14f)
        )

        val buildings = listOf(
            Triple(0.04f, 0.08f, 0.20f to 0.10f),
            Triple(0.28f, 0.08f, 0.18f to 0.12f),
            Triple(0.04f, 0.22f, 0.14f to 0.10f),
            Triple(0.22f, 0.24f, 0.20f to 0.10f),
            Triple(0.48f, 0.10f, 0.10f to 0.18f),
            Triple(0.50f, 0.32f, 0.14f to 0.12f),
            Triple(0.70f, 0.34f, 0.18f to 0.10f),
            Triple(0.04f, 0.38f, 0.20f to 0.12f),
            Triple(0.30f, 0.40f, 0.16f to 0.10f),
            Triple(0.50f, 0.50f, 0.20f to 0.14f),
            Triple(0.76f, 0.55f, 0.18f to 0.12f),
            Triple(0.30f, 0.66f, 0.18f to 0.10f),
        )
        buildings.forEach { (x, y, sz) ->
            val (sw, sh) = sz
            drawRect(
                color = building,
                topLeft = androidx.compose.ui.geometry.Offset(w * x, h * y),
                size = androidx.compose.ui.geometry.Size(w * sw, h * sh)
            )
        }

        val hLines = listOf(0.20f, 0.36f, 0.52f, 0.72f)
        hLines.forEach { ratio ->
            drawLine(
                color = roadEdge,
                start = androidx.compose.ui.geometry.Offset(0f, h * ratio - 8f),
                end = androidx.compose.ui.geometry.Offset(w, h * ratio - 8f),
                strokeWidth = 1f
            )
            drawLine(
                color = road,
                start = androidx.compose.ui.geometry.Offset(0f, h * ratio),
                end = androidx.compose.ui.geometry.Offset(w, h * ratio),
                strokeWidth = 14f
            )
            drawLine(
                color = roadEdge,
                start = androidx.compose.ui.geometry.Offset(0f, h * ratio + 8f),
                end = androidx.compose.ui.geometry.Offset(w, h * ratio + 8f),
                strokeWidth = 1f
            )
        }
        val vLines = listOf(0.24f, 0.46f, 0.68f)
        vLines.forEach { ratio ->
            drawLine(
                color = roadEdge,
                start = androidx.compose.ui.geometry.Offset(w * ratio - 8f, 0f),
                end = androidx.compose.ui.geometry.Offset(w * ratio - 8f, h),
                strokeWidth = 1f
            )
            drawLine(
                color = road,
                start = androidx.compose.ui.geometry.Offset(w * ratio, 0f),
                end = androidx.compose.ui.geometry.Offset(w * ratio, h),
                strokeWidth = 12f
            )
            drawLine(
                color = roadEdge,
                start = androidx.compose.ui.geometry.Offset(w * ratio + 8f, 0f),
                end = androidx.compose.ui.geometry.Offset(w * ratio + 8f, h),
                strokeWidth = 1f
            )
        }
    }
}

@Composable
private fun AssignSheet(
    modoDirecto: Boolean,
    purificadoraName: String,
    candidatasCount: Int,
    items: String,
    address: String,
    total: Int,
    precioMax: Int,
    onCancel: () -> Unit,
    onAsignado: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = HToGoColors.Surface,
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth().fillMaxHeight(0.62f)
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
                if (!modoDirecto) {
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = HToGoColors.AccentAmber.copy(alpha = 0.10f)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.AttachMoney, null, tint = Color(0xFFB45309),
                                modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Pedido abierto · Precio máx \$$precioMax c/u",
                                fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFB45309))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = HToGoColors.StatusAsignado.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, HToGoColors.StatusAsignado.copy(alpha = 0.20f)
                    )
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                                .background(HToGoColors.StatusAsignado.copy(alpha = 0.13f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (modoDirecto) Icons.Filled.Search else Icons.Filled.Radar,
                                null, tint = HToGoColors.StatusAsignado
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            val heroTitle = if (modoDirecto)
                                "Esperando confirmación..."
                            else
                                "Buscando purificadoras..."
                            val heroSub = if (modoDirecto)
                                "$purificadoraName está revisando tu pedido"
                            else
                                "$candidatasCount purificadoras cumplen tu precio máximo"
                            Text(heroTitle,
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                color = HToGoColors.TextPrimary)
                            Text(heroSub,
                                fontSize = 12.sp, color = HToGoColors.TextSecondary)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = HToGoColors.Background,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        val firstTitle = if (modoDirecto)
                            "Pedido recibido · $purificadoraName"
                        else
                            "Pedido lanzado · Buscando entre $candidatasCount purificadoras"
                        val firstTime = if (modoDirecto)
                            "Hace unos segundos · $purificadoraName"
                        else
                            "Hace unos segundos"
                        AssignTLStep(firstTitle, firstTime, done = true, last = false)
                        AssignTLStep(
                            if (modoDirecto) "Esperando que la purificadora acepte"
                            else "La primera purificadora que acepte se queda el pedido",
                            "En curso",
                            active = true, last = false
                        )
                        AssignTLStep("En camino a tu domicilio", "Pendiente",
                            pending = true, last = false)
                        AssignTLStep("Entregado", "Pendiente",
                            pending = true, last = true)
                    }
                }

                Spacer(Modifier.height(14.dp))
                OrderRow("Pedido", items)
                if (!modoDirecto) {
                    OrderRow("Precio máx", "\$$precioMax c/u")
                }
                OrderRow("Domicilio", address)
                Divider(Modifier.padding(vertical = 8.dp), color = HToGoColors.OutlineSoft)
                OrderRow(
                    if (modoDirecto) "Total a pagar" else "Total máximo a pagar",
                    "$$total MXN",
                    valueColor = HToGoColors.Primary, bold = true
                )

                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, HToGoColors.StatusCancelado),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HToGoColors.StatusCancelado)
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
                    Icon(Icons.Filled.Verified, null, tint = HToGoColors.StatusEntregado,
                        modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    val hint = if (modoDirecto)
                        "Puedes cancelar sin costo mientras la purificadora confirma"
                    else
                        "Puedes cancelar sin costo mientras ninguna purificadora acepta"
                    Text(hint, fontSize = 11.sp, color = HToGoColors.TextSecondary)
                }

                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = onAsignado,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Simular asignación",
                        fontSize = 12.sp, color = HToGoColors.TextTertiary,
                        fontWeight = FontWeight.Medium)
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
                    Modifier.size(18.dp).clip(CircleShape).background(HToGoColors.StatusEntregado),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(11.dp)) }
                active -> Box(
                    Modifier.size(18.dp).clip(CircleShape).background(HToGoColors.StatusAsignado)
                        .border(4.dp, HToGoColors.StatusAsignado.copy(alpha = 0.25f), CircleShape)
                )
                else -> Box(Modifier.size(18.dp).clip(CircleShape).background(HToGoColors.OutlineSoft))
            }
            if (!last) {
                Box(Modifier.width(2.dp).height(28.dp)
                    .background(if (done) HToGoColors.StatusEntregado else HToGoColors.OutlineSoft))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.padding(bottom = if (last) 0.dp else 14.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = if (pending) HToGoColors.TextTertiary else HToGoColors.TextPrimary)
            Text(time, fontSize = 11.sp,
                color = if (active) HToGoColors.StatusAsignado else HToGoColors.TextSecondary,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
        }
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

@Preview(showBackground = true, widthDp = 412, heightDp = 900, name = "07a · Asignando (directo)")
@Composable
fun AsignandoRepartidorScreenDirectoPreview() {
    HToGoTheme {
        AsignandoRepartidorScreen(modoDirecto = true)
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 900, name = "07a · Asignando (abierto)")
@Composable
fun AsignandoRepartidorScreenAbiertoPreview() {
    HToGoTheme {
        AsignandoRepartidorScreen(modoDirecto = false)
    }
}
