package com.htogo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme

enum class HistEstado(val label: String, val color: Color, val icon: ImageVector) {
    EN_CAMINO ("En camino",  HToGoColors.StatusEnCamino,  Icons.Filled.DeliveryDining),
    ENTREGADO ("Entregado",  HToGoColors.StatusEntregado, Icons.Filled.CheckCircle),
    CANCELADO ("Cancelado",  HToGoColors.StatusCancelado, Icons.Filled.Cancel)
}

data class HistPedido(
    val id: String,
    val resumen: String,
    val purificadora: String,
    val repartidor: String,
    val fecha: String,
    val direccion: String,
    val direccionIcon: ImageVector,
    val total: Int,
    val estado: HistEstado,
    val grupo: String
)

private val SAMPLE_HIST = listOf(
    HistPedido("HG-1284","2 × Bonafont 20 L","Hidro Express BJ","Carlos M.","7:15 AM",
        "Casa · Insurgentes Sur 1234", Icons.Filled.Home, 91, HistEstado.ENTREGADO, "Hoy · 26 abr"),
    HistPedido("HG-1271","5 × Ciel 20 L","Aguas Del Valle","Luis R.","5:48 PM",
        "Oficina · Av. Universidad 567", Icons.Filled.Work, 195, HistEstado.ENTREGADO, "Ayer · 25 abr"),
    HistPedido("HG-1265","1 × Epura 20 L","Aguas Del Valle","Cancelado por usuario","10:32 AM",
        "Casa · Insurgentes Sur 1234", Icons.Filled.Home, 56, HistEstado.CANCELADO, "Ayer · 25 abr"),
    HistPedido("HG-1244","3 × Ciel 20 L","Aguas Del Valle","Carlos M.","22 abr, 8:05 AM",
        "Casa · Insurgentes Sur 1234", Icons.Filled.Home, 125, HistEstado.ENTREGADO, "Esta semana"),
    HistPedido("HG-1232","10 × Santorini 20 L","AquaPura Nápoles","Sergio T.","20 abr, 11:22 AM",
        "Casa de mamá · Heriberto Frías 890", Icons.Filled.Favorite, 320, HistEstado.ENTREGADO, "Esta semana"),
    HistPedido("HG-1180","3 × Bonafont 20 L","Hidro Express BJ","Carlos M.","28 mar",
        "Casa · Insurgentes Sur 1234", Icons.Filled.Home, 134, HistEstado.ENTREGADO, "Marzo 2026"),
)

private data class TabFilter(val label: String, val count: Int)

private val SAMPLE_TABS = listOf(
    TabFilter("Todos", 14),
    TabFilter("Activos", 1),
    TabFilter("Entregados", 12),
    TabFilter("Cancelados", 1)
)

@Composable
fun HistorialPedidosScreen(
    onBack: () -> Unit = {},
    onPedidoTap: () -> Unit = {},
    onAbrirPurificadora: () -> Unit = {},
    onRepetir: () -> Unit = {}
) {
    var activeTab by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }

    Scaffold(
        topBar = { HistHeader(query = query, onQuery = { query = it }, onBack = onBack) },
        bottomBar = { BottomNav() },
        containerColor = HToGoColors.Background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            FilterTabs(SAMPLE_TABS, activeTab) { activeTab = it }

            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    GroupLabel("En curso")
                    ActiveOrderCard(
                        id = "HG-1287",
                        resumen = "3 × Ciel · Aguas Del Valle",
                        onOpen = onPedidoTap
                    )
                }

                val grupos = SAMPLE_HIST.groupBy { it.grupo }
                grupos.forEach { (grupo, pedidos) ->
                    item { GroupLabel(grupo) }
                    items(pedidos) { p ->
                        HistCard(
                            p = p,
                            onTap = onPedidoTap,
                            onAbrirPurificadora = onAbrirPurificadora,
                            onRepeat = onRepetir
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistHeader(query: String, onQuery: (String) -> Unit, onBack: () -> Unit) {
    Surface(color = HToGoColors.PrimaryDark) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderIconBtn(Icons.AutoMirrored.Filled.ArrowBack, onClick = onBack)
                Text("Mis pedidos",
                    Modifier.weight(1f),
                    fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                HeaderIconBtn(Icons.Filled.FilterList, onClick = {})
            }
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.10f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Search, null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value = query,
                        onValueChange = onQuery,
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White, fontSize = 13.sp
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text("Buscar por #, purificadora o marca…",
                                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                            }
                            inner()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderIconBtn(icon: ImageVector, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.10f),
        modifier = Modifier.size(40.dp).clickable(onClick = onClick)
    ) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White) } }
}

