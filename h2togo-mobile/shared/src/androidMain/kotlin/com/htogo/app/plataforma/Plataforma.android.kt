package com.htogo.app.plataforma

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

@SuppressLint("StaticFieldLeak") // es el applicationContext
private var contextoApp: Context? = null

/** Llamar en Application.onCreate. */
fun inicializarPlataforma(context: Context) {
    contextoApp = context.applicationContext
}

actual fun mostrarAviso(texto: String, largo: Boolean) {
    val ctx = contextoApp ?: return
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(ctx, texto, if (largo) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }
}
