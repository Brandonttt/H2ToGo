package com.htogo.shared.auth

import com.htogo.shared.data.SesionDto
import com.russhwolf.settings.Settings

/**
 * Sesión guardada en el dispositivo: SharedPreferences en Android y NSUserDefaults en iOS
 * (multiplatform-settings). Mismas claves que SessionManager de la app Android.
 */
class SesionAlmacen(private val settings: Settings = Settings()) {

    val token: String? get() = settings.getStringOrNull(KEY_TOKEN)
    val rol: String? get() = settings.getStringOrNull(KEY_ROL)
    val nombre: String? get() = settings.getStringOrNull(KEY_NOMBRE)

    fun guardar(sesion: SesionDto) {
        settings.putString(KEY_TOKEN, sesion.token)
        settings.putString(KEY_ROL, sesion.rol)
        settings.putInt(KEY_ID, sesion.perfil.idUsuario)
        settings.putString(KEY_NOMBRE, "${sesion.perfil.nombre} ${sesion.perfil.apellidos}".trim())
        settings.putString(KEY_CORREO, sesion.perfil.correo)
        settings.putString(KEY_TELEFONO, sesion.perfil.telefono)
    }

    fun cerrar() = settings.clear()

    private companion object {
        const val KEY_TOKEN = "auth_token"
        const val KEY_ROL = "user_role"
        const val KEY_ID = "user_id"
        const val KEY_NOMBRE = "user_name"
        const val KEY_CORREO = "user_email"
        const val KEY_TELEFONO = "user_phone"
    }
}
