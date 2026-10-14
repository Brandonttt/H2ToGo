package com.htogo.app.data.local

import com.htogo.app.plataforma.ahoraMs
import com.russhwolf.settings.Settings

/**
 * Hora en que el repartidor llegó al domicilio de cada pedido (RN-012). Se guarda en disco para
 * que el temporizador de 10 minutos no se reinicie al salir de la pantalla o cerrar la app.
 */
object LlegadaEntrega {
    private val prefs: Settings by lazy { Settings() }

    fun obtener(idPedido: Int): Long? = prefs.getLong("llegada_p$idPedido", 0L).takeIf { it > 0L }

    /** Registra la llegada solo la primera vez; devuelve la hora registrada. */
    fun registrar(idPedido: Int): Long =
        obtener(idPedido) ?: ahoraMs().also { prefs.putLong("llegada_p$idPedido", it) }

    fun limpiar(idPedido: Int) = prefs.remove("llegada_p$idPedido")
}
