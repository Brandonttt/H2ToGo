package com.htogo.app.data.notificaciones

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.htogo.app.data.api.ApiClient
import com.htogo.app.data.local.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Registro del token FCM en el backend para recibir notificaciones con la app cerrada. Si la app
 * se compiló sin google-services.json, Firebase no se inicializa y todo esto es un no-op: las
 * notificaciones siguen llegando por WebSocket mientras la app está abierta.
 */
object DispositivoFcm {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun disponible(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    /** Pide el token actual y lo registra (tras el login o al abrir la app con sesión). */
    fun registrar(context: Context) {
        val app = context.applicationContext
        if (!disponible(app) || !SessionManager.getInstance(app).estaAutenticado()) return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { enviar(app, it) }
    }

    /** También lo llama el servicio de mensajería cuando Firebase rota el token. */
    fun enviar(context: Context, token: String) {
        if (!SessionManager.getInstance(context).estaAutenticado()) return
        scope.launch {
            try {
                ApiClient.getInstance(context).authApi.registrarDispositivo(mapOf("tokenFcm" to token))
            } catch (_: Exception) {
                // Sin red: se reintenta en el próximo inicio de la app.
            }
        }
    }
}
