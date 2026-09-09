package com.htogo.ui.screens.cliente

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

/* --------------------------- Theme tokens ----------------------------- */

private object HtgColor {
    val Primary     = Color(0xFF0077B6)
    val PrimaryLight= Color(0xFF00B4D8)
    val PrimaryDark = Color(0xFF03045E)
    val PrimarySoft = Color(0xFFCAF0F8)
    val Bg          = Color(0xFFF8FAFC)
    val Surface     = Color(0xFFFFFFFF)
    val Text        = Color(0xFF0F172A)
    val Text2       = Color(0xFF64748B)
    val Text3       = Color(0xFF94A3B8)
    val Outline     = Color(0xFFE2E8F0)
    val Danger      = Color(0xFFEF4444)
    val Success     = Color(0xFF10B981)
}

/* ------------------------------- Models -------------------------------- */

data class UserProfile(
    val initials: String,
    val nombre: String,
    val nombreCompleto: String,
    val telefono: String,
    val email: String,
    val nacimiento: String,
    val verificado: Boolean = true,
    val totalPedidos: Int = 14,
    val totalGastado: String = "$1,892",
    val ratingPromedio: String = "4.9 ★"
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

/* ------------------------------- Screen -------------------------------- */

@Composable
fun PerfilScreen(
    user: UserProfile = SAMPLE_USER,
    direcciones: List<Direccion> = SAMPLE_DIRECCIONES,
    onBack: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onAddAddress: () -> Unit = {},
    onEditAddress: (String) -> Unit = {},
    onDeleteAddress: (String) -> Unit = {},
    onSetDefault: (String) -> Unit = {},
    onLogout: () -> Unit = {},
    onDeleteAccount: () -> Unit = {}
) {
    var notif by remember { mutableStateOf(true) }

    Scaffold(
        bottomBar = { BottomNav() },
        containerColor = HtgColor.Bg
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { ProfileHeader(user, onBack, onEditProfile) }
            item { StatsRow(user) }

            item { SectionTitle("Datos personales") }
            item { DataList(user) }

            item { SectionTitle("Mis direcciones · ${direcciones.size}") }
            items(direcciones) { d ->
                AddressCard(
                    direccion = d,
                    onSetDefault = { onSetDefault(d.id) },
                    onEdit = { onEditAddress(d.id) },
                    onDelete = { onDeleteAddress(d.id) }
                )
            }
            item { AddAddressButton(onAddAddress) }

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
                        danger = true, onClick = onDeleteAccount, showChevron = false)
                }
            }

            item {
                Text("HToGo v1.0.0 · Build 142",
                    fontSize = 11.sp, color = HtgColor.Text3,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

/* ----------------------------- Header --------------------------------- */

@Composable
private fun ProfileHeader(user: UserProfile, onBack: () -> Unit, onEdit: () -> Unit) {
    Box {
        // top bar dark area
        Surface(color = HtgColor.PrimaryDark, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 32.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeaderIconBtn(Icons.Filled.ArrowBack, onClick = onBack)
                    Text("Mi perfil",
                        Modifier.weight(1f),
                        fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    HeaderIconBtn(Icons.Filled.Settings, onClick = {})
                }
            }
        }
        // floating profile card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = HtgColor.Surface,
            shadowElevation = 4.dp,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .offset(y = (-20).dp)
                .fillMaxWidth()
        ) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(HtgColor.PrimaryLight, HtgColor.Primary)))
                        .border(3.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(user.initials, color = Color.White,
                        fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(user.nombre, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(user.telefono, fontSize = 13.sp, color = HtgColor.Text2,
                        modifier = Modifier.padding(top = 2.dp))
                    if (user.verificado) {
                        Spacer(Modifier.height(6.dp))
                        Surface(shape = RoundedCornerShape(99.dp),
                            color = HtgColor.Success.copy(alpha = 0.10f)) {
                            Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Verified, null,
                                    tint = HtgColor.Success, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Verificado", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                    color = HtgColor.Success)
                            }
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = HtgColor.PrimarySoft,
                    modifier = Modifier.size(36.dp).clickable(onClick = onEdit)
                ) { Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Edit, null, tint = HtgColor.Primary,
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

/* ------------------------------ Stats --------------------------------- */

@Composable
private fun StatsRow(user: UserProfile) {
    // Push up stats since profile card is offset
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = HtgColor.Outline,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .offset(y = (-32).dp)
            .border(1.dp, HtgColor.Outline, RoundedCornerShape(14.dp))
    ) {
        Row(Modifier.fillMaxWidth()) {
            StatCell("${user.totalPedidos}", "Pedidos", Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(56.dp).background(HtgColor.Outline))
            StatCell(user.ratingPromedio, "Promedio", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCell(num: String, lbl: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Color.White).padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(num, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HtgColor.Text)
        Text(lbl, fontSize = 11.sp, color = HtgColor.Text2,
            modifier = Modifier.padding(top = 2.dp))
    }
}

/* --------------------------- Section title ---------------------------- */

@Composable
private fun SectionTitle(text: String) {
    Text(text.uppercase(),
        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        color = HtgColor.Text2, letterSpacing = 0.4.sp,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 8.dp))
}

/* ----------------------------- Data list ------------------------------ */

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
            .background(HtgColor.PrimarySoft), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = HtgColor.Primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(lbl, fontSize = 11.sp, color = HtgColor.Text2)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = HtgColor.Text,
                modifier = Modifier.padding(top = 1.dp))
        }
        Icon(Icons.Filled.ChevronRight, null, tint = HtgColor.Text3)
    }
}

