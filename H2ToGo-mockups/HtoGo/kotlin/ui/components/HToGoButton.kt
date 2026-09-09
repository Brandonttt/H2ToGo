package com.htogo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.htogo.ui.theme.HToGoColors

/**
 * Botón primario reutilizable de HToGo. Pill shape, 56dp, color azul agua.
 */
@Composable
fun HToGoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = if (primary) ButtonDefaults.buttonColors(
            containerColor = HToGoColors.Primary,
            contentColor   = Color.White,
            disabledContainerColor = HToGoColors.OutlineSoft,
            disabledContentColor   = HToGoColors.TextTertiary
        ) else ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor   = HToGoColors.Primary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (primary) 2.dp else 0.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun HToGoTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text, color = HToGoColors.Primary,
             style = MaterialTheme.typography.labelLarge)
    }
}
