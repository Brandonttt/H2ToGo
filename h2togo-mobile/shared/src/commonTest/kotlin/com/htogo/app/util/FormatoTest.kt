package com.htogo.app.util

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatoTest {
    @Test
    fun patronesQueUsaLaApp() {
        assertEquals("$45", "$%.0f".format(45.4))
        assertEquals("$45.50", "$%.2f".format(45.5))
        assertEquals("$1,250", "$%,.0f".format(1249.6))
        assertEquals("07:05", "%02d:%02d".format(7, 5))
        assertEquals("1.25 km", "%.2f km".format(1.2468))
        assertEquals("Estás a 61 m del domicilio. Acércate a menos de 50 m.",
            "Estás a %.0f m del domicilio. Acércate a menos de %d m.".format(60.6, 50))
        assertEquals("09/03/2026", "%02d/%02d/%04d".format(9, 3, 2026))
        assertEquals("100% listo", "%d%% listo".format(100))
        assertEquals("-3.5", "%.1f".format(-3.46))
        assertEquals("0.0", "%.1f".format(-0.01))
    }
}
