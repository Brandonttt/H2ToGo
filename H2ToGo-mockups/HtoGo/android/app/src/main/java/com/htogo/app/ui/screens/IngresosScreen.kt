package com.htogo.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htogo.app.ui.components.RepartidorBottomBar
import com.htogo.app.ui.components.RepartidorTab
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme

private enum class TabIngresos { HOY, SEMANA, MES, HISTORIAL }

private data class ResumenIngresos(
    val totalIngresos: Double,
    val entregadas: Int,
    val totalEntregas: Int,
    val noEntregadas: Int,
    val garrafonesVendidos: Int,
    val tasaEntrega: Int,
    val ingresosSemana: List<Double>,
    val totalSemana: Double,
    val entregasSemana: Int,
    val deltaSemana: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngresosScreen(
    onBack: () -> Unit = {},
    onInicio: () -> Unit = {},
    onNegocio: () -> Unit = {},
    onPerfil: () -> Unit = {}
) {
    var tab by remember { mutableStateOf(TabIngresos.HOY) }

    val resumen = remember {
        ResumenIngresos(
            totalIngresos = 1350.0,
            entregadas = 11,
            totalEntregas = 13,
            noEntregadas = 2,
            garrafonesVendidos = 28,
            tasaEntrega = 92,
            ingresosSemana = listOf(820.0, 1080.0, 950.0, 1290.0, 1180.0, 1750.0, 1350.0),
            totalSemana = 8420.0,
            entregasSemana = 67,
            deltaSemana = 8
        )
    }

    Scaffold(
        containerColor = HToGoColors.Background,
        bottomBar = {
            RepartidorBottomBar(
                selected = RepartidorTab.INGRESOS,
                onInicio = onInicio,
                onNegocio = onNegocio,
                onIngresos = {},
                onPerfil = onPerfil
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(HToGoColors.PrimaryDark, HToGoColors.Primary)))
                    .padding(start = 8.dp, end = 18.dp, top = 8.dp, bottom = 16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                        }
                        Spacer(Modifier.width(2.dp))
                        Column(Modifier.weight(1f)) {
                            Text("¡Buenos días!", color = Color.White.copy(alpha = .7f), fontSize = 13.sp)
                            Text(
                                "Carlos Mendoza",
                                color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    TabsPill(tab) { tab = it }
                }
            }

            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    BigCard(resumen, modifier = Modifier.padding(18.dp))
                }
                item {
                    SectionHeader("Desglose de hoy")
                    BreakdownCard(resumen, modifier = Modifier.padding(horizontal = 18.dp))
                }
                item {
                    Spacer(Modifier.height(20.dp))
                    SectionHeader("Esta semana", actionLabel = "Ver semana")
                    ChartCard(resumen, modifier = Modifier.padding(horizontal = 18.dp))
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun TabsPill(actual: TabIngresos, onChange: (TabIngresos) -> Unit) {
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = Color.White.copy(alpha = .12f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(4.dp)) {
            TabIngresos.values().forEach { t ->
                val sel = t == actual
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(99.dp))
                        .background(if (sel) Color.White else Color.Transparent)
                        .clickable { onChange(t) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        when (t) {
                            TabIngresos.HOY -> "Hoy"
                            TabIngresos.SEMANA -> "Semana"
                            TabIngresos.MES -> "Mes"
                            TabIngresos.HISTORIAL -> "Historial"
                        },
                        color = if (sel) HToGoColors.PrimaryDark else Color.White.copy(alpha = .8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun BigCard(r: ResumenIngresos, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HToGoColors.Primary),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(HToGoColors.Primary, HToGoColors.PrimaryDark)))
                .padding(22.dp)
        ) {
            Column {
                Text(
                    "Total del día",
                    color = Color.White.copy(alpha = .8f),
                    fontSize = 12.sp, fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        "$",
                        fontSize = 22.sp,
                        color = Color.White.copy(alpha = .8f),
                        modifier = Modifier.padding(end = 4.dp, bottom = 8.dp)
                    )
                    Text(
                        "%,.0f".format(r.totalIngresos),
                        fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White
                    )
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    BigItem("Entregas", "${r.entregadas} / ${r.totalEntregas}")
                    BigItem("Garrafones", "${r.garrafonesVendidos}")
                    BigItem("Tasa de entrega", "${r.tasaEntrega}%")
                }
            }
        }
    }
}

@Composable
private fun BigItem(label: String, value: String) {
    Column {
        Text(label.uppercase(), color = Color.White.copy(alpha = .7f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Text(
            value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String, actionLabel: String? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (actionLabel != null) {
            Text(
                actionLabel,
                fontSize = 12.sp,
                color = HToGoColors.Primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun BreakdownCard(r: ResumenIngresos, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, HToGoColors.OutlineSoft),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            BreakdownRow(
                iconBg = HToGoColors.Primary.copy(alpha = .12f),
                iconTint = HToGoColors.Primary,
                label = "Venta de garrafones",
                sub = "${r.garrafonesVendidos} garrafones · ${r.entregadas} entregas",
                amount = "$%,.0f".format(r.totalIngresos),
                amountColor = HToGoColors.TextPrimary,
                isVenta = true
            )
            HorizontalDivider(color = HToGoColors.OutlineSoft)
            BreakdownRow(
                iconBg = HToGoColors.AccentRose.copy(alpha = .12f),
                iconTint = HToGoColors.AccentRose,
                label = "Entregas no concretadas",
                sub = "${r.noEntregadas} pedidos sin entregar",
                amount = "$0",
                amountColor = HToGoColors.TextTertiary,
                isVenta = false
            )
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = HToGoColors.TextPrimary, thickness = 2.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Total recibido en efectivo",
                    fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = HToGoColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "$%,.0f".format(r.totalIngresos),
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = HToGoColors.Primary
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(HToGoColors.PrimaryWash)
                    .padding(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Filled.Info, null,
                    tint = HToGoColors.Primary, modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Este monto incluye lo que pagas a tu proveedor por cada garrafón.",
                    fontSize = 11.sp, color = HToGoColors.PrimaryDark
                )
            }
        }
    }
}

@Composable
private fun BreakdownRow(
    iconBg: Color,
    iconTint: Color,
    label: String,
    sub: String,
    amount: String,
    amountColor: Color,
    isVenta: Boolean
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isVenta) Icons.Filled.WaterDrop else Icons.Filled.Cancel,
                null, tint = iconTint, modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp)
            Text(sub, fontSize = 11.sp, color = HToGoColors.TextSecondary, modifier = Modifier.padding(top = 1.dp))
        }
        Text(amount, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = amountColor)
    }
}

