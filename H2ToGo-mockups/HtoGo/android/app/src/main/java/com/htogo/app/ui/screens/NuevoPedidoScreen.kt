package com.htogo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme

private data class SavedAddress(
    val id: String,
    val label: String,
    val address: String,
    val icon: ImageVector,
    val isDefault: Boolean = false
)

private data class WaterBrand(
    val id: String,
    val name: String,
    val short: String,
    val pricePerUnit: Int,
    val color: Color
)

private val SAMPLE_ADDRESSES = listOf(
    SavedAddress("a1", "Casa",        "Av. Insurgentes Sur 1234, Int. 4B, Col. Del Valle", Icons.Filled.Home, true),
    SavedAddress("a2", "Oficina",     "Av. Universidad 567, Piso 8, Col. Narvarte",        Icons.Filled.Work),
    SavedAddress("a3", "Casa de mamá","Calle Heriberto Frías 890, Col. Nápoles",           Icons.Filled.Favorite)
)

private val SAMPLE_BRANDS = listOf(
    WaterBrand("b1", "Ciel",      "CIE", 35, Color(0xFF0077B6)),
    WaterBrand("b2", "Bonafont",  "BNF", 38, Color(0xFF1E40AF)),
    WaterBrand("b3", "Epura",     "EPU", 36, Color(0xFF0EA5E9)),
    WaterBrand("b4", "Santorini", "SAN", 30, Color(0xFF475569))
)

private const val PURIFICADORA_NAME = "Aguas Del Valle"

//private enum class TipoPedido { DIRECTA, ABIERTO }

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
private fun PrimaryCtaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary)
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        if (trailingIcon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(trailingIcon, null, tint = Color.White)
        }
    }
}

@Composable
private fun BrandRow(
    brands: List<WaterBrand>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(brands.size) { i ->
            val b = brands[i]
            val active = b.id == selectedId
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (active) HToGoColors.PrimarySoft else HToGoColors.Surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp, if (active) HToGoColors.Primary else HToGoColors.OutlineSoft
                ),
                modifier = Modifier.clickable { onSelect(b.id) }
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(b.color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(b.short, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(b.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = HToGoColors.TextPrimary)
                        Text("$${b.pricePerUnit} / 20 L",
                            fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (active) HToGoColors.Primary else HToGoColors.TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuantityCard(brand: WaterBrand, qty: Int, onQty: (Int) -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = HToGoColors.Surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(HToGoColors.PrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.WaterDrop, null, tint = HToGoColors.Primary,
                    modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("${brand.name} · Garrafón 20 L", fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold, color = HToGoColors.TextPrimary)
                Text("$${brand.pricePerUnit} c/u", fontSize = 13.sp, color = HToGoColors.TextSecondary)
            }
            Row(
                Modifier.background(HToGoColors.Background, RoundedCornerShape(99.dp)).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepperBtn("−", enabled = qty > 1) { onQty(qty - 1) }
                Text(
                    qty.toString(),
                    Modifier.widthIn(min = 28.dp).padding(horizontal = 4.dp),
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HToGoColors.TextPrimary
                )
                StepperBtn("+", enabled = qty < 30) { onQty(qty + 1) }
            }
        }
    }
}

@Composable
private fun StepperBtn(label: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (enabled) Color.White else Color.Transparent,
        shadowElevation = if (enabled) 1.dp else 0.dp,
        modifier = Modifier.size(36.dp).clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = if (enabled) HToGoColors.Primary else HToGoColors.TextTertiary)
        }
    }
}

@Composable
private fun PresetRow(qty: Int, onPick: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(1, 3, 5, 10).forEach { n ->
            val active = n == qty
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (active) HToGoColors.PrimarySoft else HToGoColors.Surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp, if (active) HToGoColors.Primary else HToGoColors.OutlineSoft
                ),
                modifier = Modifier.weight(1f).height(40.dp).clickable { onPick(n) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        n.toString(),
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = if (active) HToGoColors.Primary else HToGoColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun AddressItem(
    addr: SavedAddress,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = HToGoColors.Surface,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp, if (selected) HToGoColors.Primary else Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(HToGoColors.PrimarySoft),
                contentAlignment = Alignment.Center
            ) { Icon(addr.icon, null, tint = HToGoColors.Primary, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(addr.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = HToGoColors.TextPrimary)
                    if (addr.isDefault) {
                        Spacer(Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(6.dp), color = HToGoColors.PrimarySoft) {
                            Text(
                                "Predeterminado",
                                Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = HToGoColors.Primary
                            )
                        }
                    }
                }
                Text(addr.address, fontSize = 12.sp, color = HToGoColors.TextSecondary, maxLines = 1)
            }
            Box(
                Modifier.size(20.dp).clip(CircleShape)
                    .border(2.dp, if (selected) HToGoColors.Primary else HToGoColors.OutlineSoft, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(HToGoColors.Primary))
            }
        }
    }
}

@Composable
private fun AddAddressButton(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, HToGoColors.OutlineSoft),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AddLocationAlt, null, tint = HToGoColors.Primary)
            Spacer(Modifier.width(10.dp))
            Text("Agregar nueva ubicación",
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = HToGoColors.Primary)
        }
    }
}

