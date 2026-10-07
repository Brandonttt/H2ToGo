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
import androidx.compose.material.icons.filled.TrendingDown
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.htogo.app.data.local.SessionManager
import com.htogo.app.ui.RepartidorViewModel
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
    /** Variación % contra la semana anterior; null si la semana anterior no tuvo ingresos. */
    val deltaSemana: Int?,
    /** 0 = lunes … 6 = domingo: día resaltado en la gráfica. */
    val hoyIndex: Int,
    val etiquetaTotal: String,
    val etiquetaPeriodo: String
)

private val ZONA_CDMX: java.util.TimeZone = java.util.TimeZone.getTimeZone("America/Mexico_City")

/** Fecha ISO del backend → calendario en hora de la CDMX (minSdk 24: sin java.time). */
private fun calendarioLocal(iso: String?): java.util.Calendar? = try {
    val limpio = iso!!.replace(Regex("\\.\\d+"), "").replace("Z", "+00:00")
    val fecha = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US).parse(limpio)!!
    java.util.Calendar.getInstance(ZONA_CDMX).apply { time = fecha }
} catch (e: Exception) {
    null
}

/** Inicio (lunes 00:00, hora CDMX) de la semana que contiene [ref]. */
private fun inicioSemana(ref: java.util.Calendar): java.util.Calendar =
    (ref.clone() as java.util.Calendar).apply {
        firstDayOfWeek = java.util.Calendar.MONDAY
        set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        val desdeLunes = (get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        add(java.util.Calendar.DAY_OF_MONTH, -desdeLunes)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngresosScreen(
    onBack: () -> Unit = {},
    onInicio: () -> Unit = {},
    onNegocio: () -> Unit = {},
    onPerfil: () -> Unit = {},
    repartidorViewModel: RepartidorViewModel = viewModel()
) {
    var tab by remember { mutableStateOf(TabIngresos.HOY) }
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }
    val nombreRepartidor = remember {
        sessionManager.obtenerNombre()?.substringBefore(" ")?.ifBlank { "Repartidor" } ?: "Repartidor"
    }

    val liveMisEntregas by repartidorViewModel.misEntregas.collectAsState()

    LaunchedEffect(Unit) {
        repartidorViewModel.cargarMisEntregas()
    }

    // Cada pestaña filtra su periodo en hora de la CDMX (la fecha es la de creación del pedido;
    // las entregas son del mismo día). Antes todas mostraban el total histórico.
    val resumen = remember(liveMisEntregas, tab) {
        val ahora = java.util.Calendar.getInstance(ZONA_CDMX)
        val lunes = inicioSemana(ahora)
        val lunesAnterior = (lunes.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_MONTH, -7) }
        val conFecha = liveMisEntregas.map { it to calendarioLocal(it.fechaCreacion) }
        fun mismoDia(c: java.util.Calendar) = c.get(java.util.Calendar.YEAR) == ahora.get(java.util.Calendar.YEAR) &&
            c.get(java.util.Calendar.DAY_OF_YEAR) == ahora.get(java.util.Calendar.DAY_OF_YEAR)
        fun mismoMes(c: java.util.Calendar) = c.get(java.util.Calendar.YEAR) == ahora.get(java.util.Calendar.YEAR) &&
            c.get(java.util.Calendar.MONTH) == ahora.get(java.util.Calendar.MONTH)
        val delPeriodo = conFecha.filter { (_, c) ->
            when (tab) {
                TabIngresos.HOY -> c != null && mismoDia(c)
                TabIngresos.SEMANA -> c != null && !c.before(lunes)
                TabIngresos.MES -> c != null && mismoMes(c)
                TabIngresos.HISTORIAL -> true
            }
        }.map { it.first }
        val entregados = delPeriodo.filter { it.estado.equals("entregado", ignoreCase = true) }
        val noEntregados = delPeriodo.count { it.estado.equals("no_entregado", ignoreCase = true) }
        val cerrados = entregados.size + noEntregados

        // Gráfica: ingresos de la semana actual por día y comparación con la anterior.
        val porDia = DoubleArray(7)
        var semanaAnterior = 0.0
        var entregasSemana = 0
        conFecha.filter { it.first.estado.equals("entregado", ignoreCase = true) }.forEach { (p, c) ->
            if (c == null) return@forEach
            val monto = p.totalPagar ?: 0.0
            if (!c.before(lunes)) {
                porDia[(c.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7] += monto
                entregasSemana++
            } else if (!c.before(lunesAnterior)) {
                semanaAnterior += monto
            }
        }
        val totalSemana = porDia.sum()
        ResumenIngresos(
            totalIngresos = entregados.sumOf { it.totalPagar ?: 0.0 },
            entregadas = entregados.size,
            totalEntregas = cerrados,
            noEntregadas = noEntregados,
            garrafonesVendidos = entregados.sumOf { it.garrafonesTotales ?: 0 },
            tasaEntrega = if (cerrados > 0) entregados.size * 100 / cerrados else 0,
            ingresosSemana = porDia.toList(),
            totalSemana = totalSemana,
            entregasSemana = entregasSemana,
            deltaSemana = if (semanaAnterior > 0) ((totalSemana - semanaAnterior) / semanaAnterior * 100).toInt() else null,
            hoyIndex = (ahora.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7,
            etiquetaTotal = when (tab) {
                TabIngresos.HOY -> "Total del día"
                TabIngresos.SEMANA -> "Total de la semana"
                TabIngresos.MES -> "Total del mes"
                TabIngresos.HISTORIAL -> "Total histórico"
            },
            etiquetaPeriodo = when (tab) {
                TabIngresos.HOY -> "de hoy"
                TabIngresos.SEMANA -> "de la semana"
                TabIngresos.MES -> "del mes"
                TabIngresos.HISTORIAL -> "histórico"
            }
        )
    }
    val saludo = remember {
        when (java.util.Calendar.getInstance(ZONA_CDMX).get(java.util.Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "¡Buenos días!"
            in 12..18 -> "¡Buenas tardes!"
            else -> "¡Buenas noches!"
        }
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
                            Text(saludo, color = Color.White.copy(alpha = .7f), fontSize = 13.sp)
                            Text(
                                nombreRepartidor,
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
                    SectionHeader("Desglose ${resumen.etiquetaPeriodo}")
                    BreakdownCard(resumen, modifier = Modifier.padding(horizontal = 18.dp))
                }
                item {
                    Spacer(Modifier.height(20.dp))
                    SectionHeader("Esta semana")
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
                    r.etiquetaTotal,
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
                        "Semana actual · ${r.entregasSemana} entregas",
                        fontSize = 11.sp, color = HToGoColors.TextSecondary
                    )
                }
                val delta = r.deltaSemana
                if (delta != null) {
                    val sube = delta >= 0
                    val colorDelta = if (sube) HToGoColors.AccentEmerald else HToGoColors.AccentRose
                    Surface(shape = RoundedCornerShape(99.dp), color = colorDelta.copy(alpha = .12f)) {
                        Row(
                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(if (sube) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown, null,
                                tint = colorDelta, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                (if (sube) "+" else "") + "$delta% vs semana anterior",
                                color = colorDelta, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                            )
                        }
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
                val maxV = (r.ingresosSemana.maxOrNull() ?: 0.0).takeIf { it > 0 } ?: 1.0
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
                    // Resalta el día de hoy; los días por venir quedan más tenues.
                    val opacity = when {
                        i == r.hoyIndex -> 1f
                        i < r.hoyIndex -> 0.45f
                        else -> 0.2f
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
                        color = if (i == r.hoyIndex) HToGoColors.Primary else HToGoColors.TextSecondary,
                        fontWeight = if (i == r.hoyIndex) FontWeight.Bold else FontWeight.Normal
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
