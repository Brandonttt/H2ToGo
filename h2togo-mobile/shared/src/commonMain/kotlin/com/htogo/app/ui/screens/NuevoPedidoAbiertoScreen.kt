package com.htogo.app.ui.screens

import com.htogo.app.ui.components.BarraEstadoClara
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.htogo.app.data.dto.DetallePedidoRequest
import com.htogo.app.data.dto.PedidoCreateRequest
import com.htogo.app.ui.ClienteViewModel
import com.htogo.app.ui.components.AgregarDireccionDialog
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


private const val PRECIO_MIN = 20
private const val PRECIO_MAX = 120

@Composable
fun NuevoPedidoAbiertoScreen(
    onBack: () -> Unit = {},
    onConfirm: () -> Unit = {},
    clienteViewModel: ClienteViewModel = viewModel { ClienteViewModel() }
) {
    val direccionesDisponibles by clienteViewModel.direcciones.collectAsState()
    val marcasDisponibles by clienteViewModel.marcas.collectAsState()
    val isLoading by clienteViewModel.isLoading.collectAsState()
    val errorMsg by clienteViewModel.errorMessage.collectAsState()

    var mostrarDialogDireccion by remember { mutableStateOf(false) }

    val addresses = remember(direccionesDisponibles) {
        direccionesDisponibles.mapIndexed { idx, d ->
            val icon = when {
                d.alias.contains("Casa", ignoreCase = true) -> Icons.Filled.Home
                d.alias.contains("Oficina", ignoreCase = true) || d.alias.contains("Trabajo", ignoreCase = true) -> Icons.Filled.Work
                else -> Icons.Filled.LocationOn
            }
            AbiertoAddr(
                id = d.id.toString(),
                label = d.alias,
                address = "${d.calle} ${d.numeroExterior}${if (!d.numeroInterior.isNullOrBlank()) ", Int. " + d.numeroInterior else ""}, Col. ${d.colonia}",
                icon = icon,
                isDefault = idx == 0
            )
        }
    }

    var qty by remember { mutableStateOf(1) }
    var precioMax by remember { mutableStateOf(45) }
    val focusManager = LocalFocusManager.current
    var selectedAddrId by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var marcaSelId by remember { mutableStateOf<Int?>(null) }
    // RN-031: si el cliente no entrega envase, paga además el envase al precio del negocio que acepte.
    var tieneEnvase by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        clienteViewModel.clearError() // al abrir, sin errores de un intento anterior
        if (marcasDisponibles.isEmpty()) clienteViewModel.cargarMarcas()
    }
    LaunchedEffect(marcasDisponibles) {
        if (marcaSelId == null || marcasDisponibles.none { it.id == marcaSelId }) {
            marcaSelId = marcasDisponibles.firstOrNull()?.id
        }
    }

    LaunchedEffect(addresses) {
        if (addresses.isNotEmpty() && (selectedAddrId.isEmpty() || addresses.none { it.id == selectedAddrId })) {
            selectedAddrId = addresses.first().id
        }
    }

    val selectedAddr = addresses.firstOrNull { it.id == selectedAddrId }
    // El envío no se cobra aparte: el tope es solo el agua (más envases si no los entrega).
    val totalMax = qty * precioMax
    val canConfirm = selectedAddr != null && marcaSelId != null && qty > 0 && precioMax >= PRECIO_MIN && !isLoading

    fun ejecutarPedidoAbierto() {
        if (selectedAddr == null) {
            mostrarDialogDireccion = true
            return
        }
        val addrIdInt = selectedAddr.id.toIntOrNull() ?: return
        val marcaIdInt = marcaSelId ?: return

        val request = PedidoCreateRequest(
            tipoSolicitud = "abierta",
            precioMaximoGarrafon = precioMax.toDouble(),
            idDireccionEntrega = addrIdInt,
            indicaciones = notes.ifBlank { null },
            detalles = listOf(
                DetallePedidoRequest(
                    idMarca = marcaIdInt,
                    cantidad = qty,
                    tieneEnvase = tieneEnvase
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

    BarraEstadoClara() // el encabezado oscuro llega hasta arriba
    Scaffold(
        topBar = { TopBar(onBack) },
        bottomBar = {
            if (isLoading) {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = HToGoColors.Primary)
                    }
                }
            } else {
                CtaBar(
                    totalMax = totalMax,
                    enabled = canConfirm,
                    onConfirm = { ejecutarPedidoAbierto() }
                )
            }
        },
        containerColor = HToGoColors.Background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                // Tocar fuera de un campo cierra el teclado (el teclado numérico de iOS no trae "Listo").
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
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

            HeroExplicacion()

            Spacer(Modifier.height(20.dp))
            SectionTitle("Tu precio máximo por garrafón")
            Spacer(Modifier.height(10.dp))
            Box(Modifier.padding(horizontal = 16.dp)) {
                PrecioMaximoCard(
                    precio = precioMax,
                    onPrecio = { precioMax = it.coerceIn(PRECIO_MIN, PRECIO_MAX) }
                )
            }
            HintRow(precio = precioMax)

            Spacer(Modifier.height(20.dp))
            SectionTitle("Marca")
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (marcasDisponibles.isEmpty()) {
                    Text("Cargando marcas…", fontSize = 13.sp, color = HToGoColors.TextSecondary)
                }
                marcasDisponibles.forEach { m ->
                    val sel = m.id == marcaSelId
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = if (sel) HToGoColors.Primary else Color.White,
                        border = BorderStroke(1.dp, if (sel) HToGoColors.Primary else HToGoColors.OutlineSoft),
                        modifier = Modifier.clickable { marcaSelId = m.id }
                    ) {
                        Text(
                            m.nombre,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (sel) Color.White else HToGoColors.TextPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            SectionTitle("¿Cuántos garrafones?")
            Spacer(Modifier.height(10.dp))
            Box(Modifier.padding(horizontal = 16.dp)) {
                CantidadCard(qty = qty, onQty = { qty = it.coerceIn(1, 30) })
            }

            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, HToGoColors.OutlineSoft),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Tengo envases para intercambiar", fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold, color = HToGoColors.TextPrimary)
                        Text(
                            if (tieneEnvase) "Pagas solo el agua"
                            else "Pagarás también el envase, al precio de la purificadora que acepte",
                            fontSize = 11.sp, color = HToGoColors.TextSecondary
                        )
                    }
                    Switch(checked = tieneEnvase, onCheckedChange = { tieneEnvase = it })
                }
            }

            Spacer(Modifier.height(20.dp))
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
                    addresses.forEach { a ->
                        AddrCard(addr = a, selected = a.id == selectedAddrId) { selectedAddrId = a.id }
                    }
                }
                AgregarDireccionBtn { mostrarDialogDireccion = true }
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
                ResumenAbiertoCard(qty = qty, precioMax = precioMax, tieneEnvase = tieneEnvase)
            }
            Spacer(Modifier.height(20.dp))
        }

        if (mostrarDialogDireccion) {
            AgregarDireccionDialog(
                onDismiss = { mostrarDialogDireccion = false },
                onDireccionCreada = { nueva ->
                    mostrarDialogDireccion = false
                    selectedAddrId = nueva.id.toString()
                },
                clienteViewModel = clienteViewModel
            )
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Surface(color = HToGoColors.PrimaryDark) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 10.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
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
            }
        }
    }
}

