package mx.htogo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.delay
import mx.htogo.ui.theme.HToGoTheme

/**
 * Pantalla 12 — Ruta de entrega activa para el repartidor.
 *
 * 4 estados:
 *  EN_RUTA       → Mapa + km restantes + datos del cliente + botón "Llegué al destino"
 *  LLEGADO       → Temporizador de 10 min de espera obligatorio + botón "No entregado" deshabilitado
 *  NO_ENTREGADO  → Modal con motivo
 *  ENTREGADO     → Modal de confirmación con cobro y cambio (sin evidencia fotográfica)
 */

enum class EstadoRuta { EN_RUTA, LLEGADO }

data class EntregaActiva(
    val pedidoId: String,
    val cliente: String,
    val inicialesCliente: String,
    val ratingCliente: Double,
    val direccion: String,
    val notas: String,
    val productos: String,
    val total: Double,
    val pagaCon: Double,
    val kmRestantes: Double,
    val entregaActual: Int,
    val totalEntregas: Int
)

enum class MotivoNoEntrega(val titulo: String, val subtitulo: String) {
    CLIENTE_NO_ENCONTRADO("Cliente no se encontraba", "Toqué el timbre y nadie respondió"),
    DIRECCION_INCORRECTA("Dirección incorrecta", "No pude localizar la ubicación"),
    CLIENTE_RECHAZO("Cliente rechazó el pedido", "No quiso recibir los garrafones"),
    SIN_DINERO("No tenía dinero", "Cliente no pudo pagar el pedido"),
    OTRO("Otro motivo", "Especificar en notas")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RutaEntregaScreen(
    onBack: () -> Unit = {},
    onEntregado: () -> Unit = {},
    onNoEntregado: (MotivoNoEntrega, String) -> Unit = { _, _ -> }
) {
    val primary = Color(0xFF0077B6)
    val success = Color(0xFF10B981)
    val danger = Color(0xFFEF4444)
    val warning = Color(0xFFF59E0B)
    val bg = Color(0xFFF8FAFC)

    val entrega = remember {
        EntregaActiva(
            pedidoId = "HG-1287",
            cliente = "Andrea Martínez",
            inicialesCliente = "A",
            ratingCliente = 4.9,
            direccion = "Insurgentes Sur 1234, Int. 4B · Del Valle, Benito Juárez",
            notas = "Edificio azul, frente al parque. Tocar timbre 4B.",
            productos = "3 × Ciel 20 L",
            total = 135.0,
            pagaCon = 200.0,
            kmRestantes = 1.2,
            entregaActual = 1,
            totalEntregas = 3
        )
    }

    var estado by remember { mutableStateOf(EstadoRuta.EN_RUTA) }
    var mostrarModalEntregado by remember { mutableStateOf(false) }
    var mostrarModalNoEntregado by remember { mutableStateOf(false) }

    // Temporizador de 10 minutos cuando llega al destino
    var segundosRestantes by remember { mutableStateOf(10 * 60) }
    LaunchedEffect(estado) {
        if (estado == EstadoRuta.LLEGADO) {
            segundosRestantes = 10 * 60
            while (segundosRestantes > 0) {
                delay(1000)
                segundosRestantes--
            }
        }
    }
    val puedeMarcarNoEntregado = estado == EstadoRuta.LLEGADO && segundosRestantes <= 0

    Scaffold(containerColor = bg) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            // Mapa placeholder
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFFDDE7EE)),
                contentAlignment = Alignment.Center
            ) {
                Text("[ Mapa con ruta del repartidor ]", color = Color.Gray)
            }

            // Top floating bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 44.dp, start = 12.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingIconButton(Icons.Default.ArrowBack, onBack)
                Spacer(Modifier.width(8.dp))
                StepPill(
                    estado = estado,
                    pedidoId = entrega.pedidoId,
                    actual = entrega.entregaActual,
                    total = entrega.totalEntregas,
                    success = success
                )
                Spacer(Modifier.width(8.dp))
                FloatingIconButton(Icons.Default.MoreVert, {})
            }

            // Bottom sheet
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .heightIn(max = 560.dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(18.dp)) {
                    if (estado == EstadoRuta.EN_RUTA) {
                        EnRutaSheet(entrega, primary)
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { estado = EstadoRuta.LLEGADO },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(27.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primary)
                        ) {
                            Icon(Icons.Default.Flag, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Llegué al destino", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        LlegadoSheet(entrega, segundosRestantes, success, warning)
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { mostrarModalNoEntregado = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                shape = RoundedCornerShape(27.dp),
                                enabled = puedeMarcarNoEntregado,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp, if (puedeMarcarNoEntregado) danger else Color.Gray
                                )
                            ) {
                                Icon(
                                    if (puedeMarcarNoEntregado) Icons.Default.Cancel else Icons.Default.Lock,
                                    null,
                                    tint = if (puedeMarcarNoEntregado) danger else Color.Gray
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "No entregado",
                                    color = if (puedeMarcarNoEntregado) danger else Color.Gray,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Button(
                                onClick = { mostrarModalEntregado = true },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(54.dp),
                                shape = RoundedCornerShape(27.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = success)
                            ) {
                                Icon(Icons.Default.CheckCircle, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Marcar entregado", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        if (mostrarModalEntregado) {
            ModalEntregado(
                entrega = entrega,
                primary = primary, success = success,
                onDismiss = { mostrarModalEntregado = false },
                onConfirm = { mostrarModalEntregado = false; onEntregado() }
            )
        }
        if (mostrarModalNoEntregado) {
            ModalNoEntregado(
                danger = danger,
                onDismiss = { mostrarModalNoEntregado = false },
                onConfirm = { motivo, nota ->
                    mostrarModalNoEntregado = false
                    onNoEntregado(motivo, nota)
                }
            )
        }
    }
}

@Composable
private fun FloatingIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier.size(40.dp)
    ) {
        IconButton(onClick = onClick) { Icon(icon, null) }
    }
}

@Composable
private fun StepPill(estado: EstadoRuta, pedidoId: String, actual: Int, total: Int, success: Color) {
    val bgColor = if (estado == EstadoRuta.LLEGADO) success else Color.White
    val textColor = if (estado == EstadoRuta.LLEGADO) Color.White else Color(0xFF0F172A)
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = bgColor,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(0.7f)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (estado == EstadoRuta.LLEGADO) {
                Icon(Icons.Default.Flag, null, tint = success, modifier = Modifier
                    .size(22.dp).clip(RoundedCornerShape(50)).background(Color.White).padding(3.dp))
                Spacer(Modifier.width(8.dp))
                Text("Llegaste al destino", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Text(
                    "Entrega $actual de $total · #$pedidoId",
                    color = textColor, fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun EnRutaSheet(entrega: EntregaActiva, primary: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Pedido #${entrega.pedidoId}", fontSize = 12.sp, color = Color.Gray)
                Text(entrega.productos, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            Surface(shape = RoundedCornerShape(99.dp), color = primary.copy(alpha = .12f)) {
                Text("En ruta", color = primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = primary.copy(alpha = .08f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Directions, null, tint = primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("%.1f km restantes".format(entrega.kmRestantes),
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = primary)
                    Text(entrega.direccion.split(" · ").first(), fontSize = 11.sp, color = Color.Gray)
                }
                Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = primary)) {
                    Icon(Icons.Default.Navigation, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Navegar", fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        ClienteCard(entrega, primary)
    }
}

@Composable
private fun LlegadoSheet(entrega: EntregaActiva, segundos: Int, success: Color, warning: Color) {
    val mm = segundos / 60
    val ss = segundos % 60
    val pct = segundos / (10f * 60f)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Pedido #${entrega.pedidoId}", fontSize = 12.sp, color = Color.Gray)
                Text("${entrega.productos} · ${entrega.cliente}",
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Surface(shape = RoundedCornerShape(99.dp), color = success.copy(alpha = .12f)) {
                Text("Llegaste", color = success, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
            }
        }
        Spacer(Modifier.height(10.dp))

        // Timer card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFFEF3C7),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCD34D)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(warning),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Timer, null, tint = Color.White) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Tiempo de espera obligatorio",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF78350F))
                    Text("Podrás marcar como no entregado cuando termine",
                        fontSize = 11.sp, color = Color(0xFF92400E))
                }
                Text("%02d:%02d".format(mm, ss),
                    fontSize = 22.sp, fontWeight = FontWeight.Bold, color = warning)
            }
        }
        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 6.dp),
            color = warning,
            trackColor = Color(0xFFFEF3C7)
        )

        Spacer(Modifier.height(12.dp))
        ClienteCard(entrega, success)
    }
}

@Composable
private fun ClienteCard(entrega: EntregaActiva, accent: Color) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(50)).background(accent),
                contentAlignment = Alignment.Center
            ) { Text(entrega.inicialesCliente, color = Color.White, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entrega.cliente, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("⭐ ${entrega.ratingCliente}", fontSize = 12.sp, color = Color.Gray)
            }
            IconButton(
                onClick = {},
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(50)).background(Color(0xFF10B981))
            ) { Icon(Icons.Default.Call, null, tint = Color.White) }
        }
    }
}

