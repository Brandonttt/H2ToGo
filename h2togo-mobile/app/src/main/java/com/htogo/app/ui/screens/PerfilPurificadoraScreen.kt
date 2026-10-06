package com.htogo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import com.htogo.app.ui.components.OsmMapView
import com.htogo.app.ui.components.HToGoButton
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme

import androidx.lifecycle.viewmodel.compose.viewModel
import com.htogo.app.ui.ClienteViewModel

private data class Producto(val marca: String, val capacidad: String, val precio: String, val stock: StockEstado)
private enum class StockEstado(val label: String, val color: Color) {
    DISPONIBLE("Disponible", HToGoColors.AccentEmerald),
    POCOS("Pocas unidades", HToGoColors.AccentAmber),
    AGOTADO("Agotado", HToGoColors.TextTertiary)
}

@Composable
fun PerfilPurificadoraScreen(
    nombre: String = "Aguas Del Valle",
    distancia: String = "0.8 km",
    direccion: String = "Av. Cuauhtémoc 1102, Benito Juárez",
    horario: String = "Lun–Sáb · 8:00 AM – 7:00 PM",
    diasCerrados: String = "Cerrado los domingos",
    abiertoAhora: Boolean = true,
    tiempoEnPlataforma: String = "Purificadora verificada HToGo",
    onBack: () -> Unit = {},
    onPedir: () -> Unit = {},
    clienteViewModel: ClienteViewModel = viewModel()
) {
    val perfil by clienteViewModel.perfilPurificadora.collectAsState()
    val isLoading by clienteViewModel.isLoading.collectAsState()

    val displayNombre = perfil?.nombreComercial ?: clienteViewModel.purificadoraSeleccionadaNombre ?: nombre
    // Sin dato real (p. ej. al entrar desde el inicio) no se muestra distancia.
    val displayDistancia = clienteViewModel.purificadoraSeleccionadaDistancia
    val displayDireccion = perfil?.direccion ?: direccion
    val displayAbierto = perfil?.abiertoAhora ?: abiertoAhora

    val displayHorario = remember(perfil) {
        val hList = perfil?.horarios
        if (!hList.isNullOrEmpty()) {
            val abiertos = hList.filter { !it.cerrado }
            if (abiertos.isNotEmpty()) {
                val primero = abiertos.first()
                "Horario: ${primero.horaApertura?.take(5) ?: "--:--"} – ${primero.horaCierre?.take(5) ?: "--:--"}"
            } else {
                "Cerrado temporalmente"
            }
        } else if (perfil != null) {
            "Horario no registrado"
        } else {
            horario
        }
    }
    // Días cerrados según el horario real (antes decía siempre "Cerrado los domingos").
    val displayDiasCerrados = remember(perfil) {
        val nombres = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
        val cerrados = perfil?.horarios.orEmpty().filter { it.cerrado }.map { nombres[(it.diaSemana - 1).coerceIn(0, 6)] }
        when {
            perfil == null -> diasCerrados
            cerrados.isEmpty() -> ""
            else -> "Cerrado: " + cerrados.joinToString(", ")
        }
    }

    val productos = remember(perfil) {
        if (!perfil?.productos.isNullOrEmpty()) {
            perfil!!.productos!!.map { prod ->
                val precio = prod.precioLiquido
                // Antes 0 piezas se mostraba como "pocos".
                val stock = when {
                    prod.stockDisponible > 5 -> StockEstado.DISPONIBLE
                    prod.stockDisponible > 0 -> StockEstado.POCOS
                    else -> StockEstado.AGOTADO
                }
                Producto(
                    marca = prod.nombreMarca,
                    capacidad = "20 L",
                    precio = (if (precio % 1.0 == 0.0) "$%.0f" else "$%.2f").format(precio) + " c/u",
                    stock = stock
                )
            }
        } else {
            emptyList()
        }
    }

    Scaffold(
        containerColor = HToGoColors.Background,
        bottomBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                HToGoButton(text = "Pedir a esta purificadora", onClick = onPedir)
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            HeaderPurificadora(displayNombre, displayDistancia, displayAbierto, onBack)

            if (isLoading && perfil == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = HToGoColors.Primary)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Cargando información de la purificadora...",
                            fontSize = 13.sp,
                            color = HToGoColors.TextSecondary
                        )
                    }
                }
            } else {
                MiniMapa(
                    lat = perfil?.direccionObj?.lat,
                    lon = perfil?.direccionObj?.lon
                )
                Spacer(Modifier.height(20.dp))
                DatosNegocio(displayDireccion, displayHorario, displayDiasCerrados, displayAbierto, tiempoEnPlataforma)
                Spacer(Modifier.height(20.dp))
                SectionTitle("Productos disponibles")
                if (productos.isNotEmpty()) {
                    productos.forEach { ProductoRow(it) }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.WaterDrop, null, tint = HToGoColors.Primary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Garrafón de agua purificada 20 L disponible al ordenar.",
                                fontSize = 13.sp,
                                color = HToGoColors.TextSecondary
                            )
                        }
                    }
                }
                Spacer(Modifier.height(96.dp))
            }
        }
    }
}