@Composable
private fun HeroExplicacion() {
    Box(
        Modifier
            .fillMaxWidth()
            .background(HToGoColors.PrimaryDark)
            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
    ) {
        Text(
            "Te asignamos la primera purificadora que acepte tu precio.",
            fontSize = 13.sp,
            color = HToGoColors.PrimarySoft,
            lineHeight = 18.sp
        )
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
                // Se puede escribir el precio (al tocarlo) o ajustarlo con − / + de $5 en $5.
                var escribiendo by remember { mutableStateOf(false) }
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { escribiendo = true }
                ) {
                    Text("$$precio", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = HToGoColors.Primary)
                    Text(" MXN", fontSize = 13.sp, color = HToGoColors.TextSecondary,
                        modifier = Modifier.padding(bottom = 6.dp))
                    Icon(Icons.Filled.Edit, "Escribir precio", tint = HToGoColors.TextTertiary,
                        modifier = Modifier.padding(start = 6.dp, bottom = 9.dp).size(16.dp))
                }
                if (escribiendo) {
                    EscribirPrecioDialog(
                        actual = precio,
                        onGuardar = { onPrecio(it); escribiendo = false },
                        onCancelar = { escribiendo = false }
                    )
                }
            }
            Row(
                Modifier
                    .background(HToGoColors.Background, RoundedCornerShape(99.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pasos de $5, alineados a múltiplos de 5 (p. ej. 47 → 45 o 50).
                val fm = LocalFocusManager.current
                StepBtn(label = "−", enabled = precio > PRECIO_MIN) {
                    fm.clearFocus()
                    onPrecio((((precio - 1) / 5) * 5).coerceIn(PRECIO_MIN, PRECIO_MAX))
                }
                Spacer(Modifier.width(8.dp))
                StepBtn(label = "+", enabled = precio < PRECIO_MAX) {
                    fm.clearFocus()
                    onPrecio((((precio / 5) + 1) * 5).coerceIn(PRECIO_MIN, PRECIO_MAX))
                }
            }
        }
    }
}

