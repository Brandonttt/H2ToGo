package com.htogo.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme

data class PedidoProgramado(
    val id: String,
    val cliente: String,
    val direccion: String,
    val colonia: String,
    val productos: String,
    val cantidad: Int,
    val total: Int,
    val fechaProgramada: String,
    val horaProgramada: String,
    val notasCliente: String
)

private val SAMPLE_PEDIDO = PedidoProgramado(
    id = "HG-1290",
    cliente = "María López",
    direccion = "Calle Heriberto Frías 890, Int. 2A",
    colonia = "Col. Nápoles, Benito Juárez",
    productos = "Ciel · 20 L",
    cantidad = 5,
    total = 200,
    fechaProgramada = "Lunes 26 de abril",
    horaProgramada = "11:30 AM",
    notasCliente = "Edificio blanco, dejar en recepción si no contesto. Preguntar por María."
)

@Composable
fun SolicitudPedidoProgramadoScreen(
    onBack: () -> Unit = {},
    onIniciarRuta: () -> Unit = {}
) {
    val pedido = SAMPLE_PEDIDO

    Scaffold(
        containerColor = HToGoColors.Background,
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 8.dp) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .padding(bottom = 8.dp)
                ) {
                    Button(
                        onClick = onIniciarRuta,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary)
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Iniciar ruta", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(HToGoColors.PrimaryDark, HToGoColors.Primary)))
                    .padding(start = 8.dp, end = 18.dp, top = 8.dp, bottom = 20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                        }
                        Text(
                            "Pedido apartado",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f).padding(start = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = HToGoColors.AccentEmerald.copy(alpha = .9f),
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Aceptado · Te lo apartaste",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).background(HToGoColors.PrimarySoft),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.WaterDrop, null, tint = HToGoColors.Primary) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Pedido #${pedido.id}", fontSize = 12.sp, color = HToGoColors.TextSecondary)
                            Text(
                                "${pedido.cantidad} × ${pedido.productos}",
                                fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                                color = HToGoColors.TextPrimary
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(HToGoColors.PrimaryWash)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Schedule, null, tint = HToGoColors.Primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Programado para",
                                fontSize = 11.sp, color = HToGoColors.TextSecondary,
                                fontWeight = FontWeight.SemiBold)
                            Text(
                                "${pedido.fechaProgramada} · ${pedido.horaProgramada}",
                                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                color = HToGoColors.PrimaryDark
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            SectionTitle("Detalle del cliente")
            InfoRow(Icons.Filled.Person, "Cliente", pedido.cliente)
            InfoRow(Icons.Filled.LocationOn, "Domicilio", "${pedido.direccion}\n${pedido.colonia}")
            InfoRow(Icons.Filled.Notes, "Notas del cliente", pedido.notasCliente)

            Spacer(Modifier.height(14.dp))

            SectionTitle("Cobro")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = HToGoColors.AccentEmerald.copy(alpha = .08f)),
                border = BorderStroke(1.dp, HToGoColors.AccentEmerald.copy(alpha = .25f))
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AttachMoney, null, tint = HToGoColors.AccentEmerald,
                        modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Cobrarás en efectivo",
                            fontSize = 12.sp, color = HToGoColors.TextSecondary)
                        Text(
                            "$${pedido.total} MXN",
                            fontSize = 24.sp, fontWeight = FontWeight.Bold,
                            color = HToGoColors.AccentEmerald
                        )
                    }
                }
            }

            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = HToGoColors.TextSecondary,
        modifier = Modifier.padding(start = 22.dp, end = 18.dp, top = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, HToGoColors.OutlineSoft)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(HToGoColors.PrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = HToGoColors.Primary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 11.sp, color = HToGoColors.TextSecondary)
                Text(value,
                    fontSize = 14.sp, color = HToGoColors.TextPrimary,
                    lineHeight = 18.sp)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 868)
@Composable
fun SolicitudPedidoProgramadoScreenPreview() {
    HToGoTheme { SolicitudPedidoProgramadoScreen() }
}
