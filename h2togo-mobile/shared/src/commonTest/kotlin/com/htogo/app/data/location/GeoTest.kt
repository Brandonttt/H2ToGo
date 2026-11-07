package com.htogo.app.data.location

import kotlin.test.Test
import kotlin.test.assertEquals

class GeoTest {
    @Test
    fun formatoDuracionSegundosMinutosYHoras() {
        assertEquals("15 s", formatearDuracionSegundos(14))
        assertEquals("45 s", formatearDuracionSegundos(46))
        assertEquals("1 min", formatearDuracionSegundos(61))
        assertEquals("1 min 25 s", formatearDuracionSegundos(85))
        assertEquals("8 min", formatearDuracionSegundos(8 * 60))
        assertEquals("1 h", formatearDuracionMinutos(60))
        assertEquals("5 h", formatearDuracionMinutos(300))
        assertEquals("2 h 15 min", formatearDuracionMinutos(135))
        assertEquals("149 h 45 min", formatearDuracionMinutos(8985))
    }

    @Test
    fun formatoDistanciaMetrosYKilometros() {
        assertEquals("5 m", formatearDistanciaCorta(0.002))
        assertEquals("279 m", formatearDistanciaCorta(0.279))
        assertEquals("938 m", formatearDistanciaMetros(938.0))
        assertEquals("1 km", formatearDistanciaCorta(1.0))
        assertEquals("1.4 km", formatearDistanciaCorta(1.42))
        assertEquals("250 km", formatearDistanciaCorta(250.3))
        assertEquals("2,995 km", formatearDistanciaCorta(2994.9))
    }

    @Test
    fun formatoTiempoTranscurrido() {
        assertEquals("25 s", formatearTiempoTranscurrido(25_000L))
        assertEquals("4 min", formatearTiempoTranscurrido(240_000L))
        assertEquals("3 h", formatearTiempoTranscurrido(3 * 3_600_000L))
        assertEquals("1 h 20 min", formatearTiempoTranscurrido(80 * 60_000L))
    }
}
