package com.htogo.app.ui.screens

import androidx.compose.foundation.BorderStroke
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htogo.app.data.dto.PedidoResponse
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme

/** Datos del pedido ya listos para mostrar (formateados desde [PedidoResponse]). */
private data class PedidoApartadoUi(
    val id: Int,
    val estado: String,
    val productos: String,
    val cliente: String,
    val telefono: String?,
    val direccion: String,
    val notas: String,
    val total: Double,
    val programadoPara: String?
)

private fun aUi(p: PedidoResponse, nombresMarca: Map<Int, String>): PedidoApartadoUi {
    val productos = p.detalles.orEmpty().takeIf { it.isNotEmpty() }
        ?.joinToString(", ") { d -> "${d.cantidad} × ${d.nombreMarca ?: nombresMarca[d.idMarca] ?: "Garrafón"} · 20 L" }
        ?: "${p.garrafonesTotales ?: 0} × Garrafón · 20 L"
    return PedidoApartadoUi(
        id = p.id,
        estado = p.estado.lowercase(),
        productos = productos,
        cliente = p.nombreCliente?.takeIf { it.isNotBlank() } ?: "Cliente",
        telefono = p.telefonoCliente,
        direccion = p.direccionTexto?.takeIf { it.isNotBlank() } ?: "Dirección no disponible",
        notas = p.indicaciones?.takeIf { it.isNotBlank() } ?: "Sin indicaciones",
        total = p.totalPagar ?: 0.0,
        programadoPara = p.fechaProgramada?.takeIf { p.esProgramado }?.let(::fechaLegible)
    )
}

/** "2026-04-26T11:30:00-06:00" → "Domingo 26 de abril · 11:30 a. m." (minSdk 24: sin java.time). */
private fun fechaLegible(iso: String): String = try {
    val limpio = iso.replace(Regex("\\.\\d+"), "").replace("Z", "+00:00")
    val fecha = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US).parse(limpio)
    val es = java.util.Locale("es", "MX")
    val dia = java.text.SimpleDateFormat("EEEE d 'de' MMMM", es).format(fecha!!).replaceFirstChar { it.uppercase() }
    val hora = java.text.SimpleDateFormat("h:mm a", es).format(fecha)
    "$dia · $hora"
} catch (e: Exception) {
    iso.take(16).replace('T', ' ')
}

/**
 * Pedido que el repartidor ya aceptó (apartado). Muestra el pedido real: [pedido] es null
 * mientras se carga su detalle.
 */
@Composable
fun SolicitudPedidoProgramadoScreen(
    pedido: PedidoResponse? = null,
    nombresMarca: Map<Int, String> = emptyMap(),
    onBack: () -> Unit = {},
    onIniciarRuta: () -> Unit = {}
) {
    val context = LocalContext.current
    if (pedido == null) {
        Box(Modifier.fillMaxSize().background(HToGoColors.Background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = HToGoColors.Primary)
        }
        return
    }
    val ui = remember(pedido, nombresMarca) { aUi(pedido, nombresMarca) }
    val enRuta = ui.estado == "asignado" || ui.estado == "en_camino"
    val chip = when (ui.estado) {
        "asignado" -> "Aceptado · Te lo apartaste"
        "en_camino" -> "En camino"
        "entregado" -> "Entregado"
        "no_entregado" -> "No entregado"
        "cancelado" -> "Cancelado por el cliente"
        else -> ui.estado.replaceFirstChar { it.uppercase() }
    }

    Scaffold(
        containerColor = HToGoColors.Background,
        bottomBar = {
            if (enRuta) Surface(color = Color.White, shadowElevation = 8.dp) {
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
                        Text(
                            if (ui.estado == "en_camino") "Continuar ruta" else "Iniciar ruta",
                            color = Color.White, fontWeight = FontWeight.SemiBold
                        )
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
                                chip,
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
                            Text("Pedido #${ui.id}", fontSize = 12.sp, color = HToGoColors.TextSecondary)
                            Text(
                                ui.productos,
                                fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                                color = HToGoColors.TextPrimary
                            )
                        }
                    }
                    if (ui.programadoPara != null) {
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
                                ui.programadoPara,
                                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                color = HToGoColors.PrimaryDark
                            )
                        }
                    }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            SectionTitle("Detalle del cliente")
            InfoRow(Icons.Filled.Person, "Cliente", ui.cliente)
            if (!ui.telefono.isNullOrBlank()) {
                InfoRow(Icons.Filled.Call, "Teléfono", ui.telefono, onClick = {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${ui.telefono}")))
                })
            }
            InfoRow(Icons.Filled.LocationOn, "Domicilio", ui.direccion)
            InfoRow(Icons.Filled.Notes, "Notas del cliente", ui.notas)

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
                            "$%.0f MXN".format(ui.total),
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
private fun InfoRow(icon: ImageVector, label: String, value: String, onClick: (() -> Unit)? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
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
    HToGoTheme { SolicitudPedidoProgramadoScreen(pedido = null) }
}