@Composable
private fun HeaderPurificadora(
    nombre: String, distancia: String?, abierto: Boolean, onBack: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(HToGoColors.PrimaryDark, HToGoColors.Primary)
                )
            )
            .statusBarsPadding()
            .padding(top = 10.dp, bottom = 20.dp, start = 8.dp, end = 20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
                Spacer(Modifier.width(4.dp))
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Storefront, null, tint = HToGoColors.Primary,
                        modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(nombre, color = Color.White, fontSize = 20.sp,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EstadoNegocioBadge(abierto)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (distancia != null) {
                    Icon(Icons.Filled.LocationOn, null, tint = HToGoColors.PrimarySoft,
                        modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(distancia, color = HToGoColors.PrimarySoft, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun EstadoNegocioBadge(abierto: Boolean) {
    val color = if (abierto) HToGoColors.AccentEmerald else HToGoColors.TextTertiary
    val label = if (abierto) "● Abierto" else "● Cerrado"
    Box(
        Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MiniMapa(lat: Double? = null, lon: Double? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp)
            .height(150.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF3EE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        if (lat != null && lon != null && lat != 0.0 && lon != 0.0) {
            OsmMapView(
                latitude = lat,
                longitude = lon,
                zoom = 16,
                isInteractive = false,
                isDraggablePin = false,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(HToGoColors.Primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Storefront, null, tint = Color.White,
                            modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Base de la purificadora",
                        fontSize = 11.sp, color = HToGoColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun DatosNegocio(
    direccion: String, horario: String, diasCerrados: String,
    abierto: Boolean, tiempoEnPlataforma: String
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            DatoRow(Icons.Filled.LocationOn, "Dirección de la base", direccion)
            Spacer(Modifier.height(10.dp))
            DatoRow(
                Icons.Filled.Schedule,
                "Horario de atención",
                if (diasCerrados.isBlank()) horario else "$horario\n$diasCerrados"
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(8.dp).clip(CircleShape)
                        .background(if (abierto) HToGoColors.AccentEmerald else HToGoColors.TextTertiary)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (abierto) "Abierto ahora" else "Cerrado ahora",
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = if (abierto) HToGoColors.AccentEmerald else HToGoColors.TextTertiary
                )
                Spacer(Modifier.width(12.dp))
                Text("· $tiempoEnPlataforma",
                    fontSize = 12.sp, color = HToGoColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun DatoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(10.dp))
                .background(HToGoColors.PrimaryWash),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = HToGoColors.Primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 11.sp, color = HToGoColors.TextSecondary,
                fontWeight = FontWeight.SemiBold)
            Text(value, fontSize = 13.sp, color = HToGoColors.TextPrimary, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = HToGoColors.TextPrimary,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun ProductoRow(p: Producto) {
    val agotado = p.stock == StockEstado.AGOTADO
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (agotado) Color(0xFFF8FAFC) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (agotado) 0.dp else 1.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(HToGoColors.PrimaryWash),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.WaterDrop, null,
                    tint = if (agotado) HToGoColors.TextTertiary else HToGoColors.Primary,
                    modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${p.marca} ${p.capacidad}",
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = if (agotado) HToGoColors.TextTertiary else HToGoColors.TextPrimary)
                Text(p.precio, fontSize = 12.sp, color = HToGoColors.TextSecondary)
            }
            StockChip(p.stock)
        }
    }
}

@Composable
private fun StockChip(stock: StockEstado) {
    Box(
        Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(stock.color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(stock.label, color = stock.color, fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold)
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 868)
@Composable
private fun PerfilPurificadoraPreview() { HToGoTheme { PerfilPurificadoraScreen() } }
