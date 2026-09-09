package com.htogo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme

data class UserProfile(
    val initials: String,
    val nombre: String,
    val nombreCompleto: String,
    val telefono: String,
    val email: String,
    val nacimiento: String,
    val verificado: Boolean = true,
    val totalPedidos: Int = 14,
    val totalGastado: String = "$1,892"
)

data class Direccion(
    val id: String,
    val nombre: String,
    val icon: ImageVector,
    val linea1: String,
    val linea2: String,
    val ciudad: String,
    val referencia: String,
    val predeterminada: Boolean = false
)

private val SAMPLE_USER = UserProfile(
    initials = "DG",
    nombre = "Daniel González",
    nombreCompleto = "Daniel González Pérez",
    telefono = "+52 55 1234 5678",
    email = "daniel.g@neuroplus.mx",
    nacimiento = "15 de marzo, 1995"
)

private val SAMPLE_DIRECCIONES = listOf(
    Direccion("1","Casa", Icons.Filled.Home,
        "Insurgentes Sur 1234, Int. 4B","Del Valle, Benito Juárez, 03100",
        "CDMX","Edificio azul, frente al parque", true),
    Direccion("2","Oficina", Icons.Filled.Work,
        "Av. Universidad 567, Piso 8","Narvarte, Benito Juárez, 03020",
        "CDMX","Recepción a la izquierda", false),
    Direccion("3","Casa de mamá", Icons.Filled.Favorite,
        "Heriberto Frías 890","Narvarte, Benito Juárez, 03020",
        "CDMX","Casa amarilla con portón blanco", false),
)

@Composable
fun PerfilClienteScreen(onBack: () -> Unit = {}, onLogout: () -> Unit = {}) {
    val user = SAMPLE_USER
    val direcciones = SAMPLE_DIRECCIONES
    var notif by remember { mutableStateOf(true) }

    Scaffold(
        bottomBar = { BottomNav() },
        containerColor = HToGoColors.Background
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { ProfileHeader(user, onBack) }
            item { StatsRow(user) }

            item { SectionTitle("Datos personales") }
            item { DataList(user) }

            item { SectionTitle("Mis direcciones · ${direcciones.size}") }
            items(direcciones) { d ->
                AddressCard(direccion = d)
            }
            item { AddAddressButton {} }

            item { SectionTitle("Preferencias") }
            item {
                SettingsList {
                    SettingRowSwitch(Icons.Filled.Notifications, "Notificaciones",
                        checked = notif, onCheck = { notif = it })
                    SettingRow(Icons.Filled.HelpOutline, "Ayuda y soporte")
                    SettingRow(Icons.Filled.Description, "Términos y privacidad")
                }
            }

            item { SectionTitle("Sesión") }
            item {
                SettingsList {
                    SettingRow(Icons.Filled.Logout, "Cerrar sesión",
                        danger = true, onClick = onLogout, showChevron = false)
                    SettingRow(Icons.Filled.DeleteForever, "Eliminar cuenta",
                        danger = true, onClick = {}, showChevron = false)
                }
            }

            item {
                Text("HToGo v1.0.0 · Build 142",
                    fontSize = 11.sp, color = HToGoColors.TextTertiary,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ProfileHeader(user: UserProfile, onBack: () -> Unit) {
    Box {
        Surface(color = HToGoColors.PrimaryDark, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 32.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeaderIconBtn(Icons.AutoMirrored.Filled.ArrowBack, onClick = onBack)
                    Text("Mi perfil",
                        Modifier.weight(1f),
                        fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    HeaderIconBtn(Icons.Filled.Settings, onClick = {})
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = HToGoColors.Surface,
            shadowElevation = 4.dp,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .offset(y = (-20).dp)
                .fillMaxWidth()
        ) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(HToGoColors.PrimaryLight, HToGoColors.Primary)))
                        .border(3.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(user.initials, color = Color.White,
                        fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(user.nombre, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                        color = HToGoColors.TextPrimary)
                    Text(user.telefono, fontSize = 13.sp, color = HToGoColors.TextSecondary,
                        modifier = Modifier.padding(top = 2.dp))
                    if (user.verificado) {
                        Spacer(Modifier.height(6.dp))
                        Surface(shape = RoundedCornerShape(99.dp),
                            color = HToGoColors.AccentEmerald.copy(alpha = 0.10f)) {
                            Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Verified, null,
                                    tint = HToGoColors.AccentEmerald, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Verificado", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                    color = HToGoColors.AccentEmerald)
                            }
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = HToGoColors.PrimarySoft,
                    modifier = Modifier.size(36.dp).clickable {}
                ) { Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Edit, null, tint = HToGoColors.Primary,
                        modifier = Modifier.size(18.dp))
                } }
            }
        }
    }
}

@Composable
private fun HeaderIconBtn(icon: ImageVector, onClick: () -> Unit) {
    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.10f),
        modifier = Modifier.size(40.dp).clickable(onClick = onClick)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White) }
    }
}

