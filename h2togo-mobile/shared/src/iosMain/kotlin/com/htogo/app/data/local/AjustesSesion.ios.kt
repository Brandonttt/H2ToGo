package com.htogo.app.data.local

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

internal actual fun ajustesSesion(contexto: Any?): Settings =
    NSUserDefaultsSettings(NSUserDefaults(suiteName = SessionManager.PREFS_NAME))
