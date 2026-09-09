package com.htogo.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme

private data class AbiertoAddr(
    val id: String,
    val label: String,
    val address: String,
    val icon: ImageVector,
    val isDefault: Boolean = false
)

private val ABIERTO_ADDRESSES = listOf(
    AbiertoAddr("a1", "Casa", "Av. Insurgentes Sur 1234, Int. 4B, Col. Del Valle", Icons.Filled.Home, true),
    AbiertoAddr("a2", "Oficina", "Av. Universidad 567, Piso 8, Col. Narvarte", Icons.Filled.Work),
    AbiertoAddr("a3", "Casa de mamá", "Calle Heriberto Frías 890, Col. Nápoles", Icons.Filled.Favorite)
)

private val PRECIOS_PRESET = listOf(35, 40, 45, 50)
private const val ENVIO_ABIERTO = 15

@Composable
fun NuevoPedidoAbiertoScreen(
    onBack: () -> Unit = {},
    onConfirm: () -> Unit = {}
) {
    var qty by remember { mutableStateOf(3) }
    var precioMax by remember { mutableStateOf(45) }
    var selectedAddrId by remember { mutableStateOf("a1") }
    var notes by remember { mutableStateOf("Tocar timbre 4B, dejar en recepción si no contesto") }

    val selectedAddr = ABIERTO_ADDRESSES.first { it.id == selectedAddrId }
    val totalMax = qty * precioMax + ENVIO_ABIERTO
    val canConfirm = qty > 0 && precioMax >= 25

    Scaffold(
        topBar = { TopBar(onBack) },
        bottomBar = {
            CtaBar(
                totalMax = totalMax,
                enabled = canConfirm,
                onConfirm = onConfirm
            )
        },
        containerColor = HToGoColors.Background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            HeroExplicacion()

            Spacer(Modifier.height(20.dp))
            SectionTitle("Tu precio máximo por garrafón")
            Spacer(Modifier.height(10.dp))
            Box(Modifier.padding(horizontal = 16.dp)) {
                PrecioMaximoCard(
                    precio = precioMax,
                    onPrecio = { precioMax = it.coerceIn(25, 80) }
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.padding(horizontal = 16.dp)) {
                PresetPrecios(precio = precioMax) { precioMax = it }
            }
            Spacer(Modifier.height(8.dp))
            HintRow(precio = precioMax)

            Spacer(Modifier.height(20.dp))
            SectionTitle("¿Cuántos garrafones?")
            Spacer(Modifier.height(10.dp))
            Box(Modifier.padding(horizontal = 16.dp)) {
                CantidadCard(qty = qty, onQty = { qty = it.coerceIn(1, 30) })
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.padding(horizontal = 16.dp)) {
                PresetCantidad(qty = qty) { qty = it }
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("Domicilio de entrega")
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ABIERTO_ADDRESSES.forEach { a ->
                    AddrCard(addr = a, selected = a.id == selectedAddrId) { selectedAddrId = a.id }
                }
                AgregarDireccionBtn {}
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("Indicaciones para el repartidor")
            Spacer(Modifier.height(10.dp))
            Box(Modifier.padding(horizontal = 16.dp)) {
                NotasCard(notes) { notes = it }
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("Resumen")
            Spacer(Modifier.height(10.dp))
            Box(Modifier.padding(horizontal = 16.dp)) {
                ResumenAbiertoCard(qty = qty, precioMax = precioMax, envio = ENVIO_ABIERTO)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Surface(color = HToGoColors.PrimaryDark) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = .10f),
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onBack)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Pedido con precio máximo",
                    fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White
                )
                Text(
                    "Modo abierto · primero en aceptar",
                    fontSize = 12.sp, color = HToGoColors.PrimarySoft
                )
            }
            Surface(shape = RoundedCornerShape(99.dp), color = Color.White.copy(alpha = .10f)) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Abierto",
                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroExplicacion() {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(HToGoColors.PrimaryDark, HToGoColors.Primary)))
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(HToGoColors.PrimarySoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Speed, null, tint = HToGoColors.Primary, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Tú pones el precio tope",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HToGoColors.TextPrimary
                    )
                    Text(
                        "Cualquier purificadora cercana puede tomar el pedido si su precio es ≤ tu tope. Te asignamos al primero que acepte.",
                        fontSize = 12.sp,
                        color = HToGoColors.TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = HToGoColors.TextSecondary,
        letterSpacing = 0.4.sp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun PrecioMaximoCard(precio: Int, onPrecio: (Int) -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, HToGoColors.OutlineSoft),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(HToGoColors.PrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.AttachMoney, null, tint = HToGoColors.Primary, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Por garrafón 20 L",
                    fontSize = 12.sp,
                    color = HToGoColors.TextSecondary
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "$$precio",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = HToGoColors.Primary
                    )
                    Text(
                        " MXN",
                        fontSize = 13.sp,
                        color = HToGoColors.TextSecondary
                    )
                }
            }
            Row(
                Modifier
                    .background(HToGoColors.Background, RoundedCornerShape(99.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepBtn(label = "−", enabled = precio > 25) { onPrecio(precio - 1) }
                Text(
                    "ajustar",
                    Modifier.padding(horizontal = 8.dp),
                    fontSize = 11.sp,
                    color = HToGoColors.TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                StepBtn(label = "+", enabled = precio < 80) { onPrecio(precio + 1) }
            }
        }
    }
}

@Composable
private fun StepBtn(label: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (enabled) Color.White else Color.Transparent,
        shadowElevation = if (enabled) 1.dp else 0.dp,
        modifier = Modifier
            .size(36.dp)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) HToGoColors.Primary else HToGoColors.TextTertiary
            )
        }
    }
}

