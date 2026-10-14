package com.htogo.app.data.local

import com.russhwolf.settings.Settings
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestor de sesión local del usuario, común a Android e iOS.
 * Almacena el token opaco emitido por el backend Spring Boot (RN-018) y el rol.
 * En Android usa el mismo archivo de SharedPreferences que antes ("htogo_session_prefs"),
 * así que las sesiones existentes se conservan; en iOS, NSUserDefaults.
 */
class SessionManager(private val prefs: Settings) {

    companion object {
        internal const val PREFS_NAME = "htogo_session_prefs"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_ROL = "user_role"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_DOB = "user_dob"
        private const val KEY_NEGOCIO_NOMBRE = "negocio_nombre"
        private const val KEY_NOTIFICACIONES_ACTIVAS = "notificaciones_activas"
        private const val KEY_DIRECCION_PREDETERMINADA_ID = "direccion_predeterminada_id"

        private val _sesionExpiradaFlow = MutableSharedFlow<String>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val sesionExpiradaFlow = _sesionExpiradaFlow.asSharedFlow()

        private val _token = MutableStateFlow<String?>(null)

        private var instance: SessionManager? = null

        /**
         * Instancia única. En Android recibe el `Context` (se usa su applicationContext); en iOS
         * se pasa `null`.
         */
        fun getInstance(context: Any? = null): SessionManager =
            instance ?: SessionManager(ajustesSesion(context)).also { instance = it }
    }

    init {
        _token.value = prefs.getStringOrNull(KEY_TOKEN)
    }

    /**
     * Token de la sesión actual (null sin sesión). Los ViewModels lo observan para cargar los
     * datos del usuario al iniciar sesión y limpiarlos al cerrarla.
     */
    val token: StateFlow<String?> get() = _token.asStateFlow()

    fun guardarSesion(token: String, rol: String, idUsuario: Int, nombre: String, correo: String, telefono: String = "") {
        _token.value = token
        prefs.putString(KEY_TOKEN, token)
        prefs.putString(KEY_ROL, rol)
        prefs.putInt(KEY_USER_ID, idUsuario)
        prefs.putString(KEY_USER_NAME, nombre)
        prefs.putString(KEY_USER_EMAIL, correo)
        prefs.putString(KEY_USER_PHONE, telefono)
    }

    fun guardarFechaNacimiento(fecha: String) {
        prefs.putString(KEY_USER_DOB, fecha)
    }

    fun guardarNombreNegocio(nombre: String) {
        prefs.putString(KEY_NEGOCIO_NOMBRE, nombre)
    }

    fun obtenerToken(): String? = prefs.getStringOrNull(KEY_TOKEN)

    fun obtenerRol(): String? = prefs.getStringOrNull(KEY_ROL)

    fun obtenerNombre(): String? = prefs.getStringOrNull(KEY_USER_NAME)

    fun obtenerCorreo(): String? = prefs.getStringOrNull(KEY_USER_EMAIL)
    
    fun obtenerTelefono(): String? = prefs.getStringOrNull(KEY_USER_PHONE)
    
    fun obtenerFechaNacimiento(): String? = prefs.getStringOrNull(KEY_USER_DOB)

    fun obtenerNombreNegocio(): String? = prefs.getStringOrNull(KEY_NEGOCIO_NOMBRE)

    fun obtenerIdUsuario(): Int = prefs.getInt(KEY_USER_ID, -1)

    fun estanNotificacionesActivas(): Boolean = prefs.getBoolean(KEY_NOTIFICACIONES_ACTIVAS, true)

    fun guardarNotificacionesActivas(activas: Boolean) {
        prefs.putBoolean(KEY_NOTIFICACIONES_ACTIVAS, activas)
    }

    fun guardarDireccionPredeterminadaId(id: Int) {
        prefs.putInt(KEY_DIRECCION_PREDETERMINADA_ID, id)
    }

    fun obtenerDireccionPredeterminadaId(): Int? {
        val id = prefs.getInt(KEY_DIRECCION_PREDETERMINADA_ID, -1)
        return if (id != -1) id else null
    }

    fun limpiarDireccionPredeterminadaId() {
        prefs.remove(KEY_DIRECCION_PREDETERMINADA_ID)
    }

    fun estaAutenticado(): Boolean = !obtenerToken().isNullOrBlank()

    fun cerrarSesion() {
        prefs.clear()
        _token.value = null
    }

    fun notificarSesionExpirada(motivo: String = "Tu sesión ha expirado porque se inició sesión en otro dispositivo.") {
        cerrarSesion()
        _sesionExpiradaFlow.tryEmit(motivo)
    }
}
