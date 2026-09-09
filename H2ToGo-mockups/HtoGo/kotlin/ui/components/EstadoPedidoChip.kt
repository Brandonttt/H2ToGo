package com.htogo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htogo.ui.theme.HToGoColors

enum class EstadoPedido(val label: String, val color: Color) {
    PENDIENTE  ("Pendiente",  HToGoColors.StatusPendiente),
    ASIGNADO   ("Asignado",   HToGoColors.StatusAsignado),
    EN_CAMINO  ("En camino",  HToGoColors.StatusEnCamino),
    ENTREGADO  ("Entregado",  HToGoColors.StatusEntregado),
    CANCELADO  ("Cancelado",  HToGoColors.StatusCancelado),
}

@Composable
fun EstadoPedidoChip(estado: EstadoPedido, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(99.dp))
            .background(estado.color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(estado.color))
            Spacer(Modifier.width(6.dp))
            Text(
                estado.label,
                color = estado.color,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
