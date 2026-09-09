package mx.htogo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.htogo.ui.theme.HToGoTheme

/**
 * Pantalla 10 — Inventario / Vehículo del repartidor.
 * Muestra el stock de garrafones cargados, capacidad del vehículo y estado en línea.
 */

data class ProductoStock(
    val id: String,
    val nombre: String,
    val capacidad: String,
    val cantidad: Int,
    val maximo: Int,
    val precio: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioVehiculoScreen(
    onBack: () -> Unit = {},
    onAjustarStock: (ProductoStock) -> Unit = {},
    onRecargar: () -> Unit = {},
    onIrPedidos: () -> Unit = {}
) {
    val primary = Color(0xFF0077B6)
    val primaryDark = Color(0xFF03045E)
    val success = Color(0xFF10B981)
    val warning = Color(0xFFF59E0B)
    val bg = Color(0xFFF8FAFC)
    val outline = Color(0xFFE2E8F0)
    val text2 = Color(0xFF64748B)

    var enLinea by remember { mutableStateOf(true) }

    val productos = remember {
        listOf(
            ProductoStock("ciel20", "Ciel", "20 L", 18, 30, 45.0),
            ProductoStock("bona20", "Bonafont", "20 L", 8, 15, 50.0),
            ProductoStock("ciel10", "Ciel", "10 L", 12, 20, 30.0)
        )
    }

    val totalActual = productos.sumOf { it.cantidad }
    val totalMaximo = productos.sumOf { it.maximo }
    val porcentaje = if (totalMaximo > 0) totalActual.toFloat() / totalMaximo else 0f

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("Mi inventario", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryDark,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item { EstadoEnLineaCard(enLinea, success, outline) { enLinea = !enLinea } }
            item {
                CapacidadVehiculoCard(
                    actual = totalActual,
                    maximo = totalMaximo,
                    porcentaje = porcentaje,
                    primary = primary
                )
            }
            item {
                Text(
                    "Stock cargado",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = text2,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(productos) { producto ->
                ProductoStockCard(producto, primary, warning, outline) { onAjustarStock(producto) }
            }
            item {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRecargar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, primary)
                ) {
                    Icon(Icons.Default.Add, null, tint = primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Recargar inventario", color = primary, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onIrPedidos,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primary)
                ) {
                    Text("Ver pedidos disponibles", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun EstadoEnLineaCard(enLinea: Boolean, success: Color, outline: Color, onToggle: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, outline),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (enLinea) success else Color.Gray)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (enLinea) "Estás en línea" else "Estás desconectado", fontWeight = FontWeight.SemiBold)
                Text(
                    if (enLinea) "Recibiendo pedidos" else "No recibirás pedidos",
                    fontSize = 12.sp, color = Color.Gray
                )
            }
            Switch(checked = enLinea, onCheckedChange = { onToggle() })
        }
    }
}

@Composable
private fun CapacidadVehiculoCard(actual: Int, maximo: Int, porcentaje: Float, primary: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = primary),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Capacidad del vehículo", color = Color.White.copy(alpha = .8f), fontSize = 12.sp)
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
                Text("$actual", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(" / $maximo", fontSize = 18.sp, color = Color.White.copy(alpha = .7f))
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { porcentaje },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = .25f)
            )
        }
    }
}

@Composable
private fun ProductoStockCard(
    producto: ProductoStock,
    primary: Color,
    warning: Color,
    outline: Color,
    onClick: () -> Unit
) {
    val low = producto.cantidad < producto.maximo * 0.3
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, outline),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(primary.copy(alpha = .12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.WaterDrop, null, tint = primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${producto.nombre} ${producto.capacidad}", fontWeight = FontWeight.SemiBold)
                Text("$%.0f c/u".format(producto.precio), fontSize = 12.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${producto.cantidad}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (low) "Stock bajo" else "${producto.maximo - producto.cantidad} libres",
                    fontSize = 11.sp,
                    color = if (low) warning else Color.Gray
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 868)
@Composable
fun InventarioVehiculoScreenPreview() {
    HToGoTheme { InventarioVehiculoScreen() }
}
