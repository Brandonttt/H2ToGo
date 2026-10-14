package com.htogo.app.data.api

import com.htogo.app.data.local.SessionManager
import com.htogo.shared.data.crearClienteHttp

/**
 * Cliente HTTP centralizado (Ktor) que expone las APIs del backend, común a Android e iOS.
 * Manda el token de la sesión en cada petición y, si el backend responde 401 con una sesión
 * activa (fuera de login/registro), la marca como expirada (RN-018).
 */
class ApiClient private constructor(sessionManager: SessionManager, baseUrl: String) {

    private val http = crearClienteHttp(
        baseUrl = baseUrl,
        token = { sessionManager.obtenerToken() },
        alNoAutorizado = { ruta ->
            if (!ruta.contains("/auth/login") && !ruta.contains("/auth/registro")) {
                sessionManager.notificarSesionExpirada()
            }
        }
    )

    val authApi = AuthApi(http)
    val pedidosApi = PedidosApi(http)
    val direccionesApi = DireccionesApi(http)
    val negociosApi = NegociosApi(http)
    val repartidorApi = RepartidorApi(http)
    val inventarioApi = InventarioApi(http)
    val solicitudesApi = SolicitudesApi(http)

    companion object {
        private var instance: ApiClient? = null

        /** En Android recibe el `Context`; en iOS `null`. [baseUrl] solo aplica la primera vez. */
        fun getInstance(context: Any? = null, baseUrl: String = ApiConstants.BASE_URL): ApiClient =
            instance ?: ApiClient(SessionManager.getInstance(context), baseUrl).also {
                instance = it
                com.htogo.app.data.realtime.StompCliente.baseUrl = baseUrl // el WebSocket va al mismo backend
            }
    }
}