@Composable
private fun ChartCard(r: ResumenIngresos, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, HToGoColors.OutlineSoft),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "$%,.0f".format(r.totalSemana),
                        fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = HToGoColors.TextPrimary
                    )
                    Text(
                        "Lun 14 - Dom 20 oct · ${r.entregasSemana} entregas",
                        fontSize = 11.sp, color = HToGoColors.TextSecondary
                    )
                }
                Surface(shape = RoundedCornerShape(99.dp), color = HToGoColors.AccentEmerald.copy(alpha = .12f)) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.TrendingUp, null, tint = HToGoColors.AccentEmerald, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "+${r.deltaSemana}%",
                            color = HToGoColors.AccentEmerald, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(top = 12.dp)
            ) {
                val w = size.width
                val h = size.height
                val maxV = r.ingresosSemana.maxOrNull() ?: 1.0
                val barCount = r.ingresosSemana.size
                val barW = w / barCount * 0.7f
                val gap = (w - barW * barCount) / (barCount - 1)
                listOf(0.25f, 0.5f, 0.75f).forEach { p ->
                    drawLine(
                        HToGoColors.OutlineSoft,
                        Offset(0f, h * p),
                        Offset(w, h * p),
                        strokeWidth = 1f
                    )
                }
                r.ingresosSemana.forEachIndexed { i, v ->
                    val barH = (v / maxV * h * 0.85f).toFloat()
                    val x = i * (barW + gap)
                    val y = h - barH
                    val opacity = when {
                        i == 5 -> 1f
                        i == 3 || i == 4 -> 0.5f
                        i == 1 || i == 2 -> 0.4f
                        else -> 0.3f
                    }
                    drawRoundRect(
                        color = HToGoColors.Primary.copy(alpha = opacity),
                        topLeft = Offset(x, y),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom").forEachIndexed { i, lbl ->
                    Text(
                        lbl,
                        fontSize = 10.sp,
                        color = if (i == 5) HToGoColors.Primary else HToGoColors.TextSecondary,
                        fontWeight = if (i == 5) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 868)
@Composable
fun IngresosScreenPreview() {
    HToGoTheme { IngresosScreen() }
}
