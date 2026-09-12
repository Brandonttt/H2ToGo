package com.htogo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.htogo.app.data.api.GeocodingHelper
import com.htogo.app.data.dto.DireccionResponse
import com.htogo.app.ui.ClienteViewModel
import com.htogo.app.ui.theme.HToGoColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AgregarDireccionDialog(
    onDismiss: () -> Unit,
    onDireccionCreada: (DireccionResponse) -> Unit,
    clienteViewModel: ClienteViewModel,
    initialLat: Double? = null,
    initialLon: Double? = null,
    initialCalle: String? = null,
    initialColonia: String? = null
) {
    // Benito Juárez por defecto o recibido
    var lat by remember { mutableStateOf(initialLat ?: 19.376692) }
    var lon by remember { mutableStateOf(initialLon ?: -99.165057) }

    var alias by remember { mutableStateOf("Casa") }
    var calle by remember { mutableStateOf(initialCalle ?: "") }
    var numeroExterior by remember { mutableStateOf("") }
    var numeroInterior by remember { mutableStateOf("") }
    var colonia by remember { mutableStateOf(initialColonia ?: "Del Valle") }
    var codigoPostal by remember { mutableStateOf("03100") }
    var referencias by remember { mutableStateOf("") }

    var localError by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }

    var isGeocoding by remember { mutableStateOf(false) }
    var geocodeMessage by remember { mutableStateOf<String?>(null) }
    var isReverseGeocoding by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(calle, numeroExterior, colonia) {
        if (isReverseGeocoding) return@LaunchedEffect
        if (calle.trim().length >= 3 && numeroExterior.isNotBlank()) {
            delay(1000)
            isGeocoding = true
            geocodeMessage = "Buscando en mapa..."
            val query = "${calle.trim()} ${numeroExterior.trim()}, ${colonia.trim()}"
            val result = GeocodingHelper.buscarCoordenadas(query)
            if (result != null) {
                lat = result.lat
                lon = result.lon
                geocodeMessage = "✓ Ubicación colocada en el mapa"
                if (!result.postcode.isNullOrBlank() && (codigoPostal.isBlank() || codigoPostal == "03100")) {
                    codigoPostal = result.postcode
                }
            } else {
                geocodeMessage = null
            }
            isGeocoding = false
        }
    }

    fun ubicarEnMapaManual() {
        if (calle.isBlank()) {
            localError = "Ingresa la calle para ubicar en el mapa"
            return
        }
        coroutineScope.launch {
            isGeocoding = true
            geocodeMessage = "Buscando dirección..."
            val query = "${calle.trim()} ${numeroExterior.trim()}, ${colonia.trim()}"
            val result = GeocodingHelper.buscarCoordenadas(query)
            if (result != null) {
                lat = result.lat
                lon = result.lon
                geocodeMessage = "✓ Pin ubicado: ${result.displayName.take(45)}..."
                if (!result.postcode.isNullOrBlank()) {
                    codigoPostal = result.postcode
                }
            } else {
                geocodeMessage = "No se localizó exactamente. Puedes mover el pin en el mapa."
            }
            isGeocoding = false
        }
    }

    Dialog(
        onDismissRequest = { if (!guardando) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(24.dp),
            color = HToGoColors.Surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Nueva dirección",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = HToGoColors.TextPrimary
                        )
                        Text(
                            "Benito Juárez, CDMX",
                            fontSize = 12.sp,
                            color = HToGoColors.TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !guardando
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = HToGoColors.TextSecondary)
                    }
                }

                Divider(color = HToGoColors.OutlineSoft)

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(14.dp))

                    if (localError != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    localError!!,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Alias Chips
                    Text(
                        "Alias del lugar",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HToGoColors.TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Casa", "Oficina", "Depto", "Otro").forEach { tag ->
                            val selected = alias.equals(tag, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (selected) HToGoColors.Primary else HToGoColors.Background,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selected) HToGoColors.Primary else HToGoColors.OutlineSoft
                                ),
                                modifier = Modifier.clickable { alias = tag }
                            ) {
                                Text(
                                    tag,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Color.White else HToGoColors.TextPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Calle y Número
                    OutlinedTextField(
                        value = calle,
                        onValueChange = { calle = it; localError = null },
                        label = { Text("Calle o Avenida *") },
                        placeholder = { Text("Ej. Insurgentes Sur") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = numeroExterior,
                            onValueChange = { numeroExterior = it; localError = null },
                            label = { Text("No. Ext *") },
                            placeholder = { Text("1234") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = numeroInterior,
                            onValueChange = { numeroInterior = it },
                            label = { Text("No. Int (opc)") },
                            placeholder = { Text("4B") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = colonia,
                            onValueChange = { colonia = it; localError = null },
                            label = { Text("Colonia *") },
                            placeholder = { Text("Del Valle") },
                            singleLine = true,
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = codigoPostal,
                            onValueChange = { codigoPostal = it; localError = null },
                            label = { Text("C.P. *") },
                            placeholder = { Text("03100") },
                            singleLine = true,
                            modifier = Modifier.weight(0.9f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = referencias,
                        onValueChange = { referencias = it },
                        label = { Text("Referencias / Entre calles *") },
                        placeholder = { Text("Ej. Portón blanco, timbre junto a la puerta") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(14.dp))

                    // Ubicación en el mapa OSM
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocationOn, null, tint = HToGoColors.Primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Ubicación en el mapa",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HToGoColors.TextPrimary
                            )
                        }
                        TextButton(
                            onClick = { ubicarEnMapaManual() },
                            enabled = !isGeocoding && calle.isNotBlank(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Filled.Search, null, modifier = Modifier.size(14.dp), tint = HToGoColors.Primary)
                            Spacer(Modifier.width(4.dp))
                            Text("Ubicar en mapa", fontSize = 11.sp, color = HToGoColors.Primary, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (isGeocoding || geocodeMessage != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            if (isGeocoding) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = HToGoColors.Primary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Buscando en mapa...", fontSize = 11.sp, color = HToGoColors.Primary)
                            } else if (geocodeMessage != null) {
                                Text(
                                    geocodeMessage!!,
                                    fontSize = 11.sp,
                                    color = if (geocodeMessage!!.startsWith("✓")) Color(0xFF16A34A) else HToGoColors.TextSecondary
                                )
                            }
                        }
                    } else {
                        Text(
                            "Al escribir tu dirección se ubicará en el mapa. También puedes arrastrar el pin.",
                            fontSize = 11.sp,
                            color = HToGoColors.TextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    // OSM Webview
                    OsmMapView(
                        latitude = lat,
                        longitude = lon,
                        zoom = 16,
                        isInteractive = true,
                        isDraggablePin = true,
                        onLocationChange = { newLat, newLon ->
                            lat = newLat
                            lon = newLon
                            coroutineScope.launch {
                                isReverseGeocoding = true
                                isGeocoding = true
                                geocodeMessage = "Detectando dirección..."
                                val res = GeocodingHelper.obtenerDireccionDeCoordenadas(newLat, newLon)
                                if (res != null) {
                                    if (!res.road.isNullOrBlank()) calle = res.road
                                    if (!res.houseNumber.isNullOrBlank()) numeroExterior = res.houseNumber
                                    if (!res.neighbourhood.isNullOrBlank()) colonia = res.neighbourhood
                                    if (!res.postcode.isNullOrBlank()) codigoPostal = res.postcode
                                    geocodeMessage = "✓ Dirección detectada desde el mapa"
                                }
                                isGeocoding = false
                                delay(600)
                                isReverseGeocoding = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )

                    Spacer(Modifier.height(16.dp))
                }

                Divider(color = HToGoColors.OutlineSoft)

                // Footer Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !guardando,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Cancelar", color = HToGoColors.TextSecondary)
                    }

                    Button(
                        onClick = {
                            if (calle.isBlank()) {
                                localError = "La calle es requerida"
                                return@Button
                            }
                            if (numeroExterior.isBlank()) {
                                localError = "El número exterior es requerido"
                                return@Button
                            }
                            if (colonia.isBlank()) {
                                localError = "La colonia es requerida"
                                return@Button
                            }
                            if (codigoPostal.isBlank()) {
                                localError = "El código postal es requerido"
                                return@Button
                            }
                            if (referencias.isBlank()) {
                                localError = "Las referencias son requeridas"
                                return@Button
                            }

                            guardando = true
                            localError = null

                            clienteViewModel.crearDireccion(
                                alias = alias,
                                calle = calle,
                                numeroExterior = numeroExterior,
                                numeroInterior = numeroInterior,
                                colonia = colonia,
                                codigoPostal = codigoPostal,
                                referencias = referencias,
                                lat = lat,
                                lon = lon,
                                onSuccess = { dir ->
                                    guardando = false
                                    onDireccionCreada(dir)
                                },
                                onError = { err ->
                                    guardando = false
                                    localError = err
                                }
                            )
                        },
                        enabled = !guardando,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HToGoColors.Primary),
                        modifier = Modifier.weight(1.3f).height(48.dp)
                    ) {
                        if (guardando) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Guardar dirección", fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
