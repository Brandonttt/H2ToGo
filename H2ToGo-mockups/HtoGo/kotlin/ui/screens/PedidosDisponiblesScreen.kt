package mx.htogo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.htogo.ui.theme.HToGoTheme

/**
 * Pantalla 11 — Pedidos disponibles para el repartidor.
 * Lista de pedidos cercanos con distancia, ETA, monto y opción de aceptar.
 */

data class PedidoDisponible(
    val id: String,
    val cliente: String,
    val direccion: String,
    val colonia: String,
    val productos: String,
    val total: Double,
    val distanciaKm: Double,
    val tiempoEstimadoMin: Int,
    val esPrioritario: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidosDisponiblesScreen(
    onBack: () -> Unit = {},
    onAceptar: (PedidoDisponible) -> Unit = {},
    onVerDetalle: (PedidoDisponible) -> Unit = {}
) {
    val primary = Color(0xFF0077B6)
    val primaryDark = Color(0xFF03045E)
    val success = Color(0xFF10B981)
    val warning = Color(0xFFF59E0B)
    val bg = Color(0xFFF8FAFC)
    val outline = Color(0xFFE2E8F0)

    val pedidos = remember {
        listOf(
            PedidoDisponible("HG-1287", "Andrea Martínez", "Insurgentes Sur 1234, 4B", "Del Valle",
                "3 × Ciel 20 L", 135.0, 1.2, 8, esPrioritario = true),
            PedidoDisponible("HG-1286", "Restaurante La Esquina", "Av. Cuauhtémoc 89", "Roma Norte",
                "5 × Bonafont 20 L", 250.0, 2.4, 12),
            PedidoDisponible("HG-1285", "Laura Hernández", "Calle Coahuila 23", "Roma Sur",
                "2 × Ciel 20 L", 90.0, 0.8, 5),
            PedidoDisponible("HG-1284", "Roberto Silva", "Av. Universidad 456", "Narvarte",
                "4 × Ciel 20 L", 180.0, 3.1, 15)
        )
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pedidos disponibles", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("${pedidos.size} cerca de ti", fontSize = 12.sp, color = Color.White.copy(alpha = .8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
                },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.FilterList, "Filtrar") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryDark,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(pedidos) { pedido ->
                PedidoDisponibleCard(
                    pedido = pedido,
                    primary = primary, success = success, warning = warning, outline = outline,
                    onAceptar = { onAceptar(pedido) },
                    onVerDetalle = { onVerDetalle(pedido) }
                )
            }
        }
    }
}

@Composable
private fun PedidoDisponibleCard(
    pedido: PedidoDisponible,
    primary: Color,
    success: Color,
    warning: Color,
    outline: Color,
    onAceptar: () -> Unit,
    onVerDetalle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(
            if (pedido.esPrioritario) 2.dp else 1.dp,
            if (pedido.esPrioritario) warning else outline
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onVerDetalle() }
    ) {
        Column(Modifier.padding(14.dp)) {
            if (pedido.esPrioritario) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(warning.copy(alpha = .15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(Icons.Default.Bolt, null, tint = warning, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PRIORITARIO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = warning)
                }
                Spacer(Modifier.height(8.dp))
            }
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("#${pedido.id}", fontSize = 11.sp, color = Color.Gray)
                    Text(pedido.cliente, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        "${pedido.direccion} · ${pedido.colonia}",
                        fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 1.dp)
                    )
                }
                Text(
                    "$%.0f".format(pedido.total),
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = success
                )
            }

            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WaterDrop, null, tint = primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(pedido.productos, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("%.1f km".format(pedido.distanciaKm), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Default.Schedule, null, tint = primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("~${pedido.tiempoEstimadoMin} min", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onAceptar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(23.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primary)
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(6.dp))
                Text("Aceptar pedido", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 868)
@Composable
fun PedidosDisponiblesScreenPreview() {
    HToGoTheme { PedidosDisponiblesScreen() }
}
