package mx.htogo.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.htogo.ui.theme.HToGoTheme

/**
 * Pantalla 13 — Dashboard de ganancias del repartidor (dueño de purificadora).
 *
 * Diseño Opción B: Hero card grande sobre header degradado + breakdown
 * + barras semanales + calificación.
 *
 * Tabs: Hoy / Semana / Mes / Historial.
 * El repartidor es dueño del negocio: todo el dinero que cobra es suyo, en efectivo.
 */

enum class TabGanancias { HOY, SEMANA, MES, HISTORIAL }

data class ResumenDia(
    val totalIngresos: Double,
    val entregadas: Int,
    val totalEntregas: Int,
    val noEntregadas: Int,
    val garrafonesVendidos: Int,
    val calificacion: Double,
    val gananciasSemana: List<Double>,
    val totalSemana: Double,
    val entregasSemana: Int,
    val deltaSemana: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GananciasScreen(
    onBack: () -> Unit = {},
    onVerSemana: () -> Unit = {}
) {
    val primary = Color(0xFF0077B6)
    val primaryDark = Color(0xFF03045E)
    val success = Color(0xFF10B981)
    val danger = Color(0xFFEF4444)
    val text = Color(0xFF0F172A)
    val text2 = Color(0xFF64748B)
    val text3 = Color(0xFF94A3B8)
    val bg = Color(0xFFF8FAFC)
    val outline = Color(0xFFE2E8F0)

    var tab by remember { mutableStateOf(TabGanancias.HOY) }

    val resumen = remember {
        ResumenDia(
            totalIngresos = 1350.0,
            entregadas = 11,
            totalEntregas = 13,
            noEntregadas = 2,
            garrafonesVendidos = 28,
            calificacion = 4.9,
            gananciasSemana = listOf(820.0, 1080.0, 950.0, 1290.0, 1180.0, 1750.0, 1350.0),
            totalSemana = 8420.0,
            entregasSemana = 67,
            deltaSemana = 8
        )
    }

    Scaffold(containerColor = bg) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Header degradado con saludo + tabs
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(primaryDark, primary)))
                    .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 16.dp)
            ) {
                Column {
                    Text("¡Buenos días!", color = Color.White.copy(alpha = .7f), fontSize = 13.sp)
                    Text(
                        "Carlos Mendoza",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(14.dp))
                    TabsPill(tab, primaryDark) { tab = it }
                }
            }

            LazyColumn(Modifier.fillMaxSize()) {
                // Hero big card
                item {
                    BigCard(resumen, primary, primaryDark, modifier = Modifier.padding(18.dp))
                }
                // Desglose de hoy
                item {
                    SectionHeader("Desglose de hoy")
                    BreakdownCard(resumen, primary, danger, text, text2, text3, outline,
                        modifier = Modifier.padding(horizontal = 18.dp))
                }
                // Esta semana
                item {
                    Spacer(Modifier.height(20.dp))
                    SectionHeader("Esta semana", actionLabel = "Ver semana", actionColor = primary, onAction = onVerSemana)
                    ChartCard(resumen, primary, success, text, text2, outline,
                        modifier = Modifier.padding(horizontal = 18.dp))
                }
                // Calificación
                item {
                    Spacer(Modifier.height(20.dp))
                    SectionHeader("Calificación")
                    RatingCard(resumen.calificacion, primary, success, text, text2, outline,
                        modifier = Modifier.padding(horizontal = 18.dp))
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun TabsPill(
    actual: TabGanancias,
    activeText: Color,
    onChange: (TabGanancias) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = Color.White.copy(alpha = .12f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(4.dp)) {
            TabGanancias.values().forEach { t ->
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
                            TabGanancias.HOY -> "Hoy"
                            TabGanancias.SEMANA -> "Semana"
                            TabGanancias.MES -> "Mes"
                            TabGanancias.HISTORIAL -> "Historial"
                        },
                        color = if (sel) activeText else Color.White.copy(alpha = .8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun BigCard(
    r: ResumenDia,
    primary: Color,
    primaryDark: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = primary),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(primary, primaryDark)))
                .padding(22.dp)
        ) {
            Column {
                Text(
                    "Total del día",
                    color = Color.White.copy(alpha = .8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
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
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    BigItem("Entregas", "${r.entregadas} / ${r.totalEntregas}")
                    BigItem("Garrafones", "${r.garrafonesVendidos}")
                    BigItem("Calificación", "%.1f ★".format(r.calificacion))
                }
            }
        }
    }
}

@Composable
private fun BigItem(label: String, value: String) {
    Column {
        Text(label.uppercase(), color = Color.White.copy(alpha = .7f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    actionColor: Color = Color.Unspecified,
    onAction: () -> Unit = {}
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (actionLabel != null) {
            Text(
                actionLabel,
                fontSize = 12.sp,
                color = actionColor,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onAction() }
            )
        }
    }
}

@Composable
private fun BreakdownCard(
    r: ResumenDia,
    primary: Color,
    danger: Color,
    text: Color,
    text2: Color,
    text3: Color,
    outline: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, outline),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            BreakdownRow(
                icon = Icons.Default.WaterDrop,
                iconBg = primary.copy(alpha = .12f),
                iconTint = primary,
                label = "Venta de garrafones",
                sub = "${r.garrafonesVendidos} garrafones · ${r.entregadas} entregas",
                amount = "$%,.0f".format(r.totalIngresos),
                amountColor = text,
                text2 = text2
            )
            HorizontalDivider(color = outline)
            BreakdownRow(
                icon = Icons.Default.Cancel,
                iconBg = danger.copy(alpha = .12f),
                iconTint = danger,
                label = "Entregas no concretadas",
                sub = "${r.noEntregadas} pedidos sin entregar",
                amount = "$0",
                amountColor = text3,
                text2 = text2
            )
            // Total
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = text, thickness = 2.dp)
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Total recibido en efectivo",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = text,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "$%,.0f".format(r.totalIngresos),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = primary
                )
            }
        }
    }
}

