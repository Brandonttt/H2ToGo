package com.htogo.app.data.local

import com.russhwolf.settings.Settings

/** Almacenamiento de la sesión de cada plataforma (SharedPreferences / NSUserDefaults). */
internal expect fun ajustesSesion(contexto: Any?): Settings
