package com.htogo.app.data.local

import android.content.Context

/**
 * Hora en que el repartidor llegó al domicilio de cada pedido (RN-012). Se guarda en disco para
 * que el temporizador de 10 minutos no se reinicie al salir de la pantalla o cerrar la app.
 */
object LlegadaEntrega {
    private const val PREFS = "htogo_llegadas"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun obtener(ctx: Context, idPedido: Int): Long? =
        prefs(ctx).getLong("p$idPedido", 0L).takeIf { it > 0L }

    /** Registra la llegada solo la primera vez; devuelve la hora registrada. */
    fun registrar(ctx: Context, idPedido: Int): Long =
        obtener(ctx, idPedido) ?: System.currentTimeMillis().also {
            prefs(ctx).edit().putLong("p$idPedido", it).apply()
        }

    fun limpiar(ctx: Context, idPedido: Int) {
        prefs(ctx).edit().remove("p$idPedido").apply()
    }
}