@Composable
private fun FilterTabs(tabs: List<TabFilter>, active: Int, onSelect: (Int) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.background(HToGoColors.Background)
    ) {
        items(tabs.size) { i ->
            val t = tabs[i]
            val activeT = i == active
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = if (activeT) HToGoColors.Primary else Color.White,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, if (activeT) HToGoColors.Primary else HToGoColors.OutlineSoft
                ),
                modifier = Modifier.clickable { onSelect(i) }
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(t.label, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        color = if (activeT) Color.White else HToGoColors.TextSecondary)
                    Spacer(Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = if (activeT) Color.White.copy(alpha = 0.20f) else HToGoColors.Background
                    ) {
                        Text("${t.count}",
                            Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = if (activeT) Color.White else HToGoColors.TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(text.uppercase(),
        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        color = HToGoColors.TextSecondary, letterSpacing = 0.4.sp,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
}

@Composable
private fun ActiveOrderCard(id: String, resumen: String, onOpen: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Box(
            Modifier
                .background(
                    Brush.linearGradient(listOf(HToGoColors.PrimaryDark, HToGoColors.Primary)),
                    RoundedCornerShape(18.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.13f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Storefront, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Pedido #$id", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                        Text(resumen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = Color.White)
                    }
                    Surface(shape = RoundedCornerShape(99.dp),
                        color = Color.White.copy(alpha = 0.16f)) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                            Spacer(Modifier.width(6.dp))
                            Text("En camino", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                color = Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onOpen,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White, contentColor = HToGoColors.Primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Rastrear pedido",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun HistCard(
    p: HistPedido,
    onTap: () -> Unit,
    onAbrirPurificadora: () -> Unit,
    onRepeat: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = HToGoColors.Surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable(onClick = onTap)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                        .background(p.estado.color.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) { Icon(p.estado.icon, null, tint = p.estado.color) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("#${p.id}", fontSize = 13.sp, color = HToGoColors.TextSecondary)
                    Text(p.resumen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = HToGoColors.TextPrimary)
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onAbrirPurificadora)
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Storefront, null,
                            tint = HToGoColors.Primary, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(p.purificadora,
                            fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            color = HToGoColors.Primary)
                    }
                    Text("${p.fecha} · ${p.repartidor}",
                        fontSize = 11.sp, color = HToGoColors.TextTertiary)
                    Spacer(Modifier.height(4.dp))
                    StatusChipSmall(p.estado)
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (p.estado == HistEstado.CANCELADO) {
                        Text("$${p.total}", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            color = HToGoColors.TextTertiary,
                            textDecoration = TextDecoration.LineThrough)
                    } else {
                        Text("$${p.total}", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            color = HToGoColors.TextPrimary)
                    }
                }
            }
            Divider(Modifier.padding(top = 10.dp, bottom = 10.dp), color = HToGoColors.OutlineSoft)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(p.direccionIcon, null, tint = HToGoColors.TextTertiary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(p.direccion, fontSize = 12.sp, color = HToGoColors.TextSecondary,
                    modifier = Modifier.weight(1f), maxLines = 1)
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = HToGoColors.PrimarySoft,
                    modifier = Modifier.clickable(onClick = onRepeat)
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Refresh, null, tint = HToGoColors.Primary,
                            modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Repetir", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = HToGoColors.Primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChipSmall(estado: HistEstado) {
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = estado.color.copy(alpha = 0.10f)
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(estado.color))
            Spacer(Modifier.width(4.dp))
            Text(estado.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                color = estado.color)
        }
    }
}

@Composable
private fun BottomNav() {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Row(
            Modifier.fillMaxWidth().height(72.dp).padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NavItem(Icons.Filled.Home, "Inicio", false)
            NavItem(Icons.Filled.History, "Pedidos", true)
            NavItem(Icons.Filled.Person, "Perfil", false)
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, active: Boolean) {
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = if (active) HToGoColors.PrimarySoft else Color.Transparent
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = if (active) HToGoColors.Primary else HToGoColors.TextTertiary,
                modifier = Modifier.size(22.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                color = if (active) HToGoColors.Primary else HToGoColors.TextTertiary)
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 900, name = "08 · Historial de pedidos")
@Composable
fun HistorialPedidosScreenPreview() {
    HToGoTheme { HistorialPedidosScreen() }
}
