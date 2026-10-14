package com.htogo.app.data.local

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

internal actual fun ajustesSesion(contexto: Any?): Settings {
    val ctx = requireNotNull(contexto as? Context) { "SessionManager.getInstance requiere un Context en Android" }
    return SharedPreferencesSettings(
        ctx.applicationContext.getSharedPreferences(SessionManager.PREFS_NAME, Context.MODE_PRIVATE)
    )
}
