package com.htogo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
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
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme

@Composable
fun HomeClienteScreen(
    onNuevoPedido: () -> Unit = {},
    onElegirPurificadora: () -> Unit = {},
    onPedirAbierto: () -> Unit = {},
    onTrackPedido: () -> Unit = {},
    onHistorial: () -> Unit = {},
    onPerfil: () -> Unit = {},
    onAbrirPurificadora: () -> Unit = {},
    onSwitchRol: () -> Unit = {}
) {
    val nombre = "Gael"
    val direccion = "Av. Insurgentes Sur 1234, Col. Del Valle"

    Scaffold(
        containerColor = HToGoColors.Background,
        bottomBar = {
            ClienteBottomBar(
                selected = 0,
                onHistorial = onHistorial,
                onPerfil = onPerfil
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNuevoPedido,
                containerColor = HToGoColors.Primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(28.dp),
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Pedir agua", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(HToGoColors.PrimaryDark, HToGoColors.Primary)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Hola, $nombre",
                                color = Color.White, fontSize = 22.sp,
                                fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, null,
                                    tint = HToGoColors.PrimarySoft,
                                    modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(direccion,
                                    color = HToGoColors.PrimarySoft, fontSize = 13.sp,
                                    maxLines = 1)
                            }
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Filled.Notifications, null, tint = Color.White)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable(onClick = onSwitchRol)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.SwapHoriz, null,
                            tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Vista Repartidor",
                            color = Color.White, fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(16.dp))
                    Card(
                        onClick = onElegirPurificadora,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Search, null, tint = HToGoColors.TextTertiary)
                            Spacer(Modifier.width(10.dp))
                            Text("¿Buscas una purificadora cerca?",
                                color = HToGoColors.TextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text("Pedido en curso",
                style = MaterialTheme.typography.titleMedium,
                color = HToGoColors.TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))
            PedidoActivoCard(onClick = onTrackPedido)

            Spacer(Modifier.height(24.dp))

            Text("Acciones rápidas",
                style = MaterialTheme.typography.titleMedium,
                color = HToGoColors.TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionTile(
                    Icons.Filled.Storefront, "Elegir purificadora",
                    "Pide a un negocio específico", Modifier.weight(1f), onElegirPurificadora
                )
                QuickActionTile(
                    Icons.Filled.AttachMoney, "Pedir con precio máximo",
                    "Te asignamos el primero disponible", Modifier.weight(1f), onPedirAbierto
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionTile(Icons.Filled.History, "Repetir último", "5 garrafones", Modifier.weight(1f), {})
                QuickActionTile(Icons.Filled.Schedule, "Programar", "Pedido recurrente", Modifier.weight(1f), {})
            }

            Spacer(Modifier.height(24.dp))

            Text("Purificadoras cerca de ti",
                style = MaterialTheme.typography.titleMedium,
                color = HToGoColors.TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))
            PurificadoraMini("Aguas Del Valle", "0.8 km", onAbrirPurificadora, onNuevoPedido)
            PurificadoraMini("HidroExpress BJ", "1.4 km", onAbrirPurificadora, onNuevoPedido)
            PurificadoraMini("AquaPura Nápoles", "2.1 km", onAbrirPurificadora, onNuevoPedido)

            Spacer(Modifier.height(96.dp))
        }
    }
}

@Composable
private fun PedidoActivoCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
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
                ) {
                    Icon(Icons.Filled.WaterDrop, null, tint = HToGoColors.Primary)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Pedido #HG-1287",
                        style = MaterialTheme.typography.titleMedium,
                        color = HToGoColors.TextPrimary)
                    Text("3 garrafones · Aguas Del Valle",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HToGoColors.TextSecondary)
                }
                EstadoPedidoChip(EstadoPedido.EN_CAMINO)
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf(true, true, true, false).forEachIndexed { i, done ->
                    Box(
                        Modifier
                            .height(6.dp).weight(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (done) HToGoColors.Primary else HToGoColors.OutlineSoft)
                    )
                    if (i < 3) Spacer(Modifier.width(4.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row {
                Icon(Icons.Filled.Schedule, null,
                    tint = HToGoColors.TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Llega en ~8 min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HToGoColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector, title: String, subtitle: String,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(108.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                    .background(HToGoColors.PrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = HToGoColors.Primary,
                    modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = HToGoColors.TextPrimary)
                Text(subtitle, fontSize = 12.sp, color = HToGoColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun PurificadoraMini(
    nombre: String,
    distancia: String,
    onAbrir: () -> Unit,
    onPedir: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onAbrir),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(HToGoColors.PrimarySoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Storefront, null,
                        tint = HToGoColors.Primary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(nombre, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = HToGoColors.TextPrimary)
                    Text(distancia,
                        fontSize = 12.sp, color = HToGoColors.TextSecondary)
                }
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(HToGoColors.PrimarySoft)
                    .clickable(onClick = onPedir)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Pedir", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = HToGoColors.Primary)
            }
        }
    }
}

@Composable
private fun ClienteBottomBar(
    selected: Int,
    onHistorial: () -> Unit,
    onPerfil: () -> Unit
) {
    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        data class NavSpec(val label: String, val icon: ImageVector, val action: () -> Unit)
        val items = listOf(
            NavSpec("Inicio", Icons.Filled.Home) {},
            NavSpec("Pedidos", Icons.Filled.History, onHistorial),
            NavSpec("Perfil", Icons.Filled.Person, onPerfil)
        )
        items.forEachIndexed { i, item ->
            NavigationBarItem(
                selected = i == selected,
                onClick = item.action,
                icon = { Icon(item.icon, null) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = HToGoColors.Primary,
                    selectedTextColor = HToGoColors.Primary,
                    indicatorColor    = HToGoColors.PrimarySoft,
                    unselectedIconColor = HToGoColors.TextTertiary,
                    unselectedTextColor = HToGoColors.TextTertiary
                )
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun HomeClientePreview() { HToGoTheme { HomeClienteScreen() } }
