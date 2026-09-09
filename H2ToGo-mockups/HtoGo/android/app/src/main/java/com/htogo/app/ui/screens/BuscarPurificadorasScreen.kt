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
import androidx.compose.material.icons.filled.Star
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
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme

private data class PurificadoraResumen(
    val id: String,
    val nombre: String,
    val distancia: String,
    val direccion: String,
    val rating: Float,
    val resenas: Int,
    val precioDesde: Int,
    val abierto: Boolean,
    val horarioCierre: String,
    val tags: List<String>
)

private val SAMPLE_PURIFICADORAS = listOf(
    PurificadoraResumen(
        "p1", "Aguas Del Valle", "0.8 km",
        "Av. Cuauhtémoc 1102, Benito Juárez", 4.8f, 312, 42, true,
        "Cierra 7:00 PM", listOf("Ciel", "Bonafont", "Epura")
    ),
    PurificadoraResumen(
        "p2", "HidroExpress BJ", "1.4 km",
        "Eje 5 Sur 320, Narvarte", 4.6f, 188, 38, true,
        "Cierra 8:00 PM", listOf("Ciel", "Santorini")
    ),
    PurificadoraResumen(
        "p3", "AquaPura Nápoles", "2.1 km",
        "Heriberto Frías 612, Nápoles", 4.7f, 254, 45, true,
        "Cierra 6:30 PM", listOf("Bonafont", "Epura")
    ),
    PurificadoraResumen(
        "p4", "Purificadora Aqua Pura", "2.8 km",
        "Diagonal San Antonio 980", 4.4f, 142, 40, false,
        "Abre mañana 8:00 AM", listOf("Ciel", "Bonafont")
    ),
    PurificadoraResumen(
        "p5", "Agua Clara Del Valle", "3.2 km",
        "Insurgentes Sur 1456, Del Valle", 4.5f, 96, 36, true,
        "Cierra 9:00 PM", listOf("Santorini", "Epura")
    )
)

@Composable
fun BuscarPurificadorasScreen(
    onBack: () -> Unit = {},
    onAbrirPurificadora: () -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    val resultados = remember(query) {
        if (query.isBlank()) SAMPLE_PURIFICADORAS
        else SAMPLE_PURIFICADORAS.filter {
            it.nombre.contains(query, ignoreCase = true) ||
                it.direccion.contains(query, ignoreCase = true) ||
                it.tags.any { t -> t.contains(query, ignoreCase = true) }
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
                    EmptyState()
                }
            } else {
                items(resultados, key = { it.id }) { p ->
                    PurificadoraCard(p, onClick = onAbrirPurificadora)
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
            .padding(horizontal = 8.dp, vertical = 10.dp)
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
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Icon(Icons.Filled.Star, null, tint = HToGoColors.AccentAmber, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(
                            "%.1f".format(p.rating),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HToGoColors.TextPrimary
                        )
                        Text(
                            " (${p.resenas})",
                            fontSize = 12.sp,
                            color = HToGoColors.TextSecondary
                        )
                        Text(" · ", fontSize = 12.sp, color = HToGoColors.TextTertiary)
                        Icon(Icons.Filled.LocationOn, null, tint = HToGoColors.TextSecondary, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(
                            p.distancia,
                            fontSize = 12.sp, color = HToGoColors.TextSecondary
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Desde",
                        fontSize = 10.sp,
                        color = HToGoColors.TextTertiary
                    )
                    Text(
                        "$${p.precioDesde}",
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
                EstadoPill(abierto = p.abierto, horarioCierre = p.horarioCierre)
                Spacer(Modifier.width(6.dp))
                p.tags.take(3).forEach { tag ->
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
private fun EmptyState() {
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
            "Sin resultados",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = HToGoColors.TextPrimary
        )
        Text(
            "Probá con otra búsqueda o quita filtros",
            fontSize = 12.sp,
            color = HToGoColors.TextSecondary
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 868)
@Composable
fun BuscarPurificadorasPreview() { HToGoTheme { BuscarPurificadorasScreen() } }
