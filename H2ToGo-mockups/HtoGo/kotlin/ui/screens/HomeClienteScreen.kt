package com.htogo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import com.htogo.ui.components.EstadoPedido
import com.htogo.ui.components.EstadoPedidoChip
import com.htogo.ui.theme.HToGoColors
import com.htogo.ui.theme.HToGoTheme

@Composable
fun HomeClienteScreen(
    nombre: String = "Gael",
    direccion: String = "Av. Insurgentes Sur 1234, Col. Del Valle",
    onNuevoPedido: () -> Unit = {},
    onTrackPedido: () -> Unit = {},
    onHistorial: () -> Unit = {},
    onPerfil: () -> Unit = {}
) {
    Scaffold(
        containerColor = HToGoColors.Background,
        bottomBar = { ClienteBottomBar(selected = 0) },
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
            // Header con gradient
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
                            Text("Hola, $nombre 👋",
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
                    Spacer(Modifier.height(16.dp))
                    // Search-like card
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Search, null, tint = HToGoColors.TextTertiary)
                            Spacer(Modifier.width(10.dp))
                            Text("¿Buscas un repartidor cerca?",
                                color = HToGoColors.TextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Pedido activo
            Text("Pedido en curso",
                style = MaterialTheme.typography.titleMedium,
                color = HToGoColors.TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))
            PedidoActivoCard(onClick = onTrackPedido)

            Spacer(Modifier.height(24.dp))

            // Acciones rápidas
            Text("Acciones rápidas",
                style = MaterialTheme.typography.titleMedium,
                color = HToGoColors.TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionTile(Icons.Filled.WaterDrop, "Pedir 1 garrafón", "$35", Modifier.weight(1f), onNuevoPedido)
                QuickActionTile(Icons.Filled.WaterDrop, "Pedir 5 garrafones", "$160", Modifier.weight(1f), onNuevoPedido)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionTile(Icons.Filled.History, "Repetir último", "5 garrafones", Modifier.weight(1f), {})
                QuickActionTile(Icons.Filled.Schedule, "Programar", "Suscripción", Modifier.weight(1f), {})
            }

            Spacer(Modifier.height(24.dp))

            // Repartidores cercanos
            Text("Repartidores cerca de ti",
                style = MaterialTheme.typography.titleMedium,
                color = HToGoColors.TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))
            RepartidorMini("Aguas Del Valle", "0.8 km", 4.9, "12 min")
            RepartidorMini("HidroExpress BJ", "1.4 km", 4.7, "18 min")
            RepartidorMini("AquaPura Nápoles", "2.1 km", 4.8, "25 min")

            Spacer(Modifier.height(96.dp)) // FAB clearance
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
                    Text("3 garrafones · Carlos M.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HToGoColors.TextSecondary)
                }
                EstadoPedidoChip(EstadoPedido.EN_CAMINO)
            }
            Spacer(Modifier.height(14.dp))
            // Progress
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
private fun RepartidorMini(nombre: String, distancia: String, rating: Double, eta: String) {
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
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(HToGoColors.PrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Text(nombre.first().toString(),
                    fontWeight = FontWeight.Bold, color = HToGoColors.Primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(nombre, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = HToGoColors.TextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, null,
                        tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("$rating · $distancia · $eta",
                        fontSize = 12.sp, color = HToGoColors.TextSecondary)
                }
            }
            Box(
                Modifier.clip(RoundedCornerShape(99.dp))
                    .background(HToGoColors.PrimarySoft)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Pedir", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = HToGoColors.Primary)
            }
        }
    }
}

@Composable
private fun ClienteBottomBar(selected: Int) {
    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        listOf(
            "Inicio" to Icons.Filled.Home,
            "Pedidos" to Icons.Filled.History,
            "Perfil" to Icons.Filled.Person
        ).forEachIndexed { i, (label, icon) ->
            NavigationBarItem(
                selected = i == selected,
                onClick = {},
                icon = { Icon(icon, null) },
                label = { Text(label) },
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