@Composable
private fun DividerRow() = Divider(color = HtgColor.Outline)

/* ---------------------------- Address card ---------------------------- */

@Composable
private fun AddressCard(
    direccion: Direccion,
    onSetDefault: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(
            if (direccion.predeterminada) 2.dp else 1.dp,
            if (direccion.predeterminada) HtgColor.Primary else HtgColor.Outline
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                    .background(HtgColor.PrimarySoft), contentAlignment = Alignment.Center) {
                    Icon(direccion.icon, null, tint = HtgColor.Primary)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(direccion.nombre, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = HtgColor.Text)
                        if (direccion.predeterminada) {
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(99.dp),
                                color = HtgColor.Primary) {
                                Text("PREDETERMINADA",
                                    Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                                    color = Color.White, letterSpacing = 0.3.sp)
                            }
                        }
                    }
                    Text("${direccion.linea1}\n${direccion.linea2}",
                        fontSize = 12.sp, color = HtgColor.Text2,
                        modifier = Modifier.padding(top = 3.dp))
                    Text("${direccion.ciudad} · Ref: ${direccion.referencia}",
                        fontSize = 11.sp, color = HtgColor.Text3,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
            Divider(Modifier.padding(top = 10.dp, bottom = 4.dp), color = HtgColor.Outline)
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                if (!direccion.predeterminada) {
                    AddrAction("Predeterminar", Icons.Outlined.StarOutline,
                        HtgColor.Primary, Modifier.weight(1f), onSetDefault)
                }
                AddrAction("Editar", Icons.Filled.Edit,
                    HtgColor.Text2, Modifier.weight(1f), onEdit)
                AddrAction("Eliminar", Icons.Outlined.DeleteOutline,
                    HtgColor.Danger, Modifier.weight(1f), onDelete)
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
        border = androidx.compose.foundation.BorderStroke(1.5.dp, HtgColor.Primary),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AddLocationAlt, null, tint = HtgColor.Primary)
            Spacer(Modifier.width(8.dp))
            Text("Agregar nueva dirección",
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = HtgColor.Primary)
        }
    }
}

/* ---------------------------- Settings -------------------------------- */

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
                .background(if (danger) HtgColor.Danger.copy(alpha = 0.08f) else HtgColor.Bg),
                contentAlignment = Alignment.Center) {
                Icon(icon, null,
                    tint = if (danger) HtgColor.Danger else HtgColor.Text,
                    modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(label, fontSize = 14.sp,
                color = if (danger) HtgColor.Danger else HtgColor.Text,
                modifier = Modifier.weight(1f))
            if (trailingText != null) {
                Text(trailingText, fontSize = 13.sp, color = HtgColor.Text2,
                    modifier = Modifier.padding(end = 6.dp))
            }
            if (showChevron) Icon(Icons.Filled.ChevronRight, null, tint = HtgColor.Text3)
        }
        Divider(color = HtgColor.Outline)
    }
}

@Composable
private fun SettingRowSwitch(icon: ImageVector, label: String,
                            checked: Boolean, onCheck: (Boolean) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                .background(HtgColor.Bg), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = HtgColor.Text, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(label, fontSize = 14.sp, color = HtgColor.Text, modifier = Modifier.weight(1f))
            Switch(
                checked = checked, onCheckedChange = onCheck,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = HtgColor.Primary,
                    uncheckedTrackColor = HtgColor.Outline,
                    uncheckedThumbColor = Color.White
                )
            )
        }
        Divider(color = HtgColor.Outline)
    }
}

/* --------------------------- Bottom nav -------------------------------- */

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
        color = if (active) HtgColor.PrimarySoft else Color.Transparent) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null,
                tint = if (active) HtgColor.Primary else HtgColor.Text3,
                modifier = Modifier.size(22.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                color = if (active) HtgColor.Primary else HtgColor.Text3)
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 900, name = "09 · Perfil")
@Composable
fun PerfilScreenPreview() {
    MaterialTheme { PerfilScreen() }
}