@Composable
private fun StatsRow(user: UserProfile) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = HToGoColors.OutlineSoft,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .offset(y = (-32).dp)
            .border(1.dp, HToGoColors.OutlineSoft, RoundedCornerShape(14.dp))
    ) {
        Row(Modifier.fillMaxWidth()) {
            StatCell("${user.totalPedidos}", "Pedidos", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCell(num: String, lbl: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Color.White).padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(num, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HToGoColors.TextPrimary)
        Text(lbl, fontSize = 11.sp, color = HToGoColors.TextSecondary,
            modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text.uppercase(),
        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        color = HToGoColors.TextSecondary, letterSpacing = 0.4.sp,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 8.dp))
}

@Composable
private fun DataList(user: UserProfile) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column {
            DataRow(Icons.Filled.Person, "Nombre completo", user.nombreCompleto)
            DividerRow()
            DataRow(Icons.Filled.Mail, "Correo electrónico", user.email)
            DividerRow()
            DataRow(Icons.Filled.Phone, "Teléfono", user.telefono)
            DividerRow()
            DataRow(Icons.Filled.Cake, "Fecha de nacimiento", user.nacimiento)
        }
    }
}

@Composable
private fun DataRow(icon: ImageVector, lbl: String, value: String) {
    Row(Modifier.fillMaxWidth().clickable {}.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
            .background(HToGoColors.PrimarySoft), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = HToGoColors.Primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(lbl, fontSize = 11.sp, color = HToGoColors.TextSecondary)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = HToGoColors.TextPrimary,
                modifier = Modifier.padding(top = 1.dp))
        }
        Icon(Icons.Filled.ChevronRight, null, tint = HToGoColors.TextTertiary)
    }
}

@Composable
private fun DividerRow() = Divider(color = HToGoColors.OutlineSoft)

@Composable
private fun AddressCard(direccion: Direccion) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(
            if (direccion.predeterminada) 2.dp else 1.dp,
            if (direccion.predeterminada) HToGoColors.Primary else HToGoColors.OutlineSoft
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                    .background(HToGoColors.PrimarySoft), contentAlignment = Alignment.Center) {
                    Icon(direccion.icon, null, tint = HToGoColors.Primary)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(direccion.nombre, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = HToGoColors.TextPrimary)
                        if (direccion.predeterminada) {
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(99.dp),
                                color = HToGoColors.Primary) {
                                Text("PREDETERMINADA",
                                    Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                                    color = Color.White, letterSpacing = 0.3.sp)
                            }
                        }
                    }
                    Text("${direccion.linea1}\n${direccion.linea2}",
                        fontSize = 12.sp, color = HToGoColors.TextSecondary,
                        modifier = Modifier.padding(top = 3.dp))
                    Text(direccion.ciudad,
                        fontSize = 11.sp, color = HToGoColors.TextTertiary,
                        modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.StickyNote2, null,
                            tint = HToGoColors.Primary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("Referencias",
                                fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                                color = HToGoColors.TextSecondary, letterSpacing = 0.3.sp)
                            Text(direccion.referencia,
                                fontSize = 12.sp, color = HToGoColors.TextPrimary)
                        }
                    }
                }
            }
            Divider(Modifier.padding(top = 10.dp, bottom = 4.dp), color = HToGoColors.OutlineSoft)
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                if (!direccion.predeterminada) {
                    AddrAction("Predeterminar", Icons.Outlined.StarOutline,
                        HToGoColors.Primary, Modifier.weight(1f)) {}
                }
                AddrAction("Editar", Icons.Filled.Edit,
                    HToGoColors.TextSecondary, Modifier.weight(1f)) {}
                AddrAction("Eliminar", Icons.Outlined.DeleteOutline,
                    HToGoColors.AccentRose, Modifier.weight(1f)) {}
            }
        }
    }
}

