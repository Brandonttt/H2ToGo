package com.htogo.app.data.dto

import com.htogo.shared.data.jsonApi
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Lo que Gson hacía implícitamente y kotlinx.serialization debe respetar con jsonApi. */
class SerializacionTest {

    @Test
    fun seEnvianLosValoresPorDefectoYNoLosNull() {
        val json = jsonApi.encodeToString(RegistroRequest("Ana", "Ruiz", "a@b.mx", "secreta1", "5550000000"))
        assertTrue(json.contains("\"rol\":\"cliente\""), json) // valor por defecto: el backend lo exige
        assertFalse(json.contains("negocio"), json)           // null: no se envía
    }

    @Test
    fun solicitudConMapaLibre() {
        val req = SolicitudRequest("AGREGAR_PRODUCTO", valorNuevo = mapOf("idMarca" to 3, "precio" to 25.5, "nombre" to "Bonafont", "activo" to true))
        val json = jsonApi.encodeToString(req)
        assertTrue(json.contains("\"valorNuevo\":{\"idMarca\":3,\"precio\":25.5,\"nombre\":\"Bonafont\",\"activo\":true}"), json)
    }

    @Test
    fun respuestaConCamposFaltantesNullsYDesconocidos() {
        // Sin "subtotal", con null en un campo no nulo y un campo que la app no conoce.
        val d = jsonApi.decodeFromString<DetalleResponse>(
            """{"id":5,"idMarca":2,"cantidadSolicitada":3,"tieneEnvase":null,"campoNuevo":1}"""
        )
        assertEquals(3, d.cantidad)   // nombre alterno
        assertEquals(false, d.tieneEnvase)
        assertEquals(null, d.subtotal)
    }

    @Test
    fun jornadaUsaLosNombresDelBackend() {
        val j = jsonApi.decodeFromString<JornadaResponse>(
            """{"idVehiculo":4,"estadoOperativo":true,"inventarioVehiculo":{"idVehiculo":4,"capacidadGarrafones":30,"ocupado":6,"libre":24,"lotes":[]}}"""
        )
        assertTrue(j.disponible)
        assertEquals(6, j.inventario?.ocupado)
    }
}
