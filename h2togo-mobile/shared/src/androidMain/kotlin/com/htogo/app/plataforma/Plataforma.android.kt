package com.htogo.app.plataforma

import android.annotation.SuppressLint
import android.content.Context

@SuppressLint("StaticFieldLeak") // es el applicationContext
private var contextoApp: Context? = null

/** Llamar en Application.onCreate. */
fun inicializarPlataforma(context: Context) {
    contextoApp = context.applicationContext
}
