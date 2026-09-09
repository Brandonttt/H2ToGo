package mx.htogo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.htogo.ui.theme.HToGoTheme

/**
 * Pantalla 14 — Perfil del repartidor (dueño de purificadora).
 *
 * Sin verificación de documentos, sin gestión de productos (eso vive en inventario),
 * sin propinas. Permite múltiples vehículos, calificaciones anónimas y un solo
 * toggle de novedades.
 */

data class NegocioInfo(
    val titular: String,
    val nombreComercial: String,
    val iniciales: String,
    val direccion: String,
    val telefono: String,
    val correo: String,
    val rating: Double,
    val numCalificaciones: Int,
    val totalEntregas: Int,
    val antiguedadMeses: Int,
    val porcentajeConcretadas: Int
)

data class VehiculoRepartidor(
    val id: String,
    val tipo: String,
    val marca: String,
    val modelo: String,
    val anio: Int,
    val placas: String,
    val capacidad: Int,
    val esPrincipal: Boolean
)

data class ResenaAnonima(
    val rating: Int,
    val texto: String,
    val fecha: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilRepartidorScreen(
    onBack: () -> Unit = {},
    onEditar: (String) -> Unit = {},
    onAgregarVehiculo: () -> Unit = {},
    onEditarVehiculo: (VehiculoRepartidor) -> Unit = {},
    onCompartirQR: () -> Unit = {},
    onVerResenas: () -> Unit = {},
    onCambiarPassword: () -> Unit = {},
    onCerrarSesion: () -> Unit = {}
) {
    val primary = Color(0xFF0077B6)
    val primaryDark = Color(0xFF03045E)
    val primaryLight = Color(0xFF00B4D8)
    val primarySoft = Color(0xFFCAF0F8)
    val success = Color(0xFF10B981)
    val warning = Color(0xFFF59E0B)
    val danger = Color(0xFFEF4444)
    val bg = Color(0xFFF8FAFC)
    val outline = Color(0xFFE2E8F0)
    val text2 = Color(0xFF64748B)

    val negocio = remember {
        NegocioInfo(
            titular = "Carlos Mendoza Ramírez",
            nombreComercial = "Purificadora Aqua Pura",
            iniciales = "CM",
            direccion = "Av. Cuauhtémoc 145, Col. Roma Norte, CDMX",
            telefono = "+52 55 1234 5678",
            correo = "carlos.mendoza@aquapura.mx",
            rating = 4.9,
            numCalificaciones = 247,
            totalEntregas = 1284,
            antiguedadMeses = 8,
            porcentajeConcretadas = 98
        )
    }

    val vehiculos = remember {
        mutableStateListOf(
            VehiculoRepartidor("v1", "Pickup", "Nissan", "Frontier", 2020, "RPK-285-A", 30, true),
            VehiculoRepartidor("v2", "Motocicleta", "Italika", "DS150", 2022, "NMR-78-2", 6, false)
        )
    }

    val resenas = remember {
        listOf(
            ResenaAnonima(5, "Excelente servicio, muy puntual y los garrafones limpios. Recomendado.", "Hoy"),
            ResenaAnonima(5, "Siempre nos surte a tiempo, gran trato.", "Ayer"),
            ResenaAnonima(4, "Buen servicio, llegó un poco tarde pero todo bien.", "2 días")
        )
    }

    var enLinea by remember { mutableStateOf(true) }
    var notifPush by remember { mutableStateOf(true) }
    var notifEmail by remember { mutableStateOf(false) }
    var notifNovedades by remember { mutableStateOf(true) }

    Scaffold(containerColor = bg) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Header
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(primaryDark, primary)))
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                        .padding(bottom = 56.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Mi perfil", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f))
                        IconButton(onClick = {}, modifier = Modifier.size(38.dp)
                            .clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .15f))) {
                            Icon(Icons.Outlined.NotificationsNone, null, tint = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = {}, modifier = Modifier.size(38.dp)
                            .clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .15f))) {
                            Icon(Icons.Default.Settings, null, tint = Color.White)
                        }
                    }
                }
            }

            item {
                ProfileCard(
                    n = negocio,
                    enLinea = enLinea,
                    primary = primary, primaryLight = primaryLight, success = success,
                    warning = warning, outline = outline,
                    onToggleLinea = { enLinea = !enLinea },
                    modifier = Modifier.padding(horizontal = 18.dp).offset(y = (-42).dp)
                )
            }

            // Información del negocio
            item {
                SectionHeader("Información del negocio")
                InfoCard(primary, outline) {
                    InfoRow(Icons.Default.Person, "Titular", negocio.titular, primary, primarySoft) { onEditar("titular") }
                    InfoRow(Icons.Default.Storefront, "Nombre comercial", negocio.nombreComercial, primary, primarySoft) { onEditar("nombre") }
                    InfoRow(Icons.Default.Place, "Dirección de la base", negocio.direccion, primary, primarySoft) { onEditar("direccion") }
                    InfoRow(Icons.Default.Phone, "Teléfono", negocio.telefono, primary, primarySoft) { onEditar("telefono") }
                    InfoRow(Icons.Default.MailOutline, "Correo electrónico", negocio.correo, primary, primarySoft, last = true) { onEditar("correo") }
                }
            }

            // Vehículos
            item {
                Row(Modifier.padding(start = 22.dp, end = 18.dp, top = 18.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("MIS VEHÍCULOS", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = text2, modifier = Modifier.weight(1f))
                    TextButton(onClick = onAgregarVehiculo) { Text("+ Agregar", color = primary) }
                }
            }
            items(vehiculos) { v ->
                VehiculoCard(v, primary, primarySoft, outline,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                    onEditar = { onEditarVehiculo(v) })
            }
            item {
                OutlinedButton(
                    onClick = onAgregarVehiculo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 6.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, primary)
                ) {
                    Icon(Icons.Default.AddCircleOutline, null, tint = primary)
                    Spacer(Modifier.width(6.dp))
                    Text("Agregar otro vehículo", color = primary, fontWeight = FontWeight.SemiBold)
                }
            }

            // Horario
            item {
                SectionHeader("Horario de atención")
                HorarioCard(primary, success, outline,
                    modifier = Modifier.padding(horizontal = 18.dp))
            }

            // QR
            item {
                SectionHeader("Comparte tu purificadora")
                QRCard(primary, primaryLight,
                    modifier = Modifier.padding(horizontal = 18.dp),
                    onCompartir = onCompartirQR)
            }

            // Reseñas anónimas
            item {
                Row(Modifier.padding(start = 22.dp, end = 18.dp, top = 18.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("CALIFICACIONES RECIENTES", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = text2, modifier = Modifier.weight(1f))
                    TextButton(onClick = onVerResenas) {
                        Text("Ver todas (${negocio.numCalificaciones})", color = primary)
                    }
                }
                Card(
                    modifier = Modifier.padding(horizontal = 18.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, outline),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column {
                        resenas.forEachIndexed { i, r ->
                            ResenaRow(r, warning, outline, isLast = i == resenas.lastIndex)
                        }
                    }
                }
            }

            // Notificaciones
            item {
                SectionHeader("Notificaciones")
                Card(
                    modifier = Modifier.padding(horizontal = 18.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, outline),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column {
                        SwitchRow(Icons.Default.NotificationsActive, "Notificaciones push",
                            "Nuevos pedidos y mensajes", notifPush, primary, outline) { notifPush = it }
                        SwitchRow(Icons.Default.MailOutline, "Notificaciones por correo",
                            "Resúmenes y novedades", notifEmail, primary, outline) { notifEmail = it }
                        SwitchRow(Icons.Default.Campaign, "Novedades",
                            "Tips para hacer crecer tu negocio", notifNovedades, primary, outline,
                            isLast = true) { notifNovedades = it }
                    }
                }
            }

            // Cuenta
            item {
                SectionHeader("Cuenta")
                Card(
                    modifier = Modifier.padding(horizontal = 18.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, outline),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column {
                        ActionRow(Icons.Outlined.Lock, "Cambiar contraseña", primary, false, outline, onClick = onCambiarPassword)
                        ActionRow(Icons.Default.Logout, "Cerrar sesión", danger, true, outline, isLast = true, onClick = onCerrarSesion)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("HToGo · v1.0.0",
                    fontSize = 11.sp, color = Color.Gray,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ProfileCard(
    n: NegocioInfo, enLinea: Boolean,
    primary: Color, primaryLight: Color, success: Color, warning: Color, outline: Color,
    onToggleLinea: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(72.dp).clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(primary, primaryLight))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(n.iniciales, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(n.titular.split(" ").take(2).joinToString(" "),
                        fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(n.nombreComercial, fontSize = 13.sp, color = primary, fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.Star, null, tint = warning, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${n.rating}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(" · ${n.numCalificaciones} calificaciones", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = success.copy(alpha = .12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, success.copy(alpha = .25f))
            ) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(success))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Estás " + (if (enLinea) "en línea · Recibiendo pedidos" else "desconectado"),
                        fontSize = 13.sp, modifier = Modifier.weight(1f)
                    )
                    Switch(checked = enLinea, onCheckedChange = { onToggleLinea() })
                }
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = outline)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MiniProfileStat("${n.totalEntregas}", "Entregas")
                Box(Modifier.width(1.dp).height(32.dp).background(outline))
                MiniProfileStat("${n.antiguedadMeses} mes", "Antigüedad")
                Box(Modifier.width(1.dp).height(32.dp).background(outline))
                MiniProfileStat("${n.porcentajeConcretadas}%", "Concretadas")
            }
        }
    }
}

