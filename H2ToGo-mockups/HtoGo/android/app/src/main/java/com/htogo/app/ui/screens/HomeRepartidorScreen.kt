package com.htogo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htogo.app.ui.components.EstadoPedido
import com.htogo.app.ui.components.EstadoPedidoChip
import com.htogo.app.ui.components.RepartidorBottomBar
import com.htogo.app.ui.components.RepartidorTab
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme

@Composable
fun HomeRepartidorScreen(
    onInventario: () -> Unit = {},
    onIngresos: () -> Unit = {},
    onPerfil: () -> Unit = {},
    onRuta: () -> Unit = {},
    onPedidoProgramado: () -> Unit = {},
    onSwitchRol: () -> Unit = {}
) {
    val nombre = "Carlos"
    var enLinea by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = HToGoColors.Background,
        bottomBar = {
            RepartidorBottomBar(
                selected = RepartidorTab.INICIO,
                onInicio = {},
                onNegocio = onInventario,
                onIngresos = onIngresos,
                onPerfil = onPerfil
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Box(
                Modifier.fillMaxWidth().background(
                    Brush.verticalGradient(listOf(HToGoColors.PrimaryDark, HToGoColors.Primary))
                ).padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = .2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                nombre.first().toString(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Hola, $nombre",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Storefront,
                                    null,
                                    tint = HToGoColors.PrimarySoft,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Purificadora Aqua Pura",
                                    color = HToGoColors.PrimarySoft,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                "Lunes 26 de abril",
                                color = HToGoColors.PrimarySoft.copy(alpha = .85f),
                                fontSize = 12.sp
                            )
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(Color.White.copy(alpha = .18f))
                                .clickable { onSwitchRol() }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.SwapHoriz,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Rol",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = {}) {
                            Icon(Icons.Filled.Notifications, null, tint = Color.White)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(10.dp).clip(CircleShape)
                                    .background(if (enLinea) HToGoColors.StatusEntregado else HToGoColors.StatusPendiente)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (enLinea) "En línea" else "Fuera de línea",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HToGoColors.TextPrimary
                                )
                                Text(
                                    if (enLinea) "Recibiendo pedidos" else "Activa para recibir pedidos",
                                    fontSize = 12.sp,
                                    color = HToGoColors.TextSecondary
                                )
                            }
                            Switch(
                                checked = enLinea,
                                onCheckedChange = { enLinea = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = HToGoColors.Primary
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Tu jornada de hoy",
                style = MaterialTheme.typography.titleMedium,
                color = HToGoColors.TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("Entregas", "8", "/ 14", Icons.Filled.CheckCircle, HToGoColors.StatusEntregado, Modifier.weight(1f))
                StatCard("En tu vehículo", "22", "garrafones", Icons.Filled.DirectionsCar, HToGoColors.Primary, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("En base", "38", "garrafones", Icons.Filled.Warehouse, HToGoColors.AccentPurple, Modifier.weight(1f))
                StatCard("Ingresos", "$1,260", "MXN", Icons.Filled.Payments, HToGoColors.StatusAsignado, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("En ruta", "4h 12m", "trabajadas", Icons.Filled.Schedule, HToGoColors.PrimaryDark, Modifier.weight(1f))
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "Próxima entrega",
                style = MaterialTheme.typography.titleMedium,
                color = HToGoColors.TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(8.dp))
            Card(
                onClick = onRuta,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).background(HToGoColors.PrimarySoft),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.WaterDrop, null, tint = HToGoColors.Primary) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Pedido #HG-1287",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HToGoColors.TextPrimary
                            )
                            Text(
                                "Gael M. · 3 garrafones",
                                fontSize = 13.sp,
                                color = HToGoColors.TextSecondary
                            )
                        }
                        EstadoPedidoChip(EstadoPedido.ASIGNADO)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Schedule, null,
                            tint = HToGoColors.StatusAsignado, modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Agendado 11:30 am · en ~45 min",
                            fontSize = 13.sp,
                            color = HToGoColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Map, null,
                            tint = HToGoColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Av. Insurgentes Sur 1234, Del Valle",
                            fontSize = 13.sp,
                            color = HToGoColors.TextSecondary
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AttachMoney, null,
                            tint = HToGoColors.StatusEntregado, modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Cobrarás $120 MXN en este pedido",
                            fontSize = 13.sp,
                            color = HToGoColors.StatusEntregado,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = onRuta,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary)
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Iniciar ruta", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Pedidos apartados aceptados (3)",
                    style = MaterialTheme.typography.titleMedium,
                    color = HToGoColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                "Pedidos programados que ya te apartaste",
                fontSize = 12.sp,
                color = HToGoColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
            )
            Spacer(Modifier.height(4.dp))
            ColaItem("HG-1290", "María L.", "Col. Nápoles", "5 garrafones", "$200", EstadoPedido.ASIGNADO, onPedidoProgramado)
            ColaItem("HG-1292", "Roberto V.", "Col. Narvarte", "2 garrafones", "$80", EstadoPedido.ASIGNADO, onPedidoProgramado)
            ColaItem("HG-1295", "Lucía F.", "Col. Del Valle", "4 garrafones", "$160", EstadoPedido.ASIGNADO, onPedidoProgramado)
        }
    }
}

@Composable
private fun StatCard(
    label: String, big: String, small: String, icon: ImageVector,
    accent: Color, modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(accent.copy(alpha = .14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp)) }
                Spacer(Modifier.width(8.dp))
                Text(label, fontSize = 12.sp, color = HToGoColors.TextSecondary)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(big, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = HToGoColors.TextPrimary)
                Spacer(Modifier.width(4.dp))
                Text(
                    small, fontSize = 12.sp, color = HToGoColors.TextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ColaItem(
    id: String, cliente: String, colonia: String, cant: String, monto: String,
    estado: EstadoPedido, onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "#$id", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = HToGoColors.TextPrimary
                    )
                    Spacer(Modifier.width(8.dp)); EstadoPedidoChip(estado)
                }
                Text("$cliente · $colonia", fontSize = 12.sp, color = HToGoColors.TextSecondary)
                Text(cant, fontSize = 12.sp, color = HToGoColors.TextSecondary)
            }
            Text(monto, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HToGoColors.PrimaryDark)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun HomeRepartidorPreview() { HToGoTheme { HomeRepartidorScreen() } }