@Composable
private fun EscribirPrecioDialog(actual: Int, onGuardar: (Int) -> Unit, onCancelar: () -> Unit) {
    var texto by remember { mutableStateOf("") }
    val valor = texto.toIntOrNull()
    val valido = valor != null && valor in PRECIO_MIN..PRECIO_MAX
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { foco.requestFocus() }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Precio máximo por garrafón") },
        text = {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it.filter(Char::isDigit).take(3) },
                singleLine = true,
                prefix = { Text("$") },
                placeholder = { Text(actual.toString()) },
                supportingText = {
                    Text(
                        "Entre $$PRECIO_MIN y $$PRECIO_MAX",
                        color = if (texto.isNotEmpty() && !valido) HToGoColors.AccentRose else HToGoColors.TextSecondary
                    )
                },
                isError = texto.isNotEmpty() && !valido,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (valido) onGuardar(valor!!) }),
                modifier = Modifier.focusRequester(foco)
            )
        },
        confirmButton = {
            TextButton(onClick = { onGuardar(valor!!) }, enabled = valido) { Text("Listo") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
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
private fun HintRow(precio: Int) {
    // Solo avisa cuando el precio es bajo; con un precio normal no hace falta decir nada.
    val (texto, color) = when {
        precio < 35 -> "Pocas purificadoras aceptarán este precio" to HToGoColors.AccentRose
        precio in 35..40 -> "Puede tardar unos minutos en aceptarse" to Color(0xFFB45309)
        else -> return
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp),
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
private fun ResumenAbiertoCard(qty: Int, precioMax: Int, tieneEnvase: Boolean) {
    val sub = qty * precioMax
    val total = sub
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, HToGoColors.OutlineSoft),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            ResumenRow("$qty × Garrafón 20 L", "$$sub o menos")
            ResumenRow("Envío", "Sin costo")
            if (!tieneEnvase) ResumenRow("$qty × Envase nuevo", "Precio del negocio")
            HorizontalDivider(
                Modifier.padding(vertical = 8.dp),
                color = HToGoColors.OutlineSoft
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Pagas",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HToGoColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "$$total o menos",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = HToGoColors.Primary
                )
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
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .padding(bottom = 4.dp),
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