@Composable
private fun MiniProfileStat(v: String, l: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(v, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(l.uppercase(), fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF64748B),
        modifier = Modifier.padding(start = 22.dp, end = 18.dp, top = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun InfoCard(primary: Color, outline: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.padding(horizontal = 18.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, outline),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, valor: String,
    primary: Color, primarySoft: Color,
    last: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(primarySoft),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = primary, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = Color.Gray)
            Text(valor, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        Icon(Icons.Default.Edit, null, tint = primary, modifier = Modifier.size(18.dp))
    }
    if (!last) HorizontalDivider(color = Color(0xFFE2E8F0))
}

@Composable
private fun VehiculoCard(
    v: VehiculoRepartidor, primary: Color, primarySoft: Color, outline: Color,
    modifier: Modifier, onEditar: () -> Unit
) {
    val borderColor = if (v.esPrincipal) primary.copy(alpha = .3f) else outline
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (v.esPrincipal) primary.copy(alpha = .04f) else Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(if (v.esPrincipal) 1.5.dp else 1.dp, borderColor),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            if (v.esPrincipal) {
                Surface(shape = RoundedCornerShape(99.dp), color = primary,
                    modifier = Modifier.padding(bottom = 8.dp)) {
                    Text("PRINCIPAL", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color.White)
                        .border(1.dp, primarySoft, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (v.tipo == "Pickup") Icons.Default.LocalShipping else Icons.Default.TwoWheeler,
                        null, tint = primary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(v.tipo, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("${v.marca} ${v.modelo} ${v.anio} · ${v.capacidad} garrafones",
                        fontSize = 12.sp, color = Color.Gray)
                    Text(v.placas, fontSize = 11.sp, color = primary, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp))
                }
                IconButton(onClick = onEditar) {
                    Icon(Icons.Default.Edit, null, tint = primary)
                }
            }
        }
    }
}

@Composable
private fun HorarioCard(primary: Color, success: Color, outline: Color, modifier: Modifier) {
    var auto by remember { mutableStateOf(true) }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, outline),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(success.copy(alpha = .12f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Schedule, null, tint = success, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Atención automática activa", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Te conectarás y desconectarás automáticamente", fontSize = 11.sp, color = Color.Gray)
                }
                Switch(checked = auto, onCheckedChange = { auto = it })
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = outline)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TimeInput("Hora de inicio", "8:00 AM", Modifier.weight(1f))
                Icon(Icons.Default.ArrowForward, null, tint = Color.Gray,
                    modifier = Modifier.padding(horizontal = 8.dp))
                TimeInput("Hora de fin", "7:00 PM", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TimeInput(label: String, valor: String, modifier: Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label.uppercase(), fontSize = 10.sp, color = Color.Gray)
            Text(valor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun QRCard(primary: Color, primaryLight: Color, modifier: Modifier, onCompartir: () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = primary),
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(Modifier.background(Brush.linearGradient(listOf(primary, primaryLight)))) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(Color.White),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.QrCode2, null, tint = primary, modifier = Modifier.size(36.dp)) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Tu QR personal", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Comparte tu link o QR con tus clientes para que pidan directo a tu purificadora.",
                        color = Color.White.copy(alpha = .8f), fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp))
                    Button(
                        onClick = onCompartir,
                        modifier = Modifier.padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = primary)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Compartir", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResenaRow(r: ResenaAnonima, warning: Color, outline: Color, isLast: Boolean) {
    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(50))
                .background(Color(0xFF7c3aed)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Cliente anónimo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp))
                Text("★".repeat(r.rating) + "☆".repeat(5 - r.rating),
                    fontSize = 11.sp, color = warning)
                Spacer(Modifier.weight(1f))
                Text(r.fecha, fontSize = 11.sp, color = Color.Gray)
            }
            Text(r.texto, fontSize = 12.sp, color = Color.Gray, maxLines = 2,
                modifier = Modifier.padding(top = 2.dp))
        }
    }
    if (!isLast) HorizontalDivider(color = outline)
}

@Composable
private fun SwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String, sub: String, checked: Boolean, primary: Color, outline: Color,
    isLast: Boolean = false, onChange: (Boolean) -> Unit
) {
    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFF8FAFC)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(sub, fontSize = 11.sp, color = Color.Gray)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = primary)
        )
    }
    if (!isLast) HorizontalDivider(color = outline)
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String, accent: Color, redText: Boolean, outline: Color,
    isLast: Boolean = false, onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                .background(if (redText) accent.copy(alpha = .12f) else Color(0xFFF8FAFC)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = if (redText) accent else Color(0xFF64748B), modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(12.dp))
        Text(titulo, fontSize = 14.sp, fontWeight = FontWeight.Medium,
            color = if (redText) accent else Color(0xFF0F172A), modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF94A3B8))
    }
    if (!isLast) HorizontalDivider(color = outline)
}

@Preview(showBackground = true, widthDp = 412, heightDp = 868)
@Composable
fun PerfilRepartidorScreenPreview() {
    HToGoTheme { PerfilRepartidorScreen() }
}
