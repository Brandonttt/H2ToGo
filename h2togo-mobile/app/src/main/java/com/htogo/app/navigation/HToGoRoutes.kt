package com.htogo.app.navigation

/**
 * Catálogo de rutas de la app HToGo. Coincide con el `data-htogo-nav` de los mockups HTML.
 * Mantenemos los IDs cortos que ya usaban los HTML para que el equipo los reconozca.
 */
object HToGoRoutes {
    // Onboarding / auth
    const val SPLASH    = "splash"     // 01
    const val REGISTRO  = "registro"   // 02
    const val AVISO_PRIVACIDAD = "aviso_privacidad" // 02b
    const val OTP       = "otp"        // 03 — verificación teléfono
    const val LOGIN     = "login"      // 04

    // Cliente
    const val HOME_CLIENTE       = "home_c"        // 05
    const val BUSCAR_PURIFICADORAS = "purif_list"  // lista de purificadoras
    const val PERFIL_PURIFICADORA = "purif_c"      // 05c — detalle
    const val NUEVO_PEDIDO       = "form"          // 06 — pedido directo
    const val NUEVO_PEDIDO_ABIERTO = "form_abierto" // 06b — precio máximo
    const val ASIGNANDO          = "buscando"      // 07a
    const val SEGUIMIENTO        = "track"         // 07
    const val HISTORIAL_CLIENTE  = "hist_c"        // 08
    const val PERFIL_CLIENTE     = "perfil_c"      // 09

    // Repartidor
    const val HOME_REPARTIDOR    = "home_r"        // 05b
    const val INVENTARIO         = "inv"           // 10
    const val PRODUCTOS_PRECIOS  = "prods"         // CU-23 catálogo del negocio
    const val PEDIDOS_DISPONIBLES = "ped"          // 11
    const val RUTA_ENTREGA       = "ruta"          // 12
    const val INGRESOS           = "ingresos"      // 13 (antes "ganancias")
    const val PERFIL_REPARTIDOR  = "perfil_r"      // 14
    const val PEDIDO_PROGRAMADO  = "ped_prog"      // 15 — detalle de pedido apartado
}