@Composable
private fun AddrAction(label: String, icon: ImageVector, color: Color,
                      modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

@Composable
private fun AddAddressButton(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, HToGoColors.Primary),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AddLocationAlt, null, tint = HToGoColors.Primary)
            Spacer(Modifier.width(8.dp))
            Text("Agregar nueva dirección",
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = HToGoColors.Primary)
        }
    }
}

@Composable
private fun SettingsList(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column { content() }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    label: String,
    trailingText: String? = null,
    danger: Boolean = false,
    showChevron: Boolean = true,
    onClick: () -> Unit = {}
) {
    Column {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                .background(if (danger) HToGoColors.AccentRose.copy(alpha = 0.08f) else HToGoColors.Background),
                contentAlignment = Alignment.Center) {
                Icon(icon, null,
                    tint = if (danger) HToGoColors.AccentRose else HToGoColors.TextPrimary,
                    modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(label, fontSize = 14.sp,
                color = if (danger) HToGoColors.AccentRose else HToGoColors.TextPrimary,
                modifier = Modifier.weight(1f))
            if (trailingText != null) {
                Text(trailingText, fontSize = 13.sp, color = HToGoColors.TextSecondary,
                    modifier = Modifier.padding(end = 6.dp))
            }
            if (showChevron) Icon(Icons.Filled.ChevronRight, null, tint = HToGoColors.TextTertiary)
        }
        Divider(color = HToGoColors.OutlineSoft)
    }
}

@Composable
private fun SettingRowSwitch(icon: ImageVector, label: String,
                            checked: Boolean, onCheck: (Boolean) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                .background(HToGoColors.Background), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = HToGoColors.TextPrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(label, fontSize = 14.sp, color = HToGoColors.TextPrimary, modifier = Modifier.weight(1f))
            Switch(
                checked = checked, onCheckedChange = onCheck,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = HToGoColors.Primary,
                    uncheckedTrackColor = HToGoColors.OutlineSoft,
                    uncheckedThumbColor = Color.White
                )
            )
        }
        Divider(color = HToGoColors.OutlineSoft)
    }
}

@Composable
private fun BottomNav() {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Row(Modifier.fillMaxWidth().height(72.dp).padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround) {
            NavItem(Icons.Filled.Home, "Inicio", false)
            NavItem(Icons.Filled.History, "Pedidos", false)
            NavItem(Icons.Filled.Person, "Perfil", true)
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, active: Boolean) {
    Surface(shape = RoundedCornerShape(99.dp),
        color = if (active) HToGoColors.PrimarySoft else Color.Transparent) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null,
                tint = if (active) HToGoColors.Primary else HToGoColors.TextTertiary,
                modifier = Modifier.size(22.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                color = if (active) HToGoColors.Primary else HToGoColors.TextTertiary)
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 900, name = "09 · Perfil Cliente")
@Composable
fun PerfilClienteScreenPreview() {
    HToGoTheme { PerfilClienteScreen() }
}
