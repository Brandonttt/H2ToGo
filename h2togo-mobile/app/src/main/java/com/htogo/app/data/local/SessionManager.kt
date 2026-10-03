package com.htogo.app.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Gestor de sesión local del usuario.
 * Almacena el token opaco emitido por el backend Spring Boot (RN-018) y el rol.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "htogo_session_prefs"
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

        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun guardarSesion(token: String, rol: String, idUsuario: Int, nombre: String, correo: String, telefono: String = "") {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_ROL, rol)
            .putInt(KEY_USER_ID, idUsuario)
            .putString(KEY_USER_NAME, nombre)
            .putString(KEY_USER_EMAIL, correo)
            .putString(KEY_USER_PHONE, telefono)
            .apply()
    }

    fun guardarFechaNacimiento(fecha: String) {
        prefs.edit().putString(KEY_USER_DOB, fecha).apply()
    }

    fun guardarNombreNegocio(nombre: String) {
        prefs.edit().putString(KEY_NEGOCIO_NOMBRE, nombre).apply()
    }

    fun obtenerToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun obtenerRol(): String? = prefs.getString(KEY_ROL, null)

    fun obtenerNombre(): String? = prefs.getString(KEY_USER_NAME, null)

    fun obtenerCorreo(): String? = prefs.getString(KEY_USER_EMAIL, null)
    
    fun obtenerTelefono(): String? = prefs.getString(KEY_USER_PHONE, null)
    
    fun obtenerFechaNacimiento(): String? = prefs.getString(KEY_USER_DOB, null)

    fun obtenerNombreNegocio(): String? = prefs.getString(KEY_NEGOCIO_NOMBRE, null)

    fun obtenerIdUsuario(): Int = prefs.getInt(KEY_USER_ID, -1)

    fun estanNotificacionesActivas(): Boolean = prefs.getBoolean(KEY_NOTIFICACIONES_ACTIVAS, true)

    fun guardarNotificacionesActivas(activas: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICACIONES_ACTIVAS, activas).apply()
    }

    fun guardarDireccionPredeterminadaId(id: Int) {
        prefs.edit().putInt(KEY_DIRECCION_PREDETERMINADA_ID, id).apply()
    }

    fun obtenerDireccionPredeterminadaId(): Int? {
        val id = prefs.getInt(KEY_DIRECCION_PREDETERMINADA_ID, -1)
        return if (id != -1) id else null
    }

    fun limpiarDireccionPredeterminadaId() {
        prefs.edit().remove(KEY_DIRECCION_PREDETERMINADA_ID).apply()
    }

    fun estaAutenticado(): Boolean = !obtenerToken().isNullOrBlank()

    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }

    fun notificarSesionExpirada(motivo: String = "Tu sesión ha expirado porque se inició sesión en otro dispositivo.") {
        cerrarSesion()
        _sesionExpiradaFlow.tryEmit(motivo)
    }
}
