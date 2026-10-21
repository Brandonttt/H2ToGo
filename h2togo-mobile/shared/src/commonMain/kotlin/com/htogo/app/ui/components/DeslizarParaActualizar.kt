package com.htogo.app.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.htogo.app.ui.theme.HToGoColors

/**
 * Deslizar hacia abajo para actualizar (Android e iOS). El contenido debe poder desplazarse
 * (Column con verticalScroll o LazyColumn). [bajoBarraDeEstado]: para pantallas cuyo encabezado
 * llega hasta arriba, el indicador aparece debajo de la hora y la batería.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeslizarParaActualizar(
    actualizando: Boolean,
    onActualizar: () -> Unit,
    modifier: Modifier = Modifier,
    bajoBarraDeEstado: Boolean = false,
    contenido: @Composable BoxScope.() -> Unit
) {
    val estado = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = actualizando,
        onRefresh = onActualizar,
        modifier = modifier,
        state = estado,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = estado,
                isRefreshing = actualizando,
                color = HToGoColors.Primary,
                modifier = Modifier.align(Alignment.TopCenter)
                    .then(if (bajoBarraDeEstado) Modifier.statusBarsPadding() else Modifier)
            )
        },
        content = contenido
    )
}
