package com.htogo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htogo.app.ui.theme.HToGoColors

/**
 * Aviso discreto de que la ubicación del repartidor es simulada (FakeGPS): chip blanco como los
 * demás controles flotantes del mapa, con pausa/reanudar y salir.
 */
@Composable
fun SimulacionGpsChip(
    simulando: Boolean,
    paso: Int,
    totalPasos: Int,
    onAlternar: () -> Unit,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = HToGoColors.Surface,
        shadowElevation = 4.dp,
        modifier = modifier,
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(8.dp).clip(CircleShape)
                    .background(if (simulando) HToGoColors.AccentAmber else HToGoColors.TextTertiary)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (simulando && totalPasos > 0) "Ubicación simulada · ${paso.coerceAtMost(totalPasos)}/$totalPasos"
                else "Ubicación simulada",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = HToGoColors.TextPrimary,
            )
            Spacer(Modifier.width(4.dp))
            BotonChip(if (simulando) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                if (simulando) "Pausar" else "Reanudar", onAlternar)
            BotonChip(Icons.Filled.Close, "Salir de la simulación", onSalir)
        }
    }
}

@Composable
private fun BotonChip(icono: ImageVector, descripcion: String, onClick: () -> Unit) {
    Box(
        Modifier.size(28.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icono, descripcion, tint = HToGoColors.TextSecondary, modifier = Modifier.size(18.dp))
    }
}

/** Opción del menú de simulación, con ícono en vez de emoji. */
@Composable
fun OpcionSimulacion(texto: String, icono: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(texto, fontSize = 14.sp) },
        leadingIcon = { Icon(icono, null, tint = HToGoColors.TextSecondary, modifier = Modifier.size(20.dp)) },
        onClick = onClick,
    )
}
