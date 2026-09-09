package com.htogo.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme
import kotlinx.coroutines.delay

enum class EstadoRuta { EN_RUTA, LLEGADO }

data class EntregaActiva(
    val pedidoId: String,
    val cliente: String,
    val inicialesCliente: String,
    val direccion: String,
    val notas: String,
    val productos: String,
    val total: Double,
    val kmRestantes: Double,
    val entregaActual: Int,
    val totalEntregas: Int
)

enum class MotivoNoEntrega(val titulo: String, val subtitulo: String) {
    CLIENTE_NO_ENCONTRADO("Cliente no se encontraba", "Toqué el timbre y nadie respondió"),
    DIRECCION_INCORRECTA("Dirección incorrecta", "No pude localizar la ubicación"),
    CLIENTE_RECHAZO("Cliente rechazó el pedido", "No quiso recibir los garrafones"),
    OTRO("Otro motivo", "Especificar en notas")
}

@Composable
fun RutaEntregaScreen(
    onBack: () -> Unit = {},
    onCompletada: () -> Unit = {}
) {
    val entrega = remember {
        EntregaActiva(
            pedidoId = "HG-1287",
            cliente = "Andrea Martínez",
            inicialesCliente = "A",
            direccion = "Insurgentes Sur 1234, Int. 4B · Del Valle, Benito Juárez",
            notas = "Edificio azul, frente al parque. Tocar timbre 4B.",
            productos = "3 × Ciel 20 L",
            total = 135.0,
            kmRestantes = 1.2,
            entregaActual = 1,
            totalEntregas = 3
        )
    }

    var estado by remember { mutableStateOf(EstadoRuta.EN_RUTA) }
    var mostrarModalEntregado by remember { mutableStateOf(false) }
    var mostrarModalNoEntregado by remember { mutableStateOf(false) }

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

    Scaffold(containerColor = HToGoColors.Background) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Box(
                Modifier.fillMaxSize().background(Color(0xFFDDE7EE)),
                contentAlignment = Alignment.Center
            ) {
                Text("[ Mapa con ruta del repartidor ]", color = HToGoColors.TextSecondary)
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 44.dp, start = 12.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingIconButton(Icons.AutoMirrored.Filled.ArrowBack, onBack)
                Spacer(Modifier.width(8.dp))
                StepPill(
                    estado = estado,
                    pedidoId = entrega.pedidoId,
                    actual = entrega.entregaActual,
                    total = entrega.totalEntregas
                )
                Spacer(Modifier.width(8.dp))
                FloatingIconButton(Icons.Filled.MoreVert, {})
            }

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
                        EnRutaSheet(entrega)
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { estado = EstadoRuta.LLEGADO },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(27.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary)
                        ) {
                            Icon(Icons.Filled.Flag, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Llegué al destino", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        LlegadoSheet(entrega, segundosRestantes)
                        if (segundosRestantes > 0) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(
                                onClick = { segundosRestantes = 0 },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "DEMO · Saltar los 10 min",
                                    fontSize = 12.sp,
                                    color = HToGoColors.TextTertiary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { mostrarModalNoEntregado = true },
                                modifier = Modifier.weight(1f).height(54.dp),
                                shape = RoundedCornerShape(27.dp),
                                enabled = puedeMarcarNoEntregado,
                                border = BorderStroke(
                                    1.5.dp,
                                    if (puedeMarcarNoEntregado) HToGoColors.AccentRose else HToGoColors.OutlineSoft
                                )
                            ) {
                                Icon(
                                    if (puedeMarcarNoEntregado) Icons.Filled.Cancel else Icons.Filled.Lock,
                                    null,
                                    tint = if (puedeMarcarNoEntregado) HToGoColors.AccentRose else HToGoColors.TextTertiary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "No entregado",
                                    color = if (puedeMarcarNoEntregado) HToGoColors.AccentRose else HToGoColors.TextTertiary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Button(
                                onClick = { mostrarModalEntregado = true },
                                modifier = Modifier.weight(1.5f).height(54.dp),
                                shape = RoundedCornerShape(27.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = HToGoColors.StatusEntregado
                                )
                            ) {
                                Icon(Icons.Filled.CheckCircle, null)
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
                onDismiss = { mostrarModalEntregado = false },
                onConfirm = {
                    mostrarModalEntregado = false
                    onCompletada()
                }
            )
        }
        if (mostrarModalNoEntregado) {
            ModalNoEntregado(
                onDismiss = { mostrarModalNoEntregado = false },
                onConfirm = { motivo, nota ->
                    mostrarModalNoEntregado = false
                    // Más adelante aquí podrás mandar a la base de datos el 'motivo' y la 'nota'
                }
            )
        }
    }
}

@Composable
private fun FloatingIconButton(icon: ImageVector, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier.size(40.dp)
    ) {
        IconButton(onClick = onClick) { Icon(icon, null, tint = HToGoColors.TextPrimary) }
    }
}

