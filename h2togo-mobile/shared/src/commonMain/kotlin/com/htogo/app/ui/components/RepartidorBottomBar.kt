package com.htogo.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.htogo.app.ui.theme.HToGoColors

enum class RepartidorTab { INICIO, NEGOCIO, INGRESOS, PERFIL }

@Composable
fun RepartidorBottomBar(
    selected: RepartidorTab,
    onInicio: () -> Unit = {},
    onNegocio: () -> Unit = {},
    onIngresos: () -> Unit = {},
    onPerfil: () -> Unit = {}
) {
    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        data class NavSpec(val tab: RepartidorTab, val label: String, val icon: ImageVector, val action: () -> Unit)
        val items = listOf(
            NavSpec(RepartidorTab.INICIO,   "Inicio",      Icons.Filled.Storefront, onInicio),
            NavSpec(RepartidorTab.NEGOCIO,  "Mi negocio",  Icons.Filled.Inventory2, onNegocio),
            NavSpec(RepartidorTab.INGRESOS, "Ingresos",    Icons.Filled.Payments,   onIngresos),
            NavSpec(RepartidorTab.PERFIL,   "Perfil",      Icons.Filled.Person,     onPerfil),
        )
        items.forEach { item ->
            NavigationBarItem(
                selected = item.tab == selected,
                onClick = item.action,
                icon = { Icon(item.icon, null) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = HToGoColors.Primary,
                    selectedTextColor = HToGoColors.Primary,
                    indicatorColor = HToGoColors.PrimarySoft,
                    unselectedIconColor = HToGoColors.TextTertiary,
                    unselectedTextColor = HToGoColors.TextTertiary
                )
            )
        }
    }
}