@Composable
private fun PresetPrecios(precio: Int, onPick: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PRECIOS_PRESET.forEach { p ->
            val active = p == precio
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (active) HToGoColors.PrimarySoft else Color.White,
                border = BorderStroke(
                    1.5.dp,
                    if (active) HToGoColors.Primary else HToGoColors.OutlineSoft
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clickable { onPick(p) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "$$p",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (active) HToGoColors.Primary else HToGoColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun HintRow(precio: Int) {
    val (texto, color) = when {
        precio < 35 -> "Pocas purificadoras aceptarán este precio" to HToGoColors.AccentRose
        precio in 35..40 -> "Probable que tarde unos minutos" to HToGoColors.AccentAmber
        else -> "Asignación inmediata, varias purificadoras lo aceptan" to HToGoColors.AccentEmerald
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            texto,
            fontSize = 11.sp,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CantidadCard(qty: Int, onQty: (Int) -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, HToGoColors.OutlineSoft),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(HToGoColors.PrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.WaterDrop, null, tint = HToGoColors.Primary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Garrafón 20 L",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HToGoColors.TextPrimary
                )
                Text(
                    "$qty pieza${if (qty == 1) "" else "s"}",
                    fontSize = 13.sp,
                    color = HToGoColors.TextSecondary
                )
            }
            Row(
                Modifier
                    .background(HToGoColors.Background, RoundedCornerShape(99.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepBtn("−", enabled = qty > 1) { onQty(qty - 1) }
                Text(
                    qty.toString(),
                    Modifier
                        .widthIn(min = 28.dp)
                        .padding(horizontal = 4.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HToGoColors.TextPrimary
                )
                StepBtn("+", enabled = qty < 30) { onQty(qty + 1) }
            }
        }
    }
}

@Composable
private fun PresetCantidad(qty: Int, onPick: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(1, 3, 5, 10).forEach { n ->
            val active = n == qty
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (active) HToGoColors.PrimarySoft else Color.White,
                border = BorderStroke(
                    1.5.dp,
                    if (active) HToGoColors.Primary else HToGoColors.OutlineSoft
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clickable { onPick(n) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        n.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (active) HToGoColors.Primary else HToGoColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun AddrCard(addr: AbiertoAddr, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        border = BorderStroke(
            1.5.dp,
            if (selected) HToGoColors.Primary else HToGoColors.OutlineSoft
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(HToGoColors.PrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(addr.icon, null, tint = HToGoColors.Primary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        addr.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HToGoColors.TextPrimary
                    )
                    if (addr.isDefault) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = HToGoColors.PrimarySoft
                        ) {
                            Text(
                                "Predeterminado",
                                Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HToGoColors.Primary
                            )
                        }
                    }
                }
                Text(
                    addr.address,
                    fontSize = 12.sp,
                    color = HToGoColors.TextSecondary,
                    maxLines = 1
                )
            }
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(
                        2.dp,
                        if (selected) HToGoColors.Primary else HToGoColors.OutlineSoft,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(HToGoColors.Primary)
                )
            }
        }
    }
}

@Composable
private fun AgregarDireccionBtn(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        border = BorderStroke(1.5.dp, HToGoColors.OutlineSoft),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.AddLocationAlt, null, tint = HToGoColors.Primary)
            Spacer(Modifier.width(10.dp))
            Text(
                "Agregar nueva ubicación",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = HToGoColors.Primary
            )
        }
    }
}

@Composable
private fun NotasCard(value: String, onChange: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, HToGoColors.OutlineSoft),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.EditNote, null, tint = HToGoColors.TextTertiary)
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = HToGoColors.TextPrimary, fontSize = 14.sp),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ResumenAbiertoCard(qty: Int, precioMax: Int, envio: Int) {
    val sub = qty * precioMax
    val total = sub + envio
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, HToGoColors.OutlineSoft),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            ResumenRow("$qty × Garrafón 20 L", "≤ $$sub.00")
            ResumenRow("Envío", "$$envio.00")
            HorizontalDivider(
                Modifier.padding(vertical = 8.dp),
                color = HToGoColors.OutlineSoft
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Tope total a pagar",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HToGoColors.TextPrimary
                    )
                    Text(
                        "El precio final puede ser menor según la purificadora que acepte",
                        fontSize = 10.sp,
                        color = HToGoColors.TextTertiary,
                        lineHeight = 13.sp
                    )
                }
                Text(
                    "≤ $$total",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = HToGoColors.Primary
                )
            }
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = HToGoColors.PrimarySoft,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Verified, null,
                        tint = HToGoColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Solo cobramos lo que cobre la purificadora que acepte. Si nadie acepta en 10 min, te avisamos.",
                        fontSize = 11.sp,
                        color = HToGoColors.PrimaryDark,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ResumenRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = HToGoColors.TextSecondary)
        Text(value, fontSize = 13.sp, color = HToGoColors.TextSecondary)
    }
}

@Composable
private fun CtaBar(totalMax: Int, enabled: Boolean, onConfirm: () -> Unit) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "TOPE",
                    fontSize = 11.sp,
                    color = HToGoColors.TextSecondary,
                    letterSpacing = 0.4.sp
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "≤ $$totalMax",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = HToGoColors.TextPrimary
                    )
                    Text(
                        " MXN",
                        fontSize = 13.sp,
                        color = HToGoColors.TextSecondary
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onConfirm,
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary)
            ) {
                Text(
                    "Buscar repartidor",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 900, name = "Pedido abierto")
@Composable
fun NuevoPedidoAbiertoPreview() { HToGoTheme { NuevoPedidoAbiertoScreen() } }