@Composable
private fun StepPill(estado: EstadoRuta, pedidoId: String, actual: Int, total: Int) {
    val bgColor = if (estado == EstadoRuta.LLEGADO) HToGoColors.StatusEntregado else Color.White
    val textColor = if (estado == EstadoRuta.LLEGADO) Color.White else HToGoColors.TextPrimary
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
                Icon(
                    Icons.Filled.Flag, null,
                    tint = HToGoColors.StatusEntregado,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                        .padding(3.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Llegaste al destino",
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    "Entrega $actual de $total · #$pedidoId",
                    color = textColor,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun EnRutaSheet(entrega: EntregaActiva) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Pedido #${entrega.pedidoId}",
                    fontSize = 12.sp,
                    color = HToGoColors.TextSecondary
                )
                Text(
                    entrega.productos,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HToGoColors.TextPrimary
                )
            }
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = HToGoColors.Primary.copy(alpha = .12f)
            ) {
                Text(
                    "En ruta",
                    color = HToGoColors.Primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = HToGoColors.Primary.copy(alpha = .08f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Directions, null, tint = HToGoColors.Primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "%.1f km restantes".format(entrega.kmRestantes),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = HToGoColors.Primary
                    )
                    Text(
                        entrega.direccion.split(" · ").first(),
                        fontSize = 11.sp,
                        color = HToGoColors.TextSecondary
                    )
                }
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary)
                ) {
                    Icon(Icons.Filled.Navigation, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Navegar", fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        ClienteCard(entrega, HToGoColors.Primary)
        Spacer(Modifier.height(12.dp))
        PagoBox(entrega, soloTotal = true)
    }
}

@Composable
private fun LlegadoSheet(entrega: EntregaActiva, segundos: Int) {
    val mm = segundos / 60
    val ss = segundos % 60
    val pct = segundos / (10f * 60f)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Pedido #${entrega.pedidoId}",
                    fontSize = 12.sp,
                    color = HToGoColors.TextSecondary
                )
                Text(
                    "${entrega.productos} · ${entrega.cliente}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HToGoColors.TextPrimary
                )
            }
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = HToGoColors.StatusEntregado.copy(alpha = .12f)
            ) {
                Text(
                    "Llegaste",
                    color = HToGoColors.StatusEntregado,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFFEF3C7),
            border = BorderStroke(1.dp, Color(0xFFFCD34D)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(HToGoColors.AccentAmber),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Timer, null, tint = Color.White) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Tiempo de espera obligatorio",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF78350F)
                    )
                    Text(
                        "Podrás marcar como no entregado cuando termine",
                        fontSize = 11.sp,
                        color = Color(0xFF92400E)
                    )
                }
                Text(
                    "%02d:%02d".format(mm, ss),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = HToGoColors.AccentAmber
                )
            }
        }
        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 6.dp),
            color = HToGoColors.AccentAmber,
            trackColor = Color(0xFFFEF3C7)
        )

        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = HToGoColors.PrimaryWash,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Payments, null, tint = HToGoColors.Primary)
                Spacer(Modifier.width(10.dp))
                Text(
                    entrega.productos,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HToGoColors.TextPrimary
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        ClienteCard(entrega, HToGoColors.StatusEntregado)
    }
}

@Composable
private fun ClienteCard(entrega: EntregaActiva, accent: Color) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = HToGoColors.Background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(50)).background(accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    entrega.inicialesCliente,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    entrega.cliente,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HToGoColors.TextPrimary
                )
                Text(
                    "Cliente",
                    fontSize = 12.sp,
                    color = HToGoColors.TextSecondary
                )
            }
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(50))
                    .background(HToGoColors.StatusEntregado)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Call, null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun PagoBox(entrega: EntregaActiva, soloTotal: Boolean) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = HToGoColors.StatusEntregado.copy(alpha = .08f),
        border = BorderStroke(1.dp, HToGoColors.StatusEntregado.copy(alpha = .25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Payments, null, tint = HToGoColors.StatusEntregado)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "Cobrarás en efectivo",
                    fontSize = 11.sp,
                    color = HToGoColors.TextSecondary
                )
                Text(
                    "$%.0f MXN".format(entrega.total),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HToGoColors.StatusEntregado
                )
            }
        }
    }
}

@Composable
private fun ModalEntregado(
    entrega: EntregaActiva,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar entrega", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Text(
                    "Verifica el cobro antes de confirmar.",
                    fontSize = 13.sp,
                    color = HToGoColors.TextSecondary
                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = HToGoColors.StatusEntregado.copy(alpha = .08f),
                    border = BorderStroke(1.dp, HToGoColors.StatusEntregado.copy(alpha = .25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Payments, null, tint = HToGoColors.StatusEntregado)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Total cobrado en efectivo",
                                fontSize = 11.sp,
                                color = HToGoColors.TextSecondary
                            )
                            Text(
                                "$%.0f MXN".format(entrega.total),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = HToGoColors.StatusEntregado
                            )
                            Text(
                                "${entrega.productos} · ${entrega.cliente}",
                                fontSize = 11.sp,
                                color = HToGoColors.TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.StatusEntregado)
            ) {
                Icon(Icons.Filled.CheckCircle, null)
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
                        color = if (sel) HToGoColors.AccentRose.copy(alpha = .04f) else Color.White,
                        border = BorderStroke(
                            1.5.dp,
                            if (sel) HToGoColors.AccentRose else HToGoColors.OutlineSoft
                        ),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(role = Role.RadioButton) { seleccionado = m }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    m.titulo,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = HToGoColors.TextPrimary
                                )
                                Text(
                                    m.subtitulo,
                                    fontSize = 11.sp,
                                    color = HToGoColors.TextSecondary
                                )
                            }
                            RadioButton(
                                selected = sel,
                                onClick = { seleccionado = m },
                                colors = RadioButtonDefaults.colors(selectedColor = HToGoColors.AccentRose)
                            )
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
                colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.AccentRose)
            ) {
                Icon(Icons.Filled.Cancel, null)
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
