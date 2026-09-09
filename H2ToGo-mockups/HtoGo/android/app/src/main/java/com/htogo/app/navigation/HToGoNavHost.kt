package com.htogo.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.htogo.app.ui.screens.AsignandoRepartidorScreen
import com.htogo.app.ui.screens.AvisoPrivacidadScreen
import com.htogo.app.ui.screens.BuscarPurificadorasScreen
import com.htogo.app.ui.screens.HistorialPedidosScreen
import com.htogo.app.ui.screens.HomeClienteScreen
import com.htogo.app.ui.screens.HomeRepartidorScreen
import com.htogo.app.ui.screens.IngresosScreen
import com.htogo.app.ui.screens.InventarioVehiculoScreen
import com.htogo.app.ui.screens.LoginScreen
import com.htogo.app.ui.screens.NuevoPedidoAbiertoScreen
import com.htogo.app.ui.screens.NuevoPedidoScreen
import com.htogo.app.ui.screens.PedidosDisponiblesScreen
import com.htogo.app.ui.screens.PerfilClienteScreen
import com.htogo.app.ui.screens.PerfilPurificadoraScreen
import com.htogo.app.ui.screens.PerfilRepartidorScreen
import com.htogo.app.ui.screens.ProductosPreciosScreen
import com.htogo.app.ui.screens.RegistroScreen
import com.htogo.app.ui.screens.RutaEntregaScreen
import com.htogo.app.ui.screens.SeguimientoPedidoScreen
import com.htogo.app.ui.screens.SolicitudPedidoProgramadoScreen
import com.htogo.app.ui.screens.SplashOnboardingScreen
import com.htogo.app.ui.screens.VerificacionTelefonoScreen

