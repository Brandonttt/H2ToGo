package com.htogo.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme
import kotlinx.coroutines.delay

private data class ProductoCatalogo(
    val id: String,
    val codigo: String,
    val marca: String,
    val capacidad: String,
    val precio: Int,
    val activo: Boolean,
    val proveedor: String,
    val accent: Color
)

private data class SolicitudPendiente(
    val id: String,
    val tipo: String,
    val descripcion: String,
    val fecha: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductosPreciosScreen(
    onBack: () -> Unit = {}
) {
    var productos by remember {
        mutableStateOf(
            listOf(
                ProductoCatalogo("p1", "CIE", "Ciel", "20 L", 45, true, "Distribuidor MX", HToGoColors.Primary),
                ProductoCatalogo("p2", "BON", "Bonafont", "20 L", 48, true, "Bonafont SA", HToGoColors.AccentEmerald),
                ProductoCatalogo("p3", "EPU", "Epura", "20 L", 42, true, "Epura Distrib.", HToGoColors.AccentAmber)
            )
        )
    }
    var pendientes by remember {
        mutableStateOf(
            listOf(
                SolicitudPendiente(
                    "SOL-0119",
                    "Agregar producto",
                    "Santorini · 20 L · $40 — Proveedor Aqua MX",
                    "Hace 2 días"
                )
            )
        )
    }

    var editandoPrecio by remember { mutableStateOf<ProductoCatalogo?>(null) }
    var mostrarAgregar by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<ToastInfo?>(null) }

    LaunchedEffect(toast) {
        toast?.let {
            delay(3500)
            toast = null
        }
    }

    Scaffold(
        containerColor = HToGoColors.Background,
        topBar = {
            Column(
                Modifier.background(
                    Brush.linearGradient(listOf(HToGoColors.PrimaryDark, HToGoColors.Primary))
                )
            ) {
                TopAppBar(
                    title = {
                        Text(
                            "Productos y precios",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Storefront, null,
                        tint = HToGoColors.PrimarySoft,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Aguas Del Valle · CU-23",
                        color = HToGoColors.PrimarySoft, fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ReglasCard() }
            item { SectionTitle("Catálogo activo · ${productos.size} productos") }
            items(productos, key = { it.id }) { p ->
                ProductoCard(p) { editandoPrecio = p }
            }
            item {
                AgregarProductoBtn { mostrarAgregar = true }
            }
            if (pendientes.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    SectionTitle("Esperando aprobación · ${pendientes.size}")
                }
                items(pendientes, key = { it.id }) { s ->
                    PendienteCard(s) { id ->
                        pendientes = pendientes.filterNot { it.id == id }
                        toast = ToastInfo("Solicitud cancelada", success = false)
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }

        toast?.let {
            ToastSnackbar(it)
        }
    }

    editandoPrecio?.let { producto ->
        EditarPrecioDialog(
            producto = producto,
            onDismiss = { editandoPrecio = null },
            onConfirmar = { nuevoPrecio ->
                productos = productos.map {
                    if (it.id == producto.id) it.copy(precio = nuevoPrecio) else it
                }
                editandoPrecio = null
                toast = ToastInfo(
                    "Precio actualizado · aplicado al instante",
                    success = true
                )
            }
        )
    }

    if (mostrarAgregar) {
        AgregarProductoDialog(
            onDismiss = { mostrarAgregar = false },
            onEnviar = { marca, capacidad, precio, proveedor ->
                pendientes = pendientes + SolicitudPendiente(
                    id = "SOL-${(120..999).random()}",
                    tipo = "Agregar producto",
                    descripcion = "$marca · $capacidad · $$precio — $proveedor",
                    fecha = "Hace un momento"
                )
                mostrarAgregar = false
                toast = ToastInfo(
                    "Solicitud enviada al admin · pendiente de aprobación",
                    success = false
                )
            }
        )
    }
}

@Composable
private fun ReglasCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HToGoColors.OutlineSoft),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(HToGoColors.PrimarySoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Info, null, tint = HToGoColors.Primary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "Cómo funciona",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HToGoColors.TextPrimary
                )
            }
            Spacer(Modifier.height(10.dp))
            ReglaFila(
                icon = Icons.Filled.Bolt,
                color = HToGoColors.AccentEmerald,
                titulo = "Cambiar precio",
                detalle = "Inmediato — se actualiza para los clientes en cuanto confirmas."
            )
            Spacer(Modifier.height(8.dp))
            ReglaFila(
                icon = Icons.Filled.HourglassTop,
                color = HToGoColors.AccentAmber,
                titulo = "Agregar nuevo producto",
                detalle = "Requiere aprobación del admin (RN-020). Verás la solicitud abajo hasta que se apruebe."
            )
        }
    }
}

