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

import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.htogo.app.data.notificaciones.DestinoNotificacion
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.htogo.app.data.api.ApiClient
import com.htogo.app.data.local.SessionManager
import kotlinx.coroutines.delay

import androidx.lifecycle.viewmodel.compose.viewModel
import com.htogo.app.ui.AuthViewModel
import com.htogo.app.ui.ClienteViewModel
import com.htogo.app.ui.RepartidorViewModel

@Composable
fun HToGoNavHost(
    navController: NavHostController,
    startDestination: String = HToGoRoutes.SPLASH
) {
    val authViewModel: AuthViewModel = viewModel()
    val clienteViewModel: ClienteViewModel = viewModel()
    val repartidorViewModel: RepartidorViewModel = viewModel()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }
    val apiClient = remember { ApiClient.getInstance(context) }

    // Escucha global de expulsión por sesión duplicada o expirada (RN-018):
    // Limpia los datos de sesión y redirige inmediatamente a la pantalla de bienvenida.
    LaunchedEffect(Unit) {
        SessionManager.sesionExpiradaFlow.collect { motivo ->
            Toast.makeText(context, motivo, Toast.LENGTH_LONG).show()
            navController.navigateAndClear(HToGoRoutes.SPLASH)
        }
    }

    // Heartbeat periódico (cada 8 segundos):
    // Si se inició sesión en otro dispositivo, el backend devuelve 401 y AuthInterceptor
    // activa el flujo de expulsión inmediata sin necesidad de que el usuario pulse la pantalla.
    LaunchedEffect(Unit) {
        while (true) {
            delay(8_000)
            if (sessionManager.estaAutenticado()) {
                try {
                    apiClient.authApi.verificarSesion()
                } catch (_: Exception) {
                    // El error 401 es procesado por AuthInterceptor
                }
            }
        }
    }

    // Al tocar una notificación: abre la pantalla que corresponde a su tipo (ver DestinoNotificacion).
    val destinoNotificacion by DestinoNotificacion.pendiente.collectAsState()
    LaunchedEffect(destinoNotificacion) {
        val destino = destinoNotificacion ?: return@LaunchedEffect
        if (!sessionManager.estaAutenticado()) {
            DestinoNotificacion.consumir()
            return@LaunchedEffect
        }
        // En arranque en frío el splash redirige al inicio: se espera a que lo haga para no competir.
        snapshotFlow { navController.currentBackStackEntry?.destination?.route }
            .first { it != null && it != HToGoRoutes.SPLASH }
        val esRepartidor = sessionManager.obtenerRol()?.equals("repartidor", ignoreCase = true) == true
        val ruta = when {
            !esRepartidor && destino.tipo.startsWith("pedido_") -> HToGoRoutes.seguimiento(destino.idPedido)
            !esRepartidor -> null
            destino.tipo == "pedido_nuevo" -> HToGoRoutes.PEDIDOS_DISPONIBLES
            destino.tipo == "solicitud_resuelta" -> HToGoRoutes.PRODUCTOS_PRECIOS
            destino.tipo == "lotes_por_caducar" -> HToGoRoutes.INVENTARIO
            destino.tipo == DestinoNotificacion.TIPO_ENTREGA_EN_CURSO -> {
                // Si el proceso se reinició, recupera el pedido para que la pantalla de ruta tenga datos.
                destino.idPedido?.let { repartidorViewModel.cargarPedidoEnRuta(it) }
                HToGoRoutes.RUTA_ENTREGA
            }
            else -> null
        }
        DestinoNotificacion.consumir()
        ruta?.let { navController.navigate(it) { launchSingleTop = true } }
    }

    NavHost(navController = navController, startDestination = startDestination) {

        // ───────── Onboarding / auth ─────────
        composable(HToGoRoutes.SPLASH) {
            val context = LocalContext.current
            val sessionManager = remember { SessionManager.getInstance(context) }

            LaunchedEffect(Unit) {
                if (sessionManager.estaAutenticado()) {
                    val rol = sessionManager.obtenerRol()
                    if (rol?.equals("repartidor", ignoreCase = true) == true) {
                        navController.navigateAndClear(HToGoRoutes.HOME_REPARTIDOR)
                    } else {
                        navController.navigateAndClear(HToGoRoutes.HOME_CLIENTE)
                    }
                }
            }

            SplashOnboardingScreen(
                onLogin    = { navController.navigate(HToGoRoutes.LOGIN) },
                onRegister = { navController.navigate(HToGoRoutes.REGISTRO) }
            )
        }
        composable(HToGoRoutes.LOGIN) {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = { sesion ->
                    if (sesion.rol.equals("repartidor", ignoreCase = true)) {
                        navController.navigateAndClear(HToGoRoutes.HOME_REPARTIDOR)
                    } else {
                        navController.navigateAndClear(HToGoRoutes.HOME_CLIENTE)
                    }
                },
                onRegister = { navController.navigate(HToGoRoutes.REGISTRO) },
                onForgot   = { /* TODO recovery flow */ }
            )
        }
        composable(HToGoRoutes.REGISTRO) {
            RegistroScreen(
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onSubmitSuccess = { correo, telefono ->
                    authViewModel.setRegistrationTarget(correo, telefono)
                    navController.navigate(HToGoRoutes.OTP)
                },
                onLogin = { navController.navigate(HToGoRoutes.LOGIN) },
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
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onSuccess = {
                    val rol = authViewModel.obtenerRolActual()
                    if (rol?.equals("repartidor", ignoreCase = true) == true) {
                        navController.navigateAndClear(HToGoRoutes.HOME_REPARTIDOR)
                    } else {
                        navController.navigateAndClear(HToGoRoutes.HOME_CLIENTE)
                    }
                }
            )
        }

        // ───────── Cliente ─────────
        composable(HToGoRoutes.HOME_CLIENTE) {
            HomeClienteScreen(
                onNuevoPedido        = { navController.navigate(HToGoRoutes.NUEVO_PEDIDO) },
                onElegirPurificadora = { navController.navigate(HToGoRoutes.BUSCAR_PURIFICADORAS) },
                onPedirAbierto       = { navController.navigate(HToGoRoutes.NUEVO_PEDIDO_ABIERTO) },
                onTrackPedido        = { navController.navigate(HToGoRoutes.seguimiento(clienteViewModel.pedidoActivo.value?.id)) },
                onHistorial          = {
                    navController.navigate(HToGoRoutes.HISTORIAL_CLIENTE) {
                        popUpTo(HToGoRoutes.HOME_CLIENTE) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onPerfil             = {
                    navController.navigate(HToGoRoutes.PERFIL_CLIENTE) {
                        popUpTo(HToGoRoutes.HOME_CLIENTE) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onAbrirPurificadora  = { navController.navigate(HToGoRoutes.PERFIL_PURIFICADORA) },
                onSwitchRol          = { navController.navigate(HToGoRoutes.HOME_REPARTIDOR) },
                clienteViewModel     = clienteViewModel
            )
        }
        composable(HToGoRoutes.BUSCAR_PURIFICADORAS) {
            BuscarPurificadorasScreen(
                onBack              = { navController.popBackStack() },
                onAbrirPurificadora = { navController.navigate(HToGoRoutes.PERFIL_PURIFICADORA) },
                clienteViewModel    = clienteViewModel
            )
        }
        composable(HToGoRoutes.PERFIL_PURIFICADORA) {
            PerfilPurificadoraScreen(
                onBack           = { navController.popBackStack() },
                onPedir          = { navController.navigate(HToGoRoutes.NUEVO_PEDIDO) },
                clienteViewModel = clienteViewModel
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
                onBack           = { navController.popBackStack() },
                onConfirm        = {
                    navController.navigate(HToGoRoutes.ASIGNANDO) {
                        popUpTo(HToGoRoutes.HOME_CLIENTE)
                    }
                },
                clienteViewModel = clienteViewModel
            )
        }
        composable(HToGoRoutes.ASIGNANDO) {
            AsignandoRepartidorScreen(
                onCancel   = { navController.popBackStack(HToGoRoutes.HOME_CLIENTE, inclusive = false) },
                onAsignado = { navController.navigateAndClear(HToGoRoutes.seguimiento(clienteViewModel.pedidoActivo.value?.id)) }
            )
        }
        composable(
            HToGoRoutes.SEGUIMIENTO,
            arguments = listOf(navArgument("idPedido") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { entry ->
            // Sin id explícito (p. ej. desde el inicio) se sigue el último pedido del cliente.
            val idPedido = entry.arguments?.getString("idPedido")?.toIntOrNull() ?: clienteViewModel.pedidoActivo.value?.id
            SeguimientoPedidoScreen(
                pedidoId           = idPedido,
                onBack             = { navController.navigateAndClear(HToGoRoutes.HOME_CLIENTE) },
                onPedidoEntregado  = { navController.navigateAndClear(HToGoRoutes.HOME_CLIENTE) },
                onAbrirPurificadora = { navController.navigate(HToGoRoutes.PERFIL_PURIFICADORA) },
                clienteViewModel   = clienteViewModel
            )
        }
        composable(HToGoRoutes.HISTORIAL_CLIENTE) {
            HistorialPedidosScreen(
                onBack              = { navController.popBackStack() },
                onInicio            = {
                    navController.navigate(HToGoRoutes.HOME_CLIENTE) {
                        popUpTo(HToGoRoutes.HOME_CLIENTE) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onPerfil            = {
                    navController.navigate(HToGoRoutes.PERFIL_CLIENTE) {
                        popUpTo(HToGoRoutes.HOME_CLIENTE) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onPedidoTap         = { id -> navController.navigate(HToGoRoutes.seguimiento(id)) },
                onAbrirPurificadora = { navController.navigate(HToGoRoutes.PERFIL_PURIFICADORA) },
                onRepetir           = { navController.navigate(HToGoRoutes.NUEVO_PEDIDO) },
                clienteViewModel    = clienteViewModel
            )
        }
        composable(HToGoRoutes.PERFIL_CLIENTE) {
            val context = LocalContext.current
            val sessionManager = remember { SessionManager.getInstance(context) }
            PerfilClienteScreen(
                onBack    = { navController.popBackStack() },
                onInicio  = {
                    navController.navigate(HToGoRoutes.HOME_CLIENTE) {
                        popUpTo(HToGoRoutes.HOME_CLIENTE) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onPedidos = {
                    navController.navigate(HToGoRoutes.HISTORIAL_CLIENTE) {
                        popUpTo(HToGoRoutes.HOME_CLIENTE) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onLogout  = {
                    authViewModel.logout {
                        navController.navigateAndClear(HToGoRoutes.LOGIN)
                    }
                },
                onAvisoPrivacidad = {
                    navController.navigate(HToGoRoutes.AVISO_PRIVACIDAD)
                }
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
                onSwitchRol        = { navController.navigate(HToGoRoutes.HOME_CLIENTE) },
                onPedidosDisponibles = { navController.navigate(HToGoRoutes.PEDIDOS_DISPONIBLES) },
                repartidorViewModel = repartidorViewModel
            )
        }
        composable(HToGoRoutes.PEDIDOS_DISPONIBLES) {
            PedidosDisponiblesScreen(
                onBack     = { navController.popBackStack() },
                onAceptar  = { navController.navigate(HToGoRoutes.RUTA_ENTREGA) },
                repartidorViewModel = repartidorViewModel
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
                onProductosPrecios = { navController.navigate(HToGoRoutes.PRODUCTOS_PRECIOS) },
                repartidorViewModel = repartidorViewModel
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
                onCompletada = { navController.navigateAndClear(HToGoRoutes.HOME_REPARTIDOR) },
                repartidorViewModel = repartidorViewModel
            )
        }
        composable(HToGoRoutes.INGRESOS) {
            IngresosScreen(
                onBack    = { navController.popBackStack() },
                onInicio  = { navController.navigateAndClear(HToGoRoutes.HOME_REPARTIDOR) },
                onNegocio = { navController.navigate(HToGoRoutes.INVENTARIO) },
                onPerfil  = { navController.navigate(HToGoRoutes.PERFIL_REPARTIDOR) },
                repartidorViewModel = repartidorViewModel
            )
        }
        composable(HToGoRoutes.PERFIL_REPARTIDOR) {
            val context = LocalContext.current
            val sessionManager = remember { SessionManager.getInstance(context) }
            PerfilRepartidorScreen(
                onBack    = { navController.popBackStack() },
                onLogout  = {
                    authViewModel.logout {
                        navController.navigateAndClear(HToGoRoutes.LOGIN)
                    }
                },
                onInicio  = { navController.navigateAndClear(HToGoRoutes.HOME_REPARTIDOR) },
                onNegocio = { navController.navigate(HToGoRoutes.INVENTARIO) },
                onIngresos = { navController.navigate(HToGoRoutes.INGRESOS) },
                repartidorViewModel = repartidorViewModel
            )
        }
    }
}

private fun NavHostController.navigateAndClear(route: String) {
    navigate(route) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}