@Composable
private fun ModalEntregado(
    entrega: EntregaActiva, primary: Color, success: Color,
    onDismiss: () -> Unit, onConfirm: () -> Unit
) {
    val danger = Color(0xFFEF4444)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar entrega", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Text("Verifica el cobro antes de confirmar.", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = success.copy(alpha = .08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, success.copy(alpha = .25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payments, null, tint = success)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Total cobrado en efectivo", fontSize = 11.sp, color = Color.Gray)
                            Text("$%.0f MXN".format(entrega.total),
                                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = success)
                            Text("${entrega.productos} · ${entrega.cliente}",
                                fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Savings, null, tint = primary)
                        Spacer(Modifier.width(10.dp))
                        Text("Cambio entregado al cliente", fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text("$%.0f".format(entrega.pagaCon - entrega.total),
                            fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = success)) {
                Icon(Icons.Default.CheckCircle, null)
                Spacer(Modifier.width(4.dp))
                Text("Confirmar entrega")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun ModalNoEntregado(
    danger: Color,
    onDismiss: () -> Unit,
    onConfirm: (MotivoNoEntrega, String) -> Unit
) {
    var seleccionado by remember { mutableStateOf<MotivoNoEntrega?>(MotivoNoEntrega.CLIENTE_NO_ENCONTRADO) }
    var nota by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Por qué no se entregó?", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                MotivoNoEntrega.values().forEach { m ->
                    val sel = seleccionado == m
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (sel) danger.copy(alpha = .04f) else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp, if (sel) danger else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(role = androidx.compose.ui.semantics.Role.RadioButton) { seleccionado = m }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(m.titulo, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(m.subtitulo, fontSize = 11.sp, color = Color.Gray)
                            }
                            RadioButton(selected = sel, onClick = { seleccionado = m },
                                colors = RadioButtonDefaults.colors(selectedColor = danger))
                        }
                    }
                }
                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = { Text("Notas adicionales (opcional)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { seleccionado?.let { onConfirm(it, nota) } },
                colors = ButtonDefaults.buttonColors(containerColor = danger)
            ) {
                Icon(Icons.Default.Cancel, null)
                Spacer(Modifier.width(4.dp))
                Text("Reportar no entrega")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Preview(showBackground = true, widthDp = 412, heightDp = 868)
@Composable
fun RutaEntregaScreenPreview() {
    HToGoTheme { RutaEntregaScreen() }
}
