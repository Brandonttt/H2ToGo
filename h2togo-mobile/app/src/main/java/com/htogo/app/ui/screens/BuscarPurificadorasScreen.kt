package com.htogo.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.htogo.app.ui.ClienteViewModel
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme

private data class PurificadoraResumen(
    val id: Int,
    val nombre: String,
    /** null si no se conoce la ubicación del negocio o del cliente. */
    val distancia: String?,
    val direccion: String,
    val precioDesde: Double?,
    val abierto: Boolean,
    val marcas: List<String>
)

private fun calcularDistanciaKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}

@Composable
fun BuscarPurificadorasScreen(
    onBack: () -> Unit = {},
    onAbrirPurificadora: () -> Unit = {},
    clienteViewModel: ClienteViewModel = viewModel()
) {
    var query by remember { mutableStateOf("") }
    val livePurificadoras by clienteViewModel.purificadoras.collectAsState()
    val direccionesCliente by clienteViewModel.direcciones.collectAsState()
    val direccionCliente = remember(direccionesCliente) {
        direccionesCliente.firstOrNull()
    }

    // Solo datos reales del backend (antes había calificaciones, reseñas, precios y marcas
    // inventados, y una lista de ejemplo cuando no había resultados).
    val sourceList = remember(livePurificadoras, direccionCliente) {
        livePurificadoras.map { p ->
            val distKm = when {
                p.distanciaKm > 0.0 -> p.distanciaKm
                direccionCliente != null && p.lat != null && p.lon != null && (p.lat != 0.0 || p.lon != 0.0) ->
                    calcularDistanciaKm(direccionCliente.lat, direccionCliente.lon, p.lat, p.lon)
                else -> null
            }
            PurificadoraResumen(
                id = p.id,
                nombre = p.nombreComercial,
                distancia = distKm?.let {
                    if (it < 1.0) "${(it * 1000).toInt().coerceAtLeast(50)} m"
                    else String.format(java.util.Locale.US, "%.1f km", it)
                },
                direccion = p.direccion?.takeIf { it.isNotBlank() } ?: "Benito Juárez, CDMX",
                precioDesde = p.precioDesde,
                abierto = p.abiertoAhora,
                marcas = p.marcas.orEmpty()
            )
        }
    }

    val resultados = remember(query, sourceList) {
        if (query.isBlank()) sourceList
        else sourceList.filter {
            it.nombre.contains(query, ignoreCase = true) ||
                it.direccion.contains(query, ignoreCase = true) ||
                it.marcas.any { t -> t.contains(query, ignoreCase = true) }
        }
    }

    Scaffold(
        containerColor = HToGoColors.Background,
        topBar = { Header(query, { query = it }, onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${resultados.size} purificadoras",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HToGoColors.TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, HToGoColors.OutlineSoft)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Tune, null,
                                tint = HToGoColors.TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Cercanía",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HToGoColors.TextPrimary
                            )
                        }
                    }
                }
            }
            if (resultados.isEmpty()) {
                item {
                    if (sourceList.isEmpty()) {
                        EmptyState("Sin purificadoras cerca", "Aún no hay purificadoras con cobertura en tu zona.")
                    } else {
                        EmptyState("Sin resultados", "Prueba con otro nombre, marca o colonia.")
                    }
                }
            } else {
                items(resultados, key = { it.id }) { p ->
                    PurificadoraCard(p, onClick = {
                        clienteViewModel.purificadoraSeleccionadaId = p.id
                        clienteViewModel.purificadoraSeleccionadaNombre = p.nombre
                        clienteViewModel.purificadoraSeleccionadaDistancia = p.distancia
                        clienteViewModel.cargarPerfilPurificadora(p.id)
                        onAbrirPurificadora()
                    })
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun Header(query: String, onQuery: (String) -> Unit, onBack: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(HToGoColors.PrimaryDark, HToGoColors.Primary)))
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .padding(bottom = 8.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Elegir purificadora",
                        color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Pide a un negocio específico",
                        color = HToGoColors.PrimarySoft, fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Search, null, tint = HToGoColors.TextTertiary)
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                "Buscar por nombre, marca o colonia",
                                fontSize = 13.sp,
                                color = HToGoColors.TextTertiary
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = onQuery,
                            singleLine = true,
                            textStyle = TextStyle(color = HToGoColors.TextPrimary, fontSize = 14.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQuery("") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Filled.Close, null, tint = HToGoColors.TextTertiary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PurificadoraCard(p: PurificadoraResumen, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, HToGoColors.OutlineSoft),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(HToGoColors.PrimarySoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Storefront, null, tint = HToGoColors.Primary, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        p.nombre,
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = HToGoColors.TextPrimary
                    )
                    if (p.distancia != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            Icon(Icons.Filled.LocationOn, null, tint = HToGoColors.TextSecondary, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(p.distancia, fontSize = 12.sp, color = HToGoColors.TextSecondary)
                        }
                    }
                }
                if (p.precioDesde != null) Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Desde",
                        fontSize = 10.sp,
                        color = HToGoColors.TextTertiary
                    )
                    Text(
                        if (p.precioDesde % 1.0 == 0.0) "$%.0f".format(p.precioDesde) else "$%.2f".format(p.precioDesde),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = HToGoColors.Primary
                    )
                    Text(
                        "/ 20 L",
                        fontSize = 10.sp,
                        color = HToGoColors.TextTertiary
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                p.direccion,
                fontSize = 12.sp,
                color = HToGoColors.TextSecondary,
                maxLines = 1
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                EstadoPill(abierto = p.abierto, horarioCierre = if (p.abierto) "Abierto ahora" else "Cerrado")
                Spacer(Modifier.width(6.dp))
                p.marcas.take(3).forEach { tag ->
                    TagPill(tag)
                    Spacer(Modifier.width(6.dp))
                }
            }
        }
    }
}

@Composable
private fun EstadoPill(abierto: Boolean, horarioCierre: String) {
    val color = if (abierto) HToGoColors.AccentEmerald else HToGoColors.TextTertiary
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = color.copy(alpha = .12f)
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(5.dp))
            Icon(
                Icons.Filled.Schedule, null,
                tint = color,
                modifier = Modifier.size(11.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                horarioCierre,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
private fun TagPill(label: String) {
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = HToGoColors.Background,
        border = BorderStroke(1.dp, HToGoColors.OutlineSoft)
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.WaterDrop, null,
                tint = HToGoColors.Primary,
                modifier = Modifier.size(10.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = HToGoColors.TextSecondary
            )
        }
    }
}

@Composable
private fun EmptyState(titulo: String, detalle: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(HToGoColors.PrimarySoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Search, null, tint = HToGoColors.Primary, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            titulo,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = HToGoColors.TextPrimary
        )
        Text(
            detalle,
            fontSize = 12.sp,
            color = HToGoColors.TextSecondary
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 868)
@Composable
fun BuscarPurificadorasPreview() { HToGoTheme { BuscarPurificadorasScreen() } }
