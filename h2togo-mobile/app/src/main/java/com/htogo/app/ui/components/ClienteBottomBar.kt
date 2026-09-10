package com.htogo.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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

enum class ClienteTab { INICIO, PEDIDOS, PERFIL }

@Composable
fun ClienteBottomBar(
    selected: ClienteTab,
    onInicio: () -> Unit = {},
    onPedidos: () -> Unit = {},
    onPerfil: () -> Unit = {}
) {
    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        data class NavSpec(val tab: ClienteTab, val label: String, val icon: ImageVector, val action: () -> Unit)
        val items = listOf(
            NavSpec(ClienteTab.INICIO,  "Inicio",  Icons.Filled.Home,    onInicio),
            NavSpec(ClienteTab.PEDIDOS, "Pedidos", Icons.Filled.History, onPedidos),
            NavSpec(ClienteTab.PERFIL,  "Perfil",  Icons.Filled.Person,  onPerfil),
        )
        items.forEach { item ->
            NavigationBarItem(
                selected = item.tab == selected,
                onClick = item.action,
                icon = { Icon(item.icon, contentDescription = item.label) },
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