@Composable
private fun BreakdownRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color,
    label: String,
    sub: String,
    amount: String,
    amountColor: Color,
    text2: Color
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(iconBg),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp)
            Text(sub, fontSize = 11.sp, color = text2, modifier = Modifier.padding(top = 1.dp))
        }
        Text(amount, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = amountColor)
    }
}

@Composable
private fun ChartCard(
    r: ResumenDia,
    primary: Color,
    success: Color,
    text: Color,
    text2: Color,
    outline: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, outline),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("$%,.0f".format(r.totalSemana), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = text)
                    Text(
                        "Lun 14 - Dom 20 oct · ${r.entregasSemana} entregas",
                        fontSize = 11.sp,
                        color = text2
                    )
                }
                Surface(shape = RoundedCornerShape(99.dp), color = success.copy(alpha = .12f)) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.TrendingUp, null, tint = success, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("+${r.deltaSemana}%", color = success, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            // Bar chart
            Canvas(Modifier.fillMaxWidth().height(140.dp).padding(top = 12.dp)) {
                val w = size.width
                val h = size.height
                val maxV = r.gananciasSemana.maxOrNull() ?: 1.0
                val barCount = r.gananciasSemana.size
                val barW = w / barCount * 0.7f
                val gap = (w - barW * barCount) / (barCount - 1)
                // Gridlines
                listOf(0.25f, 0.5f, 0.75f).forEach { p ->
                    drawLine(
                        outline,
                        Offset(0f, h * p),
                        Offset(w, h * p),
                        strokeWidth = 1f
                    )
                }
                r.gananciasSemana.forEachIndexed { i, v ->
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
                        color = primary.copy(alpha = opacity),
                        topLeft = Offset(x, y),
                        size = Size(barW, barH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom").forEachIndexed { i, lbl ->
                    Text(
                        lbl,
                        fontSize = 10.sp,
                        color = if (i == 5) primary else text2,
                        fontWeight = if (i == 5) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingCard(
    rating: Double,
    primary: Color,
    success: Color,
    text: Color,
    text2: Color,
    outline: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, outline),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "%.1f".format(rating),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = primary
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text("★★★★★", color = Color(0xFFF59E0B), fontSize = 14.sp)
                Text("Promedio de 247 calificaciones", fontSize = 12.sp, color = text2,
                    modifier = Modifier.padding(top = 2.dp))
                Text("▲ +0.1 esta semana", fontSize = 11.sp, color = success, fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 868)
@Composable
fun GananciasScreenPreview() {
    HToGoTheme { GananciasScreen() }
}