@Composable
private fun MapConfirmCard(addr: SavedAddress, onAdjust: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = HToGoColors.Surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                Modifier.fillMaxWidth().height(170.dp)
                    .background(Brush.linearGradient(listOf(Color(0xFFDCEEF7), Color(0xFFE8F5FA))))
            ) {
                Box(Modifier.align(Alignment.Center)) {
                    Icon(
                        Icons.Filled.LocationOn, null,
                        tint = HToGoColors.Primary,
                        modifier = Modifier.size(42.dp)
                    )
                }
                Column(
                    Modifier.align(Alignment.BottomEnd).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MapFab(Icons.Filled.MyLocation)
                    MapFab(Icons.Filled.Add)
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Place, null, tint = HToGoColors.Primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(addr.address.substringBefore(","),
                        fontSize = 13.sp, color = HToGoColors.TextPrimary)
                    Text(addr.address.substringAfter(", "),
                        fontSize = 11.sp, color = HToGoColors.TextSecondary)
                }
                TextButton(onClick = onAdjust) {
                    Text("Ajustar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = HToGoColors.Primary)
                }
            }
        }
    }
}

@Composable
private fun MapFab(icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = HToGoColors.TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun NotesField(value: String, onChange: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = HToGoColors.Surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
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
private fun ResumenCard(qty: Int, unit: Int = 35, envio: Int = 15) {
    val sub = qty * unit
    val total = sub + envio
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = HToGoColors.Surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            ResumenRow("$qty × Garrafón 20 L", "$$sub.00")
            ResumenRow("Envío", "$$envio.00")
            Divider(Modifier.padding(vertical = 8.dp), color = HToGoColors.OutlineSoft)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HToGoColors.TextPrimary)
                Text("$$total.00", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HToGoColors.Primary)
            }
        }
    }
}

@Composable
private fun ResumenRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = HToGoColors.TextSecondary)
        Text(value, fontSize = 13.sp, color = HToGoColors.TextSecondary)
    }
}

@Composable
private fun NuevoPedidoTopBar(onBack: () -> Unit) {
    Surface(color = HToGoColors.PrimaryDark) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.10f),
                modifier = Modifier.size(40.dp).clickable(onClick = onBack)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text("Nuevo pedido",
                Modifier.weight(1f),
                fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Surface(shape = RoundedCornerShape(99.dp), color = Color.White.copy(alpha = 0.10f)) {
                Text("Paso 1 de 2",
                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

@Composable
private fun CtaBar(total: Int, onConfirm: () -> Unit) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp).padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("TOTAL", fontSize = 11.sp, color = HToGoColors.TextSecondary, letterSpacing = 0.4.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$$total", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        color = HToGoColors.TextPrimary)
                    Text(" MXN", fontSize = 13.sp, color = HToGoColors.TextSecondary)
                }
            }
            Spacer(Modifier.width(12.dp))
            PrimaryCtaButton(
                text = "Confirmar pedido",
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward
            )
        }
    }
}

@Composable
fun NuevoPedidoScreen(
    onBack: () -> Unit = {},
    onConfirm: () -> Unit = {}
) {
    var qty by remember { mutableStateOf(3) }
    var selectedAddrId by remember { mutableStateOf("a1") }
    var selectedBrandId by remember { mutableStateOf("b1") }
    var notes by remember { mutableStateOf("Tocar timbre 4B, dejar en recepción si no contesto") }

    val brand = SAMPLE_BRANDS.first { it.id == selectedBrandId }
    val envio = 15
    val unitPrice = brand.pricePerUnit
    val total = qty * unitPrice + envio
    val selectedAddr = SAMPLE_ADDRESSES.first { it.id == selectedAddrId }

    Scaffold(
        topBar = { NuevoPedidoTopBar(onBack) },
        bottomBar = { CtaBar(total = total, onConfirm = onConfirm) },
        containerColor = HToGoColors.Background
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(18.dp))
            SectionTitle("Marca de garrafón")
            Spacer(Modifier.height(10.dp))
            BrandRow(
                brands = SAMPLE_BRANDS,
                selectedId = selectedBrandId,
                onSelect = { selectedBrandId = it }
            )
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Storefront, null, tint = HToGoColors.TextTertiary,
                    modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "Marcas que vende $PURIFICADORA_NAME",
                    fontSize = 11.sp, color = HToGoColors.TextTertiary
                )
            }

            Spacer(Modifier.height(8.dp))
            SectionTitle("¿Cuántos garrafones?")
            Spacer(Modifier.height(10.dp))
            Column(Modifier.padding(horizontal = 16.dp)) {
                QuantityCard(brand, qty) { qty = it }
                Spacer(Modifier.height(12.dp))
                PresetRow(qty) { qty = it }
            }

            Spacer(Modifier.height(18.dp))
            SectionTitle("Domicilio de entrega")
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SAMPLE_ADDRESSES.forEach { addr ->
                    AddressItem(addr, selected = addr.id == selectedAddrId) { selectedAddrId = addr.id }
                }
                AddAddressButton {}
            }

            Spacer(Modifier.height(18.dp))
            SectionTitle("Confirmar en el mapa")
            Spacer(Modifier.height(10.dp))
            Box(Modifier.padding(horizontal = 16.dp)) {
                MapConfirmCard(selectedAddr) {}
            }

            Spacer(Modifier.height(18.dp))
            SectionTitle("Indicaciones para el repartidor")
            Spacer(Modifier.height(10.dp))
            Box(Modifier.padding(horizontal = 16.dp)) {
                NotesField(notes) { notes = it }
            }

            Spacer(Modifier.height(18.dp))
            SectionTitle("Resumen")
            Spacer(Modifier.height(10.dp))
            Box(Modifier.padding(horizontal = 16.dp)) {
                ResumenCard(qty = qty, unit = unitPrice, envio = envio)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 900, name = "06 · Nuevo pedido")
@Composable
fun NuevoPedidoScreenPreview() {
    HToGoTheme { NuevoPedidoScreen() }
}
