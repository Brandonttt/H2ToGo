package com.htogo.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htogo.app.data.dto.DireccionNegocioDto
import com.htogo.app.data.dto.SolicitudResponse
import com.htogo.app.ui.components.RepartidorBottomBar
import com.htogo.app.ui.components.RepartidorTab
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme
import org.json.JSONObject

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
    onIngresos: () -> Unit = {},
    onHistorial: () -> Unit = {},
    repartidorViewModel: com.htogo.app.ui.RepartidorViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { com.htogo.app.data.local.SessionManager.getInstance(context) }

    val liveMiNegocio by repartidorViewModel.miNegocio.collectAsState()
    val totalEntregas by repartidorViewModel.totalEntregas.collectAsState()
    val solicitudes by repartidorViewModel.solicitudes.collectAsState()

    var mostrarDialogLogout by remember { mutableStateOf(false) }
    var showModalCambioNombre by remember { mutableStateOf(false) }
    var showModalCambioDireccion by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repartidorViewModel.cargarMiNegocio()
        repartidorViewModel.cargarMisEntregas()
        repartidorViewModel.cargarSolicitudes()
    }

    val realNombre = sessionManager.obtenerNombre() ?: "Repartidor"
    val realCorreo = sessionManager.obtenerCorreo() ?: "repartidor@h2togo.mx"
    val realTelefono = sessionManager.obtenerTelefono() ?: ""
    val realNegocio = liveMiNegocio?.nombreComercial ?: sessionManager.obtenerNombreNegocio() ?: "Mi Purificadora"
    val direccionBaseTexto = liveMiNegocio?.direccion ?: "Dirección no registrada"

    // Solicitudes pendientes CU-020
    val solicitudPendienteNombre = remember(solicitudes) {
        solicitudes.firstOrNull {
            it.estado.equals("pendiente", ignoreCase = true) && it.codigoCambio == "NOMBRE_NEGOCIO"
        }
    }
    val nombrePropuesto = remember(solicitudPendienteNombre) {
        solicitudPendienteNombre?.valorNuevo?.let { raw ->
            try {
                JSONObject(raw).optString("nombreComercial").ifBlank { null }
            } catch (_: Exception) {
                raw
            }
        }
    }

    val solicitudPendienteDireccion = remember(solicitudes) {
        solicitudes.firstOrNull {
            it.estado.equals("pendiente", ignoreCase = true) && it.codigoCambio == "DIRECCION_BASE"
        }
    }
    val direccionPropuesta = remember(solicitudPendienteDireccion) {
        solicitudPendienteDireccion?.valorNuevo?.let { raw ->
            try {
                val json = JSONObject(raw)
                val c = json.optString("calle")
                val ne = json.optString("numeroExterior")
                val col = json.optString("colonia")
                val cp = json.optString("codigoPostal")
                listOf(c, ne, col, if (cp.isNotBlank()) "CP $cp" else "").filter { it.isNotBlank() }.joinToString(", ")
            } catch (_: Exception) {
                "Nueva dirección en revisión"
            }
        }
    }

    val iniciales = remember(realNombre) {
        val parts = realNombre.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (parts.size >= 2) {
            "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
        } else {
            realNombre.take(2).uppercase().ifBlank { "R" }
        }
    }

    val usuario = PerfilUsuario(
        nombre = realNombre,
        iniciales = iniciales,
        nombreNegocio = realNegocio,
        telefono = if (realTelefono.isNotBlank()) realTelefono else "No registrado",
        correo = realCorreo,
        totalEntregas = totalEntregas,
        antiguedadMeses = 1
    )

    var enLinea by remember { mutableStateOf(true) }
    var notifPush by remember { mutableStateOf(sessionManager.estanNotificacionesActivas()) }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            Brush.verticalGradient(listOf(HToGoColors.PrimaryDark, HToGoColors.Primary))
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                        }
                        Text(
                            "Mi perfil",
                            color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f).padding(start = 4.dp)
                        )
                        IconButton(onClick = {
                            val nuevoEstado = !notifPush
                            notifPush = nuevoEstado
                            sessionManager.guardarNotificacionesActivas(nuevoEstado)
                            Toast.makeText(
                                context,
                                if (nuevoEstado) "Notificaciones activadas" else "Notificaciones desactivadas",
                                Toast.LENGTH_SHORT
                            ).show()
                        }) {
                            Icon(
                                if (notifPush) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                                contentDescription = if (notifPush) "Desactivar notificaciones" else "Activar notificaciones",
                                tint = if (notifPush) Color.White else Color.White.copy(alpha = 0.6f)
                            )
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
                    onHistorial = onHistorial,
                    modifier = Modifier
                        .padding(horizontal = 18.dp)
                        .offset(y = (-42).dp)
                )
            }

            // SECCIÓN: MI CUENTA (CU-020)
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
                        InfoRow(Icons.Filled.MailOutline, "Correo", usuario.correo)
                        // CU-020: Nombre comercial con solicitud al administrador
                        EditableInfoRow(
                            icon = Icons.Filled.Storefront,
                            label = "Purificadora (Nombre comercial)",
                            valor = usuario.nombreNegocio,
                            pendienteTexto = if (solicitudPendienteNombre != null) "Cambio pendiente: ${nombrePropuesto ?: "En revisión"}" else null,
                            onClick = { showModalCambioNombre = true }
                        )
                        // CU-020: Dirección de la base con solicitud al administrador
                        EditableInfoRow(
                            icon = Icons.Filled.LocationOn,
                            label = "Dirección de la base",
                            valor = direccionBaseTexto,
                            pendienteTexto = if (solicitudPendienteDireccion != null) "Cambio pendiente: ${direccionPropuesta ?: "En revisión"}" else null,
                            last = true,
                            onClick = { showModalCambioDireccion = true }
                        )
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
                            "Nuevos pedidos y mensajes", notifPush) {
                            notifPush = it
                            sessionManager.guardarNotificacionesActivas(it)
                            Toast.makeText(
                                context,
                                if (it) "Notificaciones activadas" else "Notificaciones desactivadas",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
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
                        ActionRow(Icons.Filled.History, "Mi historial de entregas", redText = false, onClick = onHistorial)
                        ActionRow(Icons.Outlined.Lock, "Cambiar contraseña", redText = false, onClick = {})
                        ActionRow(Icons.Filled.Logout, "Cerrar sesión", redText = true, isLast = true, onClick = { mostrarDialogLogout = true })
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

        // Dialog Logout
        if (mostrarDialogLogout) {
            AlertDialog(
                onDismissRequest = { mostrarDialogLogout = false },
                icon = { Icon(Icons.Filled.Logout, null, tint = MaterialTheme.colorScheme.error) },
                title = {
                    Text("¿Cerrar sesión?", fontWeight = FontWeight.Bold, color = HToGoColors.TextPrimary)
                },
                text = {
                    Text(
                        "¿Estás seguro de que deseas cerrar tu sesión? Tendrás que volver a ingresar tus credenciales para acceder.",
                        color = HToGoColors.TextSecondary,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            mostrarDialogLogout = false
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cerrar sesión", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarDialogLogout = false }) {
                        Text("Cancelar", color = HToGoColors.TextSecondary)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = Color.White
            )
        }

        // Modal CU-020: Cambiar nombre comercial
        if (showModalCambioNombre) {
            DialogCambioNombreComercial(
                nombreActual = realNegocio,
                solicitudPendiente = solicitudPendienteNombre,
                nombrePropuesto = nombrePropuesto,
                onDismiss = { showModalCambioNombre = false },
                onEnviar = { nuevoNombre ->
                    repartidorViewModel.solicitarCambioNombreNegocio(
                        nuevoNombre = nuevoNombre,
                        onSuccess = {
                            showModalCambioNombre = false
                            Toast.makeText(
                                context,
                                "Solicitud enviada al administrador (Pendiente de aprobación)",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        onError = { err ->
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }

        // Modal CU-020: Cambiar dirección de la base
        if (showModalCambioDireccion) {
            ModalCambioDireccionBase(
                direccionActual = liveMiNegocio?.direccionObj,
                direccionActualTexto = direccionBaseTexto,
                solicitudPendiente = solicitudPendienteDireccion,
                direccionPropuestaTexto = direccionPropuesta,
                onDismiss = { showModalCambioDireccion = false },
                onEnviar = { calle, numExt, numInt, col, cp, ref ->
                    repartidorViewModel.solicitarCambioDireccionBase(
                        calle = calle,
                        numeroExterior = numExt,
                        numeroInterior = numInt,
                        colonia = col,
                        codigoPostal = cp,
                        referencias = ref,
                        onSuccess = {
                            showModalCambioDireccion = false
                            Toast.makeText(
                                context,
                                "Solicitud de cambio de dirección enviada al administrador",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        onError = { err ->
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun ProfileCard(
    u: PerfilUsuario,
    enLinea: Boolean,
    onToggleLinea: () -> Unit,
    onHistorial: () -> Unit = {},
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
                MiniProfileStat("${u.totalEntregas}", "Entregas", onClick = onHistorial)
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
private fun MiniProfileStat(v: String, l: String, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ) {
        Text(v, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (onClick != null) HToGoColors.Primary else HToGoColors.TextPrimary)
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
            Text(valor, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    if (!last) HorizontalDivider(color = HToGoColors.OutlineSoft)
}

/**
 * Fila editable con soporte para indicador de solicitud pendiente (CU-020).
 */
@Composable
private fun EditableInfoRow(
    icon: ImageVector,
    label: String,
    valor: String,
    pendienteTexto: String? = null,
    last: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 11.sp, color = HToGoColors.TextSecondary)
                if (pendienteTexto != null) {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = HToGoColors.AccentAmber.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "PENDIENTE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = HToGoColors.AccentAmber,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            Text(valor, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (pendienteTexto != null) {
                Text(
                    pendienteTexto,
                    fontSize = 11.sp,
                    color = HToGoColors.AccentAmber,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(HToGoColors.Background),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = "Solicitar cambio",
                tint = HToGoColors.Primary,
                modifier = Modifier.size(15.dp)
            )
        }
    }
    if (!last) HorizontalDivider(color = HToGoColors.OutlineSoft)
}

/**
 * Diálogo CU-020 para solicitar cambio de nombre comercial con valor actual y valor nuevo.
 * Valida formato y controla Flujo Alterno S1 si ya existe una solicitud pendiente.
 */
@Composable
private fun DialogCambioNombreComercial(
    nombreActual: String,
    solicitudPendiente: SolicitudResponse?,
    nombrePropuesto: String?,
    onDismiss: () -> Unit,
    onEnviar: (String) -> Unit
) {
    if (solicitudPendiente != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Filled.HourglassEmpty, null, tint = HToGoColors.StatusAsignado) },
            title = { Text("Solicitud en revisión", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Ya existe una solicitud pendiente para cambiar el nombre comercial de tu negocio (RN-019).",
                        color = HToGoColors.TextSecondary,
                        fontSize = 14.sp
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = HToGoColors.Background),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text("Nombre solicitado:", fontSize = 11.sp, color = HToGoColors.TextSecondary)
                            Text(nombrePropuesto ?: "En revisión", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HToGoColors.TextPrimary)
                        }
                    }
                    Text(
                        "No se puede registrar otra solicitud hasta que el administrador resuelva la actual (Flujo alterno S1).",
                        color = HToGoColors.TextTertiary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Entendido", color = Color.White)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    } else {
        var nuevoNombre by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        var enviando by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!enviando) onDismiss() },
            icon = { Icon(Icons.Filled.Storefront, null, tint = HToGoColors.Primary) },
            title = { Text("Cambiar nombre comercial", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Ingresa el nuevo nombre comercial. La solicitud quedará pendiente hasta ser aprobada por el administrador (RN-019).",
                        fontSize = 13.sp,
                        color = HToGoColors.TextSecondary
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = HToGoColors.Background),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text("Nombre actual:", fontSize = 11.sp, color = HToGoColors.TextSecondary)
                            Text(nombreActual, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = HToGoColors.TextPrimary)
                        }
                    }
                    OutlinedTextField(
                        value = nuevoNombre,
                        onValueChange = {
                            nuevoNombre = it
                            error = null
                        },
                        label = { Text("Nuevo nombre comercial *") },
                        placeholder = { Text("Ej. Purificadora San José") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = error != null,
                        supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val t = nuevoNombre.trim()
                        if (t.isBlank()) {
                            error = "El nombre comercial no puede estar vacío"
                        } else if (t.equals(nombreActual.trim(), ignoreCase = true)) {
                            error = "El nuevo nombre debe ser diferente al actual"
                        } else if (t.length > 150) {
                            error = "El nombre no puede exceder 150 caracteres"
                        } else {
                            enviando = true
                            onEnviar(t)
                        }
                    },
                    enabled = !enviando,
                    colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (enviando) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Enviar solicitud", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss, enabled = !enviando) {
                    Text("Cancelar", color = HToGoColors.TextSecondary)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * Modal BottomSheet CU-020 para solicitar cambio de dirección de la base.
 * Muestra dirección actual, formulario con nuevo valor y controla Flujo Alterno S1.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModalCambioDireccionBase(
    direccionActual: DireccionNegocioDto?,
    direccionActualTexto: String,
    solicitudPendiente: SolicitudResponse?,
    direccionPropuestaTexto: String?,
    onDismiss: () -> Unit,
    onEnviar: (calle: String, numExt: String, numInt: String?, col: String, cp: String, ref: String?) -> Unit
) {
    if (solicitudPendiente != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Filled.HourglassEmpty, null, tint = HToGoColors.StatusAsignado) },
            title = { Text("Solicitud en revisión", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Ya existe una solicitud pendiente para cambiar la dirección de la base de tu negocio (RN-019).",
                        color = HToGoColors.TextSecondary,
                        fontSize = 14.sp
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = HToGoColors.Background),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text("Dirección solicitada:", fontSize = 11.sp, color = HToGoColors.TextSecondary)
                            Text(direccionPropuestaTexto ?: "En revisión", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HToGoColors.TextPrimary)
                        }
                    }
                    Text(
                        "No se puede registrar otra solicitud hasta que el administrador resuelva la actual (Flujo alterno S1).",
                        color = HToGoColors.TextTertiary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Entendido", color = Color.White)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    } else {
        var calle by remember { mutableStateOf(direccionActual?.calle ?: "") }
        var numExt by remember { mutableStateOf(direccionActual?.numeroExterior ?: "") }
        var numInt by remember { mutableStateOf(direccionActual?.numeroInterior ?: "") }
        var colonia by remember { mutableStateOf(direccionActual?.colonia ?: "") }
        var cp by remember { mutableStateOf(direccionActual?.codigoPostal ?: "") }
        var referencias by remember { mutableStateOf(direccionActual?.referencias ?: "") }
        var error by remember { mutableStateOf<String?>(null) }
        var enviando by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { if (!enviando) onDismiss() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Cambiar dirección de la base", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HToGoColors.TextPrimary)
                        Text("Solicitud de cambio (CU-020)", fontSize = 12.sp, color = HToGoColors.TextSecondary)
                    }
                    IconButton(onClick = onDismiss, enabled = !enviando) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = HToGoColors.TextSecondary)
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = HToGoColors.Background),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text("Dirección actual registrada:", fontSize = 11.sp, color = HToGoColors.TextSecondary)
                        Text(direccionActualTexto, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = HToGoColors.TextPrimary)
                    }
                }

                Text(
                    "Esta solicitud quedará en estado pendiente hasta ser aprobada por el administrador (RN-019).",
                    fontSize = 12.sp,
                    color = HToGoColors.TextSecondary
                )

                OutlinedTextField(
                    value = calle,
                    onValueChange = { calle = it; error = null },
                    label = { Text("Calle *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = numExt,
                        onValueChange = { numExt = it; error = null },
                        label = { Text("Núm. ext. *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = numInt,
                        onValueChange = { numInt = it },
                        label = { Text("Núm. int.") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = colonia,
                    onValueChange = { colonia = it; error = null },
                    label = { Text("Colonia *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = cp,
                    onValueChange = { cp = it; error = null },
                    label = { Text("Código postal *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = referencias,
                    onValueChange = { referencias = it },
                    label = { Text("Referencias (opcional)") },
                    placeholder = { Text("Ej. Entre calle 5 y 7, frente al parque") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, enabled = !enviando) {
                        Text("Cancelar", color = HToGoColors.TextSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (calle.isBlank() || numExt.isBlank() || colonia.isBlank() || cp.isBlank()) {
                                error = "Por favor completa todos los campos obligatorios (*)"
                            } else {
                                enviando = true
                                onEnviar(
                                    calle.trim(),
                                    numExt.trim(),
                                    numInt.trim().ifBlank { null },
                                    colonia.trim(),
                                    cp.trim(),
                                    referencias.trim().ifBlank { null }
                                )
                            }
                        },
                        enabled = !enviando,
                        colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (enviando) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Enviar solicitud", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
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
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
