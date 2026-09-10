package com.htogo.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htogo.app.ui.components.RepartidorBottomBar
import com.htogo.app.ui.components.RepartidorTab
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme

private data class PerfilUsuario(
    val nombre: String,
    val iniciales: String,
    val nombreNegocio: String,
    val telefono: String,
    val correo: String,
    val totalEntregas: Int,
    val antiguedadMeses: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilRepartidorScreen(
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    onInicio: () -> Unit = {},
    onNegocio: () -> Unit = {},
    onIngresos: () -> Unit = {}
) {
    val usuario = remember {
        PerfilUsuario(
            nombre = "Carlos Mendoza Ramírez",
            iniciales = "CM",
            nombreNegocio = "Aguas Del Valle",
            telefono = "+52 55 1234 5678",
            correo = "carlos.mendoza@aguasdelvalle.mx",
            totalEntregas = 1284,
            antiguedadMeses = 8
        )
    }

    var enLinea by remember { mutableStateOf(true) }
    var notifPush by remember { mutableStateOf(true) }
    var notifEmail by remember { mutableStateOf(false) }
    var notifNovedades by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = HToGoColors.Background,
        bottomBar = {
            RepartidorBottomBar(
                selected = RepartidorTab.PERFIL,
                onInicio = onInicio,
                onNegocio = onNegocio,
                onIngresos = onIngresos,
                onPerfil = {}
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(HToGoColors.PrimaryDark, HToGoColors.Primary)))
                        .padding(start = 8.dp, end = 18.dp, top = 8.dp, bottom = 16.dp)
                        .padding(bottom = 56.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                        }
                        Text(
                            "Mi perfil",
                            color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f).padding(start = 4.dp)
                        )
                        IconButton(onClick = {}) {
                            Icon(Icons.Outlined.NotificationsNone, null, tint = Color.White)
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Filled.Settings, null, tint = Color.White)
                        }
                    }
                }
            }

            item {
                ProfileCard(
                    u = usuario,
                    enLinea = enLinea,
                    onToggleLinea = { enLinea = !enLinea },
                    modifier = Modifier
                        .padding(horizontal = 18.dp)
                        .offset(y = (-42).dp)
                )
            }

            item {
                SectionHeader("Mi cuenta")
                Card(
                    modifier = Modifier.padding(horizontal = 18.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HToGoColors.OutlineSoft),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column {
                        InfoRow(Icons.Filled.Person, "Nombre", usuario.nombre)
                        InfoRow(Icons.Filled.Phone, "Teléfono", usuario.telefono)
                        InfoRow(Icons.Filled.MailOutline, "Correo", usuario.correo, last = true)
                    }
                }
            }

            item {
                SectionHeader("Notificaciones")
                Card(
                    modifier = Modifier.padding(horizontal = 18.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HToGoColors.OutlineSoft),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column {
                        SwitchRow(Icons.Filled.NotificationsActive, "Notificaciones push",
                            "Nuevos pedidos y mensajes", notifPush) { notifPush = it }
                        SwitchRow(Icons.Filled.MailOutline, "Notificaciones por correo",
                            "Resúmenes y novedades", notifEmail) { notifEmail = it }
                        SwitchRow(Icons.Filled.Campaign, "Novedades",
                            "Tips para hacer crecer tu negocio", notifNovedades, isLast = true) { notifNovedades = it }
                    }
                }
            }

            item {
                SectionHeader("Cuenta")
                Card(
                    modifier = Modifier.padding(horizontal = 18.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HToGoColors.OutlineSoft),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column {
                        ActionRow(Icons.Outlined.Lock, "Cambiar contraseña", redText = false, onClick = {})
                        ActionRow(Icons.Filled.Logout, "Cerrar sesión", redText = true, isLast = true, onClick = onLogout)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "HToGo · v1.0.0",
                    fontSize = 11.sp, color = HToGoColors.TextTertiary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(
    u: PerfilUsuario,
    enLinea: Boolean,
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
                    Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(HToGoColors.Primary, HToGoColors.PrimaryLight))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(u.iniciales, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        u.nombre.split(" ").take(2).joinToString(" "),
                        fontSize = 17.sp, fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Icon(Icons.Filled.Storefront, null, tint = HToGoColors.Primary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            u.nombreNegocio,
                            fontSize = 13.sp, color = HToGoColors.Primary, fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = HToGoColors.AccentEmerald.copy(alpha = .12f),
                border = BorderStroke(1.dp, HToGoColors.AccentEmerald.copy(alpha = .25f))
            ) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(HToGoColors.AccentEmerald)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (enLinea) "Estás en línea · Recibiendo pedidos" else "Estás desconectado",
                        fontSize = 13.sp, modifier = Modifier.weight(1f)
                    )
                    Switch(checked = enLinea, onCheckedChange = { onToggleLinea() })
                }
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = HToGoColors.OutlineSoft)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MiniProfileStat("${u.totalEntregas}", "Entregas")
                Box(
                    Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(HToGoColors.OutlineSoft)
                )
                MiniProfileStat("${u.antiguedadMeses} mes", "Antigüedad")
            }
        }
    }
}

@Composable
private fun MiniProfileStat(v: String, l: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(v, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(l.uppercase(), fontSize = 10.sp, color = HToGoColors.TextSecondary)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = HToGoColors.TextSecondary,
        modifier = Modifier.padding(start = 22.dp, end = 18.dp, top = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    valor: String,
    last: Boolean = false
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(HToGoColors.PrimarySoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = HToGoColors.Primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = HToGoColors.TextSecondary)
            Text(valor, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
    if (!last) HorizontalDivider(color = HToGoColors.OutlineSoft)
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    titulo: String,
    sub: String,
    checked: Boolean,
    isLast: Boolean = false,
    onChange: (Boolean) -> Unit
) {
    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(HToGoColors.Background),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = HToGoColors.TextSecondary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(sub, fontSize = 11.sp, color = HToGoColors.TextSecondary)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = HToGoColors.Primary
            )
        )
    }
    if (!isLast) HorizontalDivider(color = HToGoColors.OutlineSoft)
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    titulo: String,
    redText: Boolean,
    isLast: Boolean = false,
    onClick: () -> Unit
) {
    val accent = HToGoColors.AccentRose
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (redText) accent.copy(alpha = .12f) else HToGoColors.Background),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, null,
                tint = if (redText) accent else HToGoColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            titulo,
            fontSize = 14.sp, fontWeight = FontWeight.Medium,
            color = if (redText) accent else HToGoColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Filled.ChevronRight, null, tint = HToGoColors.TextTertiary)
    }
    if (!isLast) HorizontalDivider(color = HToGoColors.OutlineSoft)
}

@Preview(showBackground = true, widthDp = 412, heightDp = 868)
@Composable
fun PerfilRepartidorScreenPreview() {
    HToGoTheme { PerfilRepartidorScreen() }
}
