package com.htogo.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htogo.app.ui.components.HToGoButton
import com.htogo.app.ui.components.HToGoTextButton
import com.htogo.app.ui.components.HToGoTextField
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme

private enum class RolUsuario { CLIENTE, REPARTIDOR }
private enum class ModoPurificadora { UNIRSE, CREAR }

private data class PurificadoraExistente(
    val id: String,
    val nombre: String,
    val direccion: String,
    val repartidores: Int
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(
    onBack: () -> Unit = {},
    onSubmit: () -> Unit = {},
    onLogin: () -> Unit = {},
    onAvisoPrivacidad: () -> Unit = {}
) {
    var rol by remember { mutableStateOf(RolUsuario.CLIENTE) }
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var aceptaTerminos by remember { mutableStateOf(false) }

    var modoPurif by remember { mutableStateOf(ModoPurificadora.UNIRSE) }
    var purificadoraSeleccionada by remember { mutableStateOf<String?>(null) }
    var busquedaPurif by remember { mutableStateOf("") }

    var nuevoNombreNegocio by remember { mutableStateOf("") }
    var nuevaDireccion by remember { mutableStateOf("") }
    var nuevaColonia by remember { mutableStateOf("") }
    var nuevoCp by remember { mutableStateOf("") }
    var nuevoHorarioApertura by remember { mutableStateOf("08:00") }
    var nuevoHorarioCierre by remember { mutableStateOf("19:00") }
    var nuevoRfc by remember { mutableStateOf("") }

    val purificadoras = remember {
        listOf(
            PurificadoraExistente("p1", "Aguas Del Valle", "Av. Cuauhtémoc 1102, Benito Juárez", 4),
            PurificadoraExistente("p2", "HidroPlus BJ", "Eje 5 Sur 320, Narvarte", 2),
            PurificadoraExistente("p3", "Agua Clara", "Insurgentes Sur 1456, Del Valle", 3),
            PurificadoraExistente("p4", "Purificadora Aqua Pura", "Diagonal San Antonio 980", 5)
        )
    }
    val purificadorasFiltradas = purificadoras.filter {
        busquedaPurif.isBlank() || it.nombre.contains(busquedaPurif, ignoreCase = true)
    }

    val passwordsMatch = password.isNotEmpty() && password == confirm
    val datosBaseOk = nombre.isNotBlank() && correo.contains("@") &&
            telefono.length >= 10 && password.length >= 8 &&
            passwordsMatch && aceptaTerminos

    val datosNegocioOk = when (rol) {
        RolUsuario.CLIENTE -> true
        RolUsuario.REPARTIDOR -> when (modoPurif) {
            ModoPurificadora.UNIRSE -> purificadoraSeleccionada != null
            ModoPurificadora.CREAR -> nuevoNombreNegocio.isNotBlank() &&
                    nuevaDireccion.isNotBlank() &&
                    nuevaColonia.isNotBlank() &&
                    nuevoCp.length == 5
        }
    }

    val canSubmit = datosBaseOk && datosNegocioOk

    Scaffold(
        containerColor = HToGoColors.Background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HToGoColors.Background
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(HToGoColors.Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.WaterDrop, null, tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Crea tu cuenta",
                        style = MaterialTheme.typography.headlineLarge,
                        color = HToGoColors.TextPrimary
                    )
                    Text(
                        "Tarda menos de un minuto",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HToGoColors.TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            SelectorRol(rol = rol, onChange = { rol = it })

            HToGoTextField(
                value = nombre, onValueChange = { nombre = it },
                label = "Nombre completo", leadingIcon = Icons.Filled.Person
            )
            HToGoTextField(
                value = correo, onValueChange = { correo = it },
                label = "Correo electrónico",
                leadingIcon = Icons.Filled.AlternateEmail,
                keyboardType = KeyboardType.Email
            )
            HToGoTextField(
                value = telefono, onValueChange = { telefono = it.filter(Char::isDigit).take(10) },
                label = "Teléfono (10 dígitos)",
                leadingIcon = Icons.Filled.Phone,
                keyboardType = KeyboardType.Phone
            )
            HToGoTextField(
                value = password, onValueChange = { password = it },
                label = "Contraseña",
                leadingIcon = Icons.Filled.Lock,
                isPassword = true,
                supportingText = "Mínimo 8 caracteres"
            )
            HToGoTextField(
                value = confirm, onValueChange = { confirm = it },
                label = "Confirmar contraseña",
                leadingIcon = Icons.Filled.Lock,
                isPassword = true,
                isError = confirm.isNotEmpty() && !passwordsMatch,
                supportingText = if (confirm.isNotEmpty() && !passwordsMatch)
                    "Las contraseñas no coinciden" else null
            )

            if (rol == RolUsuario.REPARTIDOR) {
                BloquePurificadora(
                    modo = modoPurif,
                    onModoChange = { modoPurif = it },
                    purificadoras = purificadorasFiltradas,
                    purificadoraSeleccionada = purificadoraSeleccionada,
                    onSeleccionarPurif = { purificadoraSeleccionada = it },
                    busqueda = busquedaPurif,
                    onBusquedaChange = { busquedaPurif = it },
                    nombreNegocio = nuevoNombreNegocio,
                    onNombreNegocioChange = { nuevoNombreNegocio = it },
                    direccion = nuevaDireccion,
                    onDireccionChange = { nuevaDireccion = it },
                    colonia = nuevaColonia,
                    onColoniaChange = { nuevaColonia = it },
                    cp = nuevoCp,
                    onCpChange = { nuevoCp = it.filter(Char::isDigit).take(5) },
                    horarioApertura = nuevoHorarioApertura,
                    onHorarioAperturaChange = { nuevoHorarioApertura = it },
                    horarioCierre = nuevoHorarioCierre,
                    onHorarioCierreChange = { nuevoHorarioCierre = it },
                    rfc = nuevoRfc,
                    onRfcChange = { nuevoRfc = it.uppercase().take(13) }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = aceptaTerminos,
                    onCheckedChange = { aceptaTerminos = it },
                    colors = CheckboxDefaults.colors(checkedColor = HToGoColors.Primary)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Acepto los Términos y el ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HToGoColors.TextSecondary
                    )
                    Text(
                        "Aviso de Privacidad",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HToGoColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onAvisoPrivacidad)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            HToGoButton(
                text = if (rol == RolUsuario.REPARTIDOR && modoPurif == ModoPurificadora.CREAR)
                    "Registrarme y crear purificadora"
                else "Regístrate",
                onClick = onSubmit,
                enabled = canSubmit
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "¿Ya tienes cuenta?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HToGoColors.TextSecondary
                )
                HToGoTextButton(text = "Iniciar sesión", onClick = onLogin)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SelectorRol(rol: RolUsuario, onChange: (RolUsuario) -> Unit) {
    Column {
        Text(
            "QUIERO REGISTRARME COMO",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = HToGoColors.TextSecondary
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RolCard(
                seleccionado = rol == RolUsuario.CLIENTE,
                titulo = "Cliente",
                subtitulo = "Pedir agua",
                icon = Icons.Filled.Person,
                modifier = Modifier.weight(1f)
            ) { onChange(RolUsuario.CLIENTE) }
            RolCard(
                seleccionado = rol == RolUsuario.REPARTIDOR,
                titulo = "Repartidor",
                subtitulo = "Llevar agua",
                icon = Icons.Filled.LocalShipping,
                modifier = Modifier.weight(1f)
            ) { onChange(RolUsuario.REPARTIDOR) }
        }
    }
}

@Composable
private fun RolCard(
    seleccionado: Boolean,
    titulo: String,
    subtitulo: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (seleccionado) HToGoColors.PrimaryWash else Color.White,
        border = BorderStroke(
            width = if (seleccionado) 2.dp else 1.dp,
            color = if (seleccionado) HToGoColors.Primary else HToGoColors.OutlineSoft
        )
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (seleccionado) HToGoColors.Primary
                        else HToGoColors.PrimarySoft
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, null,
                    tint = if (seleccionado) Color.White else HToGoColors.Primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    titulo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (seleccionado) HToGoColors.PrimaryDark else HToGoColors.TextPrimary
                )
                Text(
                    subtitulo,
                    fontSize = 11.sp,
                    color = HToGoColors.TextSecondary
                )
            }
            if (seleccionado) {
                Icon(
                    Icons.Filled.CheckCircle, null,
                    tint = HToGoColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun BloquePurificadora(
    modo: ModoPurificadora,
    onModoChange: (ModoPurificadora) -> Unit,
    purificadoras: List<PurificadoraExistente>,
    purificadoraSeleccionada: String?,
    onSeleccionarPurif: (String) -> Unit,
    busqueda: String,
    onBusquedaChange: (String) -> Unit,
    nombreNegocio: String,
    onNombreNegocioChange: (String) -> Unit,
    direccion: String,
    onDireccionChange: (String) -> Unit,
    colonia: String,
    onColoniaChange: (String) -> Unit,
    cp: String,
    onCpChange: (String) -> Unit,
    horarioApertura: String,
    onHorarioAperturaChange: (String) -> Unit,
    horarioCierre: String,
    onHorarioCierreChange: (String) -> Unit,
    rfc: String,
    onRfcChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HToGoColors.OutlineSoft),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(HToGoColors.PrimarySoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Storefront, null, tint = HToGoColors.Primary)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Tu purificadora",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Cada repartidor pertenece a una purificadora",
                        fontSize = 11.sp,
                        color = HToGoColors.TextSecondary
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ModoPurifOption(
                    seleccionado = modo == ModoPurificadora.UNIRSE,
                    titulo = "Pertenezco a una purificadora existente",
                    subtitulo = "Mi jefe ya tiene cuenta en HToGo",
                    icon = Icons.Filled.Apartment
                ) { onModoChange(ModoPurificadora.UNIRSE) }
                ModoPurifOption(
                    seleccionado = modo == ModoPurificadora.CREAR,
                    titulo = "Quiero crear una nueva purificadora",
                    subtitulo = "Soy dueño / responsable del negocio",
                    icon = Icons.Filled.Storefront
                ) { onModoChange(ModoPurificadora.CREAR) }
            }
            Spacer(Modifier.height(14.dp))

            when (modo) {
                ModoPurificadora.UNIRSE -> {
                    OutlinedTextField(
                        value = busqueda,
                        onValueChange = onBusquedaChange,
                        label = { Text("Buscar purificadora") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    if (purificadoras.isEmpty()) {
                        Text(
                            "No encontramos purificadoras con ese nombre. Pídele al dueño que te invite o crea una nueva.",
                            fontSize = 12.sp,
                            color = HToGoColors.TextTertiary,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            purificadoras.forEach { p ->
                                PurificadoraOptionRow(
                                    seleccionada = p.id == purificadoraSeleccionada,
                                    purificadora = p
                                ) { onSeleccionarPurif(p.id) }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = HToGoColors.PrimaryWash,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle, null,
                                tint = HToGoColors.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "El dueño debe aprobar tu solicitud antes de que puedas tomar pedidos.",
                                fontSize = 11.sp,
                                color = HToGoColors.PrimaryDark
                            )
                        }
                    }
                }
                ModoPurificadora.CREAR -> {
                    Text(
                        "DATOS DEL NEGOCIO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HToGoColors.TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    HToGoTextField(
                        value = nombreNegocio,
                        onValueChange = onNombreNegocioChange,
                        label = "Nombre comercial de la purificadora",
                        leadingIcon = Icons.Filled.Storefront
                    )
                    Spacer(Modifier.height(8.dp))
                    HToGoTextField(
                        value = direccion,
                        onValueChange = onDireccionChange,
                        label = "Calle y número de la base",
                        leadingIcon = Icons.Filled.LocationOn
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HToGoTextField(
                            value = colonia,
                            onValueChange = onColoniaChange,
                            label = "Colonia",
                            modifier = Modifier.weight(1.4f)
                        )
                        HToGoTextField(
                            value = cp,
                            onValueChange = onCpChange,
                            label = "C.P.",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "HORARIO DE ATENCIÓN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HToGoColors.TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HToGoTextField(
                            value = horarioApertura,
                            onValueChange = onHorarioAperturaChange,
                            label = "Apertura",
                            leadingIcon = Icons.Filled.Schedule,
                            modifier = Modifier.weight(1f)
                        )
                        HToGoTextField(
                            value = horarioCierre,
                            onValueChange = onHorarioCierreChange,
                            label = "Cierre",
                            leadingIcon = Icons.Filled.Schedule,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    HToGoTextField(
                        value = rfc,
                        onValueChange = onRfcChange,
                        label = "RFC del negocio (opcional)",
                        supportingText = "Para emisión de facturas a clientes"
                    )
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = HToGoColors.AccentAmber.copy(alpha = .12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle, null,
                                tint = HToGoColors.AccentAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Quedarás registrado como dueño. Después podrás invitar a más repartidores y configurar precios y vehículos.",
                                fontSize = 11.sp,
                                color = HToGoColors.TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModoPurifOption(
    seleccionado: Boolean,
    titulo: String,
    subtitulo: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (seleccionado) 2.dp else 1.dp,
                color = if (seleccionado) HToGoColors.Primary else HToGoColors.OutlineSoft,
                shape = RoundedCornerShape(12.dp)
            )
            .background(if (seleccionado) HToGoColors.PrimaryWash else Color.White)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(
                    2.dp,
                    if (seleccionado) HToGoColors.Primary else HToGoColors.OutlineSoft,
                    CircleShape
                )
                .background(if (seleccionado) HToGoColors.Primary else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (seleccionado) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Icon(
            icon, null,
            tint = if (seleccionado) HToGoColors.Primary else HToGoColors.TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                titulo,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (seleccionado) HToGoColors.PrimaryDark else HToGoColors.TextPrimary
            )
            Text(
                subtitulo,
                fontSize = 11.sp,
                color = HToGoColors.TextSecondary
            )
        }
    }
}

@Composable
private fun PurificadoraOptionRow(
    seleccionada: Boolean,
    purificadora: PurificadoraExistente,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (seleccionada) 1.5.dp else 1.dp,
                color = if (seleccionada) HToGoColors.Primary else HToGoColors.OutlineSoft,
                shape = RoundedCornerShape(10.dp)
            )
            .background(if (seleccionada) HToGoColors.PrimaryWash else Color.White)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(HToGoColors.PrimarySoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Storefront, null, tint = HToGoColors.Primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                purificadora.nombre,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = HToGoColors.TextPrimary
            )
            Text(
                "${purificadora.direccion} · ${purificadora.repartidores} repartidores",
                fontSize = 11.sp,
                color = HToGoColors.TextSecondary,
                maxLines = 1
            )
        }
        if (seleccionada) {
            Icon(
                Icons.Filled.CheckCircle, null,
                tint = HToGoColors.Primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun RegistroScreenPreview() {
    HToGoTheme { RegistroScreen() }
}
