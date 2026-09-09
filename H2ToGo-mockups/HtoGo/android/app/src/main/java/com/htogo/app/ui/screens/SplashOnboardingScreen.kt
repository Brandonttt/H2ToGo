package com.htogo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htogo.app.ui.components.HToGoButton
import com.htogo.app.ui.components.HToGoTextButton
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String
)

private val pages = listOf(
    OnboardingPage(
        Icons.Filled.WaterDrop,
        "Agua a un toque",
        "Pide tus garrafones de agua potable sin llamadas ni esperas innecesarias."
    ),
    OnboardingPage(
        Icons.Filled.LocationOn,
        "Sigue tu pedido en vivo",
        "Mira en tiempo real dónde está tu repartidor y cuándo llegará a tu puerta."
    ),
    OnboardingPage(
        Icons.Filled.LocalShipping,
        "Hecho para Benito Juárez",
        "Repartidores locales verificados, listos para entregar en tu colonia."
    )
)

@Composable
fun SplashOnboardingScreen(
    onLogin: () -> Unit = {},
    onRegister: () -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(HToGoColors.PrimarySoft, HToGoColors.Background)
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Brand mark
            Row(
                Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(HToGoColors.Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.WaterDrop,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "HToGo",
                    color = HToGoColors.PrimaryDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }

            Spacer(Modifier.height(40.dp))

            // Pager (illustration + title + subtitle)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val p = pages[page]
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(48.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            p.icon,
                            contentDescription = null,
                            tint = HToGoColors.Primary,
                            modifier = Modifier.size(96.dp)
                        )
                    }
                    Spacer(Modifier.height(40.dp))
                    Text(
                        p.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = HToGoColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        p.subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = HToGoColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Page indicator
            Row(
                Modifier.padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(pages.size) { i ->
                    val active = pagerState.currentPage == i
                    Box(
                        Modifier
                            .height(8.dp)
                            .width(if (active) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) HToGoColors.Primary
                                else HToGoColors.OutlineSoft
                            )
                    )
                }
            }

            // CTAs
            HToGoButton(
                text = if (pagerState.currentPage == pages.lastIndex) "Crear cuenta" else "Siguiente",
                onClick = {
                    if (pagerState.currentPage == pages.lastIndex) onRegister()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            )
            Spacer(Modifier.height(8.dp))
            HToGoTextButton(
                text = "Ya tengo cuenta · Iniciar sesión",
                onClick = onLogin
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun SplashOnboardingPreview() {
    HToGoTheme { SplashOnboardingScreen() }
}
