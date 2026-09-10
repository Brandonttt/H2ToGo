package com.htogo.app.data.local

import android.content.Context
import android.content.SharedPreferences

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

        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun guardarSesion(token: String, rol: String, idUsuario: Int, nombre: String, correo: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_ROL, rol)
            .putInt(KEY_USER_ID, idUsuario)
            .putString(KEY_USER_NAME, nombre)
            .putString(KEY_USER_EMAIL, correo)
            .apply()
    }

    fun obtenerToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun obtenerRol(): String? = prefs.getString(KEY_ROL, null)

    fun obtenerNombre(): String? = prefs.getString(KEY_USER_NAME, null)

    fun obtenerCorreo(): String? = prefs.getString(KEY_USER_EMAIL, null)

    fun obtenerIdUsuario(): Int = prefs.getInt(KEY_USER_ID, -1)

    fun estaAutenticado(): Boolean = !obtenerToken().isNullOrBlank()

    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }
}
