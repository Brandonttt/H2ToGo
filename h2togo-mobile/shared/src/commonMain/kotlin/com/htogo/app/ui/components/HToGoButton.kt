package com.htogo.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.htogo.app.ui.theme.HToGoColors

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
