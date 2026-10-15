package com.htogo.app.data.notificaciones

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CanalNotificacionesTest {
    @Test
    fun leeElAvisoDelBackend() {
        val a = CanalNotificaciones.leer(
            """{"id":"n1","titulo":"Pedido en camino","mensaje":"Tu repartidor va en camino.","datos":{"tipo":"pedido_en_camino","idPedido":"16"}}"""
        )
        assertEquals("Pedido en camino", a?.titulo)
        assertEquals("16", a?.datos?.get("idPedido"))
    }

    @Test
    fun formatoInesperadoSeIgnora() {
        assertNull(CanalNotificaciones.leer("""{"titulo":"sin id"}"""))
        assertNull(CanalNotificaciones.leer("no es json"))
    }
}
