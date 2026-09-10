package com.htogo.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────── Paleta HToGo ───────────────────────────
object HToGoColors {
    val Primary       = Color(0xFF0077B6)   // azul agua
    val PrimaryLight  = Color(0xFF00B4D8)
    val PrimaryDark   = Color(0xFF03045E)
    val PrimarySoft   = Color(0xFFCAF0F8)   // wash de fondo
    val PrimaryWash   = Color(0xFFE0F7FF)

    val Surface       = Color(0xFFFFFFFF)
    val Background    = Color(0xFFF8FAFC)
    val OutlineSoft   = Color(0xFFE2E8F0)

    val TextPrimary   = Color(0xFF0F172A)
    val TextSecondary = Color(0xFF64748B)
    val TextTertiary  = Color(0xFF94A3B8)

    // Estados de pedido
    val StatusPendiente = Color(0xFF94A3B8)
    val StatusAsignado  = Color(0xFFF59E0B)
    val StatusEnCamino  = Color(0xFF0077B6)
    val StatusEntregado = Color(0xFF10B981)
    val StatusCancelado = Color(0xFFEF4444)

    // Acentos
    val AccentEmerald = Color(0xFF10B981)
    val AccentAmber   = Color(0xFFF59E0B)
    val AccentRose    = Color(0xFFEF4444)
    val AccentPurple  = Color(0xFF8B5CF6)
}

private val LightScheme = lightColorScheme(
    primary           = HToGoColors.Primary,
    onPrimary         = Color.White,
    primaryContainer  = HToGoColors.PrimarySoft,
    onPrimaryContainer = HToGoColors.PrimaryDark,
    secondary         = HToGoColors.PrimaryLight,
    background        = HToGoColors.Background,
    onBackground      = HToGoColors.TextPrimary,
    surface           = HToGoColors.Surface,
    onSurface         = HToGoColors.TextPrimary,
    surfaceVariant    = Color(0xFFF1F5F9),
    onSurfaceVariant  = HToGoColors.TextSecondary,
    outline           = HToGoColors.OutlineSoft,
    error             = HToGoColors.StatusCancelado
)

val HToGoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val HToGoTypography = Typography(
    displayLarge = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold,    letterSpacing = (-0.5).sp),
    displayMedium= TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    headlineLarge= TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall= TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleLarge   = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleMedium  = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyLarge    = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyMedium   = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    labelLarge   = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp)
)

@Composable
fun HToGoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightScheme,
        shapes      = HToGoShapes,
        typography  = HToGoTypography,
        content     = content
    )
}
