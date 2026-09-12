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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.htogo.app.data.api.GeocodingHelper
import com.htogo.app.data.dto.DetallePedidoRequest
import com.htogo.app.data.dto.PedidoCreateRequest
import com.htogo.app.ui.ClienteViewModel
import com.htogo.app.ui.components.AgregarDireccionDialog
import com.htogo.app.ui.components.OsmMapView
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme
import kotlinx.coroutines.launch

private data class SavedAddress(
    val id: String,
    val label: String,
    val address: String,
    val icon: ImageVector,
    val isDefault: Boolean = false,
    val lat: Double = 19.376692,
    val lon: Double = -99.165057
)

private data class WaterBrand(
    val id: String,
    val name: String,
    val short: String,
    val pricePerUnit: Int,
    val color: Color
)

private val SAMPLE_ADDRESSES = listOf(
    SavedAddress("a1", "Casa",        "Av. Insurgentes Sur 1234, Int. 4B, Col. Del Valle", Icons.Filled.Home, isDefault = true),
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
    enabled: Boolean = true,
    trailingIcon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = HToGoColors.Primary,
            disabledContainerColor = HToGoColors.OutlineSoft
        )
    ) {
        Text(
            text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) Color.White else HToGoColors.TextTertiary
        )
        if (trailingIcon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(
                trailingIcon,
                null,
                tint = if (enabled) Color.White else HToGoColors.TextTertiary
            )
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
private fun MapConfirmCard(
    addr: SavedAddress?,
    onAdjust: (Double, Double, String?) -> Unit,
    onLocationSelected: (Double, Double, String?) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var currentLat by remember(addr?.lat) { mutableStateOf(addr?.lat ?: 19.376692) }
    var currentLon by remember(addr?.lon) { mutableStateOf(addr?.lon ?: -99.165057) }
    var pinAddressText by remember(addr?.address) { mutableStateOf(addr?.address ?: "Benito Juárez, CDMX") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = HToGoColors.Surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Buscador de dirección sobre el mapa
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar calle o dirección...", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, null, tint = HToGoColors.Primary, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = HToGoColors.Primary)
                        } else if (searchQuery.isNotBlank()) {
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    isSearching = true
                                    val res = GeocodingHelper.buscarCoordenadas(searchQuery)
                                    if (res != null) {
                                        currentLat = res.lat
                                        currentLon = res.lon
                                        val formatted = "${res.road ?: searchQuery} ${res.houseNumber ?: ""}, Col. ${res.neighbourhood ?: ""}".trim().trim(',')
                                        pinAddressText = formatted
                                        onLocationSelected(res.lat, res.lon, formatted)
                                    }
                                    isSearching = false
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Buscar", tint = HToGoColors.Primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 12.sp, color = HToGoColors.TextPrimary),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HToGoColors.Primary,
                        unfocusedBorderColor = HToGoColors.OutlineSoft
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // Mapa interactivo OSM
            OsmMapView(
                latitude = currentLat,
                longitude = currentLon,
                zoom = 16,
                isInteractive = true,
                isDraggablePin = true,
                onLocationChange = { newLat, newLon ->
                    currentLat = newLat
                    currentLon = newLon
                    coroutineScope.launch {
                        val res = GeocodingHelper.obtenerDireccionDeCoordenadas(newLat, newLon)
                        if (res != null) {
                            val road = res.road ?: ""
                            val num = res.houseNumber ?: ""
                            val col = res.neighbourhood ?: ""
                            val formatted = "$road $num, Col. $col".trim().trim(',')
                            if (formatted.isNotBlank()) {
                                pinAddressText = formatted
                            }
                            onLocationSelected(newLat, newLon, pinAddressText)
                        } else {
                            onLocationSelected(newLat, newLon, null)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )

            // Info de la dirección seleccionada
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Place, null, tint = HToGoColors.Primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        pinAddressText.substringBefore(","),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HToGoColors.TextPrimary,
                        maxLines = 1
                    )
                    Text(
                        if (pinAddressText.contains(",")) pinAddressText.substringAfter(", ") else "Arrastra el pin para ajustar la ubicación exacta",
                        fontSize = 11.sp,
                        color = HToGoColors.TextSecondary,
                        maxLines = 1
                    )
                }
                TextButton(onClick = { onAdjust(currentLat, currentLon, pinAddressText) }) {
                    Text(
                        if (addr != null) "Ajustar" else "Guardar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HToGoColors.Primary
                    )
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
private fun CtaBar(
    total: Int,
    canConfirm: Boolean = true,
    onConfirm: () -> Unit
) {
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
                text = if (canConfirm) "Confirmar pedido" else "Agrega un domicilio",
                onClick = onConfirm,
                enabled = canConfirm,
                modifier = Modifier.weight(1f),
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward
            )
        }
    }
}

@Composable
fun NuevoPedidoScreen(
    onBack: () -> Unit = {},
    onConfirm: () -> Unit = {},
    clienteViewModel: ClienteViewModel = viewModel()
) {
    val marcasDisponibles by clienteViewModel.marcas.collectAsState()
    val direccionesDisponibles by clienteViewModel.direcciones.collectAsState()
    val isLoading by clienteViewModel.isLoading.collectAsState()
    val errorMsg by clienteViewModel.errorMessage.collectAsState()

    var mostrarDialogDireccion by remember { mutableStateOf(false) }
    var latParaDialog by remember { mutableStateOf<Double?>(null) }
    var lonParaDialog by remember { mutableStateOf<Double?>(null) }
    var calleParaDialog by remember { mutableStateOf<String?>(null) }

    val brands = remember(marcasDisponibles) {
        if (marcasDisponibles.isNotEmpty()) {
            val colors = listOf(
                Color(0xFF0077B6), Color(0xFF1E40AF), Color(0xFF0EA5E9), Color(0xFF475569)
            )
            marcasDisponibles.mapIndexed { idx, m ->
                WaterBrand(
                    id = m.id.toString(),
                    name = m.nombre,
                    short = m.nombre.take(3).uppercase(),
                    pricePerUnit = 35 + (idx * 2),
                    color = colors[idx % colors.size]
                )
            }
        } else {
            SAMPLE_BRANDS
        }
    }

    val addresses = remember(direccionesDisponibles) {
        direccionesDisponibles.mapIndexed { idx, d ->
            val icon = when {
                d.alias.contains("Casa", ignoreCase = true) -> Icons.Filled.Home
                d.alias.contains("Oficina", ignoreCase = true) || d.alias.contains("Trabajo", ignoreCase = true) -> Icons.Filled.Work
                else -> Icons.Filled.LocationOn
            }
            SavedAddress(
                id = d.id.toString(),
                label = d.alias,
                address = "${d.calle} ${d.numeroExterior}${if (!d.numeroInterior.isNullOrBlank()) ", Int. " + d.numeroInterior else ""}, Col. ${d.colonia}",
                icon = icon,
                lat = d.lat,
                lon = d.lon,
                isDefault = idx == 0
            )
        }
    }

    var qty by remember { mutableStateOf(3) }
    var selectedAddrId by remember { mutableStateOf("") }
    var selectedBrandId by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("Tocar timbre, dejar en recepción si no contesto") }

    LaunchedEffect(brands) {
        if (selectedBrandId.isEmpty() || brands.none { it.id == selectedBrandId }) {
            selectedBrandId = brands.firstOrNull()?.id ?: "b1"
        }
    }

    LaunchedEffect(addresses) {
        if (addresses.isNotEmpty() && (selectedAddrId.isEmpty() || addresses.none { it.id == selectedAddrId })) {
            selectedAddrId = addresses.first().id
        }
    }

    val brand = brands.firstOrNull { it.id == selectedBrandId } ?: brands.firstOrNull() ?: SAMPLE_BRANDS.first()
    val envio = 15
    val unitPrice = brand.pricePerUnit
    val total = qty * unitPrice + envio
    val selectedAddr = addresses.firstOrNull { it.id == selectedAddrId }

    val purificadoraNombre = clienteViewModel.purificadoraSeleccionadaNombre ?: PURIFICADORA_NAME
    val purificadoraId = clienteViewModel.purificadoraSeleccionadaId
    val canConfirm = selectedAddr != null && !isLoading

    fun ejecutarPedido() {
        if (selectedAddr == null) {
            mostrarDialogDireccion = true
            return
        }
        val addrIdInt = selectedAddr.id.toIntOrNull() ?: return
        val brandIdInt = brand.id.toIntOrNull() ?: 1

        val request = PedidoCreateRequest(
            tipoSolicitud = if (purificadoraId != null) "directa" else "directa",
            idNegocio = purificadoraId ?: 1,
            idDireccionEntrega = addrIdInt,
            indicaciones = notes.ifBlank { null },
            detalles = listOf(
                DetallePedidoRequest(
                    idMarca = brandIdInt,
                    cantidad = qty,
                    tieneEnvase = true
                )
            )
        )

        clienteViewModel.crearPedido(
            request = request,
            onSuccess = {
                onConfirm()
            },
            onError = { }
        )
    }

    Scaffold(
        topBar = { NuevoPedidoTopBar(onBack) },
        bottomBar = {
            if (isLoading) {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        Modifier.fillMaxWidth().padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = HToGoColors.Primary)
                    }
                }
            } else {
                CtaBar(total = total, canConfirm = canConfirm, onConfirm = { ejecutarPedido() })
            }
        },
        containerColor = HToGoColors.Background
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            if (errorMsg != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            errorMsg!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionTitle("Marca de garrafón")
            Spacer(Modifier.height(10.dp))
            BrandRow(
                brands = brands,
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
                    "Marcas que vende $purificadoraNombre",
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
                if (addresses.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = HToGoColors.PrimarySoft,
                        border = androidx.compose.foundation.BorderStroke(1.dp, HToGoColors.Primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.LocationOff, null, tint = HToGoColors.Primary, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Sin domicilios registrados",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HToGoColors.TextPrimary
                                )
                                Text(
                                    "Agrega una dirección para continuar con tu pedido.",
                                    fontSize = 11.sp,
                                    color = HToGoColors.TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    addresses.forEach { addr ->
                        AddressItem(addr, selected = addr.id == selectedAddrId) { selectedAddrId = addr.id }
                    }
                }
                AddAddressButton { mostrarDialogDireccion = true }
            }

            Spacer(Modifier.height(18.dp))
            SectionTitle("Confirmar en el mapa")
            Spacer(Modifier.height(10.dp))
            Box(Modifier.padding(horizontal = 16.dp)) {
                MapConfirmCard(
                    addr = selectedAddr,
                    onAdjust = { lat, lon, detectedAddr ->
                        latParaDialog = lat
                        lonParaDialog = lon
                        calleParaDialog = detectedAddr?.substringBefore(",")?.takeIf { it.isNotBlank() }
                        mostrarDialogDireccion = true
                    },
                    onLocationSelected = { lat, lon, detectedAddr ->
                        latParaDialog = lat
                        lonParaDialog = lon
                        if (detectedAddr != null) {
                            calleParaDialog = detectedAddr.substringBefore(",").takeIf { it.isNotBlank() }
                        }
                    }
                )
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

        if (mostrarDialogDireccion) {
            AgregarDireccionDialog(
                onDismiss = {
                    mostrarDialogDireccion = false
                    latParaDialog = null
                    lonParaDialog = null
                    calleParaDialog = null
                },
                onDireccionCreada = { nueva ->
                    mostrarDialogDireccion = false
                    latParaDialog = null
                    lonParaDialog = null
                    calleParaDialog = null
                    selectedAddrId = nueva.id.toString()
                },
                clienteViewModel = clienteViewModel,
                initialLat = latParaDialog,
                initialLon = lonParaDialog,
                initialCalle = calleParaDialog
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 900, name = "06 · Nuevo pedido")
@Composable
fun NuevoPedidoScreenPreview() {
    HToGoTheme { NuevoPedidoScreen() }
}
