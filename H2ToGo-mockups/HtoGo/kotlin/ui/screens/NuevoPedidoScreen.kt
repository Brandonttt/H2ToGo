package com.htogo.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WaterDrop
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
import com.htogo.ui.components.HToGoButton
import com.htogo.ui.theme.HToGoColors
import com.htogo.ui.theme.HToGoTheme

private const val PRECIO = 35

@Composable
fun NuevoPedidoScreen(
    onBack: () -> Unit = {},
    onConfirmar: (Int) -> Unit = {}
) {
    var cantidad by remember { mutableStateOf(3) }
    var notas by remember { mutableStateOf("") }
    val total = cantidad * PRECIO

    Scaffold(
        containerColor = HToGoColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Nuevo pedido", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = HToGoColors.Background)
            )
        },
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 8.dp) {
                Column(Modifier.padding(20.dp)) {
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text("Total", color = HToGoColors.TextSecondary, fontSize = 12.sp)
                            Text("\$${total} MXN",
                                fontSize = 24.sp, fontWeight = FontWeight.Bold,
                                color = HToGoColors.TextPrimary)
                        }
                        Text("$cantidad × \$${PRECIO}",
                            color = HToGoColors.TextSecondary, fontSize = 13.sp,
                            modifier = Modifier.align(Alignment.CenterVertically))
                    }
                    Spacer(Modifier.height(12.dp))
                    HToGoButton(text = "Confirmar pedido", onClick = { onConfirmar(cantidad) })
                }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Cantidad
            Column {
                Text("¿Cuántos garrafones?",
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    color = HToGoColors.TextPrimary)
                Spacer(Modifier.height(12.dp))
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QtyButton(Icons.Filled.Remove, enabled = cantidad > 1) {
                            if (cantidad > 1) cantidad--
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.size(72.dp).clip(CircleShape).background(HToGoColors.PrimarySoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.WaterDrop, null,
                                    tint = HToGoColors.Primary, modifier = Modifier.size(36.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("$cantidad",
                                fontSize = 36.sp, fontWeight = FontWeight.Bold,
                                color = HToGoColors.TextPrimary)
                            Text(if (cantidad == 1) "garrafón" else "garrafones",
                                fontSize = 13.sp, color = HToGoColors.TextSecondary)
                        }
                        QtyButton(Icons.Filled.Add, enabled = cantidad < 20) {
                            if (cantidad < 20) cantidad++
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 3, 5, 10).forEach { n ->
                        QuickQty(n, selected = cantidad == n) { cantidad = n }
                    }
                }
            }

            // Domicilio + mapa
            Column {
                Text("Confirmar domicilio",
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    color = HToGoColors.TextPrimary)
                Spacer(Modifier.height(12.dp))
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        // Map placeholder
                        Box(
                            Modifier.fillMaxWidth().height(180.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFD9EEF8), Color(0xFFC0E0EE))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier.size(48.dp).clip(CircleShape).background(HToGoColors.Primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.LocationOn, null, tint = Color.White,
                                    modifier = Modifier.size(28.dp))
                            }
                            FloatingActionButton(
                                onClick = {},
                                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                                containerColor = Color.White,
                                contentColor = HToGoColors.Primary,
                                shape = CircleShape
                            ) { Icon(Icons.Filled.MyLocation, null) }
                        }
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.LocationOn, null,
                                tint = HToGoColors.Primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Av. Insurgentes Sur 1234",
                                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                    color = HToGoColors.TextPrimary)
                                Text("Col. Del Valle, Benito Juárez",
                                    fontSize = 12.sp, color = HToGoColors.TextSecondary)
                            }
                            IconButton(onClick = {}) {
                                Icon(Icons.Filled.Edit, null, tint = HToGoColors.Primary)
                            }
                        }
                    }
                }
            }

            // Notas
            Column {
                Text("Notas para el repartidor (opcional)",
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    color = HToGoColors.TextPrimary)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = notas, onValueChange = { notas = it },
                    placeholder = { Text("Ej. Tocar el timbre del depto 3B") },
                    modifier = Modifier.fillMaxWidth().height(96.dp),
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = { Icon(Icons.Filled.Note, null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HToGoColors.Primary,
                        unfocusedBorderColor = HToGoColors.OutlineSoft
                    )
                )
            }

            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun QtyButton(icon: androidx.compose.ui.graphics.vector.ImageVector,
                      enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape)
            .background(if (enabled) HToGoColors.PrimarySoft else HToGoColors.OutlineSoft)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null,
            tint = if (enabled) HToGoColors.Primary else HToGoColors.TextTertiary)
    }
}

@Composable
private fun QuickQty(n: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) HToGoColors.Primary else Color.White)
            .border(1.dp,
                if (selected) HToGoColors.Primary else HToGoColors.OutlineSoft,
                RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("$n", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else HToGoColors.TextPrimary)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun NuevoPedidoPreview() { HToGoTheme { NuevoPedidoScreen() } }