@Composable
private fun ReglaFila(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, titulo: String, detalle: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = .14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                titulo,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = HToGoColors.TextPrimary
            )
            Text(
                detalle,
                fontSize = 11.sp,
                color = HToGoColors.TextSecondary,
                lineHeight = 14.sp
            )
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
        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
    )
}

@Composable
private fun ProductoCard(p: ProductoCatalogo, onEditar: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HToGoColors.OutlineSoft),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(p.accent.copy(alpha = .14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(p.codigo, color = p.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "${p.marca} · ${p.capacidad}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HToGoColors.TextPrimary
                    )
                    Text(
                        p.proveedor,
                        fontSize = 11.sp,
                        color = HToGoColors.TextSecondary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$${p.precio}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = HToGoColors.Primary
                    )
                    Text(
                        "/ pieza",
                        fontSize = 10.sp,
                        color = HToGoColors.TextTertiary
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = HToGoColors.AccentEmerald.copy(alpha = .14f)
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle, null,
                            tint = HToGoColors.AccentEmerald,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Activo",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HToGoColors.AccentEmerald
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = HToGoColors.PrimarySoft,
                    modifier = Modifier.clickable(onClick = onEditar)
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Edit, null,
                            tint = HToGoColors.Primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Cambiar precio",
                            color = HToGoColors.Primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgregarProductoBtn(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        border = BorderStroke(1.5.dp, HToGoColors.Primary),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(vertical = 14.dp, horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(HToGoColors.PrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, null, tint = HToGoColors.Primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Agregar producto al catálogo",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HToGoColors.Primary
                )
                Text(
                    "Sujeto a aprobación del admin",
                    fontSize = 11.sp,
                    color = HToGoColors.TextSecondary
                )
            }
            Icon(
                Icons.Filled.HourglassTop, null,
                tint = HToGoColors.AccentAmber,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun PendienteCard(s: SolicitudPendiente, onCancelar: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = HToGoColors.AccentAmber.copy(alpha = .08f),
        border = BorderStroke(1.dp, HToGoColors.AccentAmber.copy(alpha = .4f)),
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
                    .background(HToGoColors.AccentAmber.copy(alpha = .18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PendingActions, null,
                    tint = HToGoColors.AccentAmber,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        s.tipo,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HToGoColors.TextPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "#${s.id}",
                        fontSize = 10.sp,
                        color = HToGoColors.TextTertiary
                    )
                }
                Text(
                    s.descripcion,
                    fontSize = 12.sp,
                    color = HToGoColors.TextSecondary,
                    lineHeight = 15.sp
                )
                Text(
                    "${s.fecha} · esperando admin",
                    fontSize = 10.sp,
                    color = HToGoColors.AccentAmber,
                    fontWeight = FontWeight.SemiBold
                )
            }
            TextButton(onClick = { onCancelar(s.id) }) {
                Text(
                    "Cancelar",
                    fontSize = 12.sp,
                    color = HToGoColors.AccentRose,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun EditarPrecioDialog(
    producto: ProductoCatalogo,
    onDismiss: () -> Unit,
    onConfirmar: (Int) -> Unit
) {
    var precioStr by remember { mutableStateOf(producto.precio.toString()) }
    val precioInt = precioStr.toIntOrNull()
    val valido = precioInt != null && precioInt in 20..120
    val cambia = precioInt != null && precioInt != producto.precio

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "Cambiar precio",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Se aplicará de inmediato a todos los clientes.",
                    fontSize = 13.sp,
                    color = HToGoColors.TextSecondary
                )
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = HToGoColors.Background,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(producto.accent.copy(alpha = .14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                producto.codigo,
                                color = producto.accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${producto.marca} · ${producto.capacidad}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Precio actual: $${producto.precio}",
                                fontSize = 11.sp,
                                color = HToGoColors.TextSecondary
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = precioStr,
                    onValueChange = { raw -> precioStr = raw.filter(Char::isDigit).take(3) },
                    label = { Text("Nuevo precio (MXN)") },
                    leadingIcon = { Text("$", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !valido && precioStr.isNotEmpty(),
                    supportingText = {
                        Text(
                            if (!valido && precioStr.isNotEmpty()) "Rango permitido: $20 – $120"
                            else "El precio anterior queda en el historial"
                        )
                    }
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = HToGoColors.AccentEmerald.copy(alpha = .12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Bolt, null,
                            tint = HToGoColors.AccentEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Cambio inmediato — sin necesidad de aprobación.",
                            fontSize = 11.sp,
                            color = HToGoColors.TextPrimary
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(23.dp)
                    ) { Text("Cancelar") }
                    Button(
                        onClick = { precioInt?.let(onConfirmar) },
                        enabled = valido && cambia,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(23.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary)
                    ) { Text("Aplicar precio", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
private fun AgregarProductoDialog(
    onDismiss: () -> Unit,
    onEnviar: (marca: String, capacidad: String, precio: Int, proveedor: String) -> Unit
) {
    var marca by remember { mutableStateOf("") }
    var capacidad by remember { mutableStateOf("20 L") }
    var precioStr by remember { mutableStateOf("") }
    var proveedor by remember { mutableStateOf("") }
    val precioInt = precioStr.toIntOrNull()
    val valido = marca.isNotBlank() && capacidad.isNotBlank() &&
        precioInt != null && precioInt in 20..120 && proveedor.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Agregar nuevo producto",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "El admin debe aprobar antes de que aparezca en tu catálogo.",
                    fontSize = 13.sp,
                    color = HToGoColors.TextSecondary
                )
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = HToGoColors.AccentAmber.copy(alpha = .12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.HourglassTop, null,
                            tint = HToGoColors.AccentAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Tiempo promedio de aprobación: 4 h",
                            fontSize = 11.sp,
                            color = HToGoColors.TextPrimary
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = marca,
                    onValueChange = { marca = it.take(40) },
                    label = { Text("Marca del producto") },
                    leadingIcon = { Icon(Icons.Filled.WaterDrop, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = capacidad,
                        onValueChange = { capacidad = it.take(10) },
                        label = { Text("Capacidad") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = precioStr,
                        onValueChange = { raw -> precioStr = raw.filter(Char::isDigit).take(3) },
                        label = { Text("Precio") },
                        leadingIcon = { Text("$", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = proveedor,
                    onValueChange = { proveedor = it.take(50) },
                    label = { Text("Proveedor / distribuidor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(23.dp)
                    ) { Text("Cancelar") }
                    Button(
                        onClick = {
                            if (precioInt != null) {
                                onEnviar(marca.trim(), capacidad.trim(), precioInt, proveedor.trim())
                            }
                        },
                        enabled = valido,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(23.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.AccentAmber)
                    ) { Text("Enviar a aprobación", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

private data class ToastInfo(val mensaje: String, val success: Boolean)

@Composable
private fun ToastSnackbar(toast: ToastInfo) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (toast.success) HToGoColors.AccentEmerald else HToGoColors.AccentAmber,
            shadowElevation = 6.dp
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (toast.success) Icons.Filled.Bolt else Icons.Filled.HourglassTop,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    toast.mensaje,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 868)
@Composable
fun ProductosPreciosPreview() { HToGoTheme { ProductosPreciosScreen() } }