@Composable
fun HToGoNavHost(
    navController: NavHostController,
    startDestination: String = HToGoRoutes.SPLASH
) {
    NavHost(navController = navController, startDestination = startDestination) {

        // ───────── Onboarding / auth ─────────
        composable(HToGoRoutes.SPLASH) {
            SplashOnboardingScreen(
                onLogin    = { navController.navigate(HToGoRoutes.LOGIN) },
                onRegister = { navController.navigate(HToGoRoutes.REGISTRO) }
            )
        }
        composable(HToGoRoutes.LOGIN) {
            LoginScreen(
                onLogin    = { navController.navigateAndClear(HToGoRoutes.HOME_CLIENTE) },
                onRegister = { navController.navigate(HToGoRoutes.REGISTRO) },
                onForgot   = { /* TODO recovery flow */ }
            )
        }
        composable(HToGoRoutes.REGISTRO) {
            RegistroScreen(
                onBack    = { navController.popBackStack() },
                onSubmit  = { navController.navigate(HToGoRoutes.OTP) },
                onLogin   = { navController.navigate(HToGoRoutes.LOGIN) },
                onAvisoPrivacidad = { navController.navigate(HToGoRoutes.AVISO_PRIVACIDAD) }
            )
        }
        composable(HToGoRoutes.AVISO_PRIVACIDAD) {
            AvisoPrivacidadScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(HToGoRoutes.OTP) {
            VerificacionTelefonoScreen(
                onBack    = { navController.popBackStack() },
                onSuccess = { navController.navigateAndClear(HToGoRoutes.HOME_CLIENTE) }
            )
        }

        // ───────── Cliente ─────────
        composable(HToGoRoutes.HOME_CLIENTE) {
            HomeClienteScreen(
                onNuevoPedido        = { navController.navigate(HToGoRoutes.NUEVO_PEDIDO) },
                onElegirPurificadora = { navController.navigate(HToGoRoutes.BUSCAR_PURIFICADORAS) },
                onPedirAbierto       = { navController.navigate(HToGoRoutes.NUEVO_PEDIDO_ABIERTO) },
                onTrackPedido        = { navController.navigate(HToGoRoutes.SEGUIMIENTO) },
                onHistorial          = { navController.navigate(HToGoRoutes.HISTORIAL_CLIENTE) },
                onPerfil             = { navController.navigate(HToGoRoutes.PERFIL_CLIENTE) },
                onAbrirPurificadora  = { navController.navigate(HToGoRoutes.PERFIL_PURIFICADORA) },
                onSwitchRol          = { navController.navigate(HToGoRoutes.HOME_REPARTIDOR) }
            )
        }
        composable(HToGoRoutes.BUSCAR_PURIFICADORAS) {
            BuscarPurificadorasScreen(
                onBack              = { navController.popBackStack() },
                onAbrirPurificadora = { navController.navigate(HToGoRoutes.PERFIL_PURIFICADORA) }
            )
        }
        composable(HToGoRoutes.PERFIL_PURIFICADORA) {
            PerfilPurificadoraScreen(
                onBack  = { navController.popBackStack() },
                onPedir = { navController.navigate(HToGoRoutes.NUEVO_PEDIDO) }
            )
        }
        composable(HToGoRoutes.NUEVO_PEDIDO_ABIERTO) {
            NuevoPedidoAbiertoScreen(
                onBack    = { navController.popBackStack() },
                onConfirm = {
                    navController.navigate(HToGoRoutes.ASIGNANDO) {
                        popUpTo(HToGoRoutes.HOME_CLIENTE)
                    }
                }
            )
        }
        composable(HToGoRoutes.NUEVO_PEDIDO) {
            NuevoPedidoScreen(
                onBack    = { navController.popBackStack() },
                onConfirm = {
                    navController.navigate(HToGoRoutes.ASIGNANDO) {
                        popUpTo(HToGoRoutes.HOME_CLIENTE)
                    }
                }
            )
        }
        composable(HToGoRoutes.ASIGNANDO) {
            AsignandoRepartidorScreen(
                onCancel   = { navController.popBackStack(HToGoRoutes.HOME_CLIENTE, inclusive = false) },
                onAsignado = { navController.navigateAndClear(HToGoRoutes.SEGUIMIENTO) }
            )
        }
        composable(HToGoRoutes.SEGUIMIENTO) {
            SeguimientoPedidoScreen(
                onBack             = { navController.navigateAndClear(HToGoRoutes.HOME_CLIENTE) },
                onPedidoEntregado  = { navController.navigateAndClear(HToGoRoutes.HOME_CLIENTE) },
                onAbrirPurificadora = { navController.navigate(HToGoRoutes.PERFIL_PURIFICADORA) }
            )
        }
        composable(HToGoRoutes.HISTORIAL_CLIENTE) {
            HistorialPedidosScreen(
                onBack             = { navController.popBackStack() },
                onPedidoTap        = { navController.navigate(HToGoRoutes.SEGUIMIENTO) },
                onAbrirPurificadora = { navController.navigate(HToGoRoutes.PERFIL_PURIFICADORA) },
                onRepetir          = { navController.navigate(HToGoRoutes.NUEVO_PEDIDO) }
            )
        }
        composable(HToGoRoutes.PERFIL_CLIENTE) {
            PerfilClienteScreen(
                onBack    = { navController.popBackStack() },
                onLogout  = { navController.navigateAndClear(HToGoRoutes.LOGIN) }
            )
        }

        // ───────── Repartidor ─────────
        composable(HToGoRoutes.HOME_REPARTIDOR) {
            HomeRepartidorScreen(
                onInventario       = { navController.navigate(HToGoRoutes.INVENTARIO) },
                onIngresos         = { navController.navigate(HToGoRoutes.INGRESOS) },
                onPerfil           = { navController.navigate(HToGoRoutes.PERFIL_REPARTIDOR) },
                onRuta             = { navController.navigate(HToGoRoutes.RUTA_ENTREGA) },
                onPedidoProgramado = { navController.navigate(HToGoRoutes.PEDIDO_PROGRAMADO) },
                onSwitchRol        = { navController.navigate(HToGoRoutes.HOME_CLIENTE) }
            )
        }
        composable(HToGoRoutes.PEDIDOS_DISPONIBLES) {
            PedidosDisponiblesScreen(
                onBack     = { navController.popBackStack() },
                onAceptar  = { navController.navigate(HToGoRoutes.RUTA_ENTREGA) }
            )
        }
        composable(HToGoRoutes.PEDIDO_PROGRAMADO) {
            SolicitudPedidoProgramadoScreen(
                onBack        = { navController.popBackStack() },
                onIniciarRuta = { navController.navigateAndClear(HToGoRoutes.RUTA_ENTREGA) }
            )
        }
        composable(HToGoRoutes.INVENTARIO) {
            InventarioVehiculoScreen(
                onBack    = { navController.popBackStack() },
                onInicio  = { navController.navigateAndClear(HToGoRoutes.HOME_REPARTIDOR) },
                onIngresos = { navController.navigate(HToGoRoutes.INGRESOS) },
                onPerfil  = { navController.navigate(HToGoRoutes.PERFIL_REPARTIDOR) },
                onProductosPrecios = { navController.navigate(HToGoRoutes.PRODUCTOS_PRECIOS) }
            )
        }
        composable(HToGoRoutes.PRODUCTOS_PRECIOS) {
            ProductosPreciosScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(HToGoRoutes.RUTA_ENTREGA) {
            RutaEntregaScreen(
                onBack       = { navController.popBackStack() },
                onCompletada = { navController.navigateAndClear(HToGoRoutes.HOME_REPARTIDOR) }
            )
        }
        composable(HToGoRoutes.INGRESOS) {
            IngresosScreen(
                onBack    = { navController.popBackStack() },
                onInicio  = { navController.navigateAndClear(HToGoRoutes.HOME_REPARTIDOR) },
                onNegocio = { navController.navigate(HToGoRoutes.INVENTARIO) },
                onPerfil  = { navController.navigate(HToGoRoutes.PERFIL_REPARTIDOR) }
            )
        }
        composable(HToGoRoutes.PERFIL_REPARTIDOR) {
            PerfilRepartidorScreen(
                onBack    = { navController.popBackStack() },
                onLogout  = { navController.navigateAndClear(HToGoRoutes.LOGIN) },
                onInicio  = { navController.navigateAndClear(HToGoRoutes.HOME_REPARTIDOR) },
                onNegocio = { navController.navigate(HToGoRoutes.INVENTARIO) },
                onIngresos = { navController.navigate(HToGoRoutes.INGRESOS) }
            )
        }
    }
}

private fun NavHostController.navigateAndClear(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { inclusive = false }
        launchSingleTop = true
    }
}
