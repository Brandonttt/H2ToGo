package com.htogo.shared.auth

import com.htogo.shared.data.crearClienteHttp
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AuthRepositorioTest {

    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun repo(status: HttpStatusCode, cuerpo: String, alPedir: (String, String) -> Unit = { _, _ -> }) =
        AuthRepositorio(crearClienteHttp("https://api.test/api/v1/", engine = MockEngine { req ->
            alPedir(req.url.toString(), req.body.toByteArray().decodeToString())
            respond(cuerpo, status, json)
        }))

    @Test
    fun loginExitosoLeeLaSesionEIgnoraCamposNuevos() = runTest {
        var url = ""; var enviado = ""
        val r = repo(HttpStatusCode.OK, """
            {"token":"abc","rol":"cliente","campoNuevo":1,
             "perfil":{"idUsuario":7,"correo":"a@b.mx","nombre":"Ana","apellidos":"Ruiz","telefono":"555","rol":"cliente"}}
        """) { u, b -> url = u; enviado = b }.login("  a@b.mx ", "secreta1")

        val exito = assertIs<ResultadoLogin.Exito>(r)
        assertEquals("abc", exito.sesion.token)
        assertEquals(7, exito.sesion.perfil.idUsuario)
        assertEquals("https://api.test/api/v1/auth/login", url)
        assertTrue(enviado.contains("\"correo\":\"a@b.mx\""), enviado) // se recorta el correo
    }

    @Test
    fun conflictoEsSesionActiva() = runTest {
        assertIs<ResultadoLogin.SesionActiva>(repo(HttpStatusCode.Conflict, "{}").login("a@b.mx", "x"))
    }

    @Test
    fun errorDeNegocioMuestraElMensajeDelBackend() = runTest {
        val r = repo(HttpStatusCode.UnprocessableEntity,
            """{"codigo":"TELEFONO_NO_VERIFICADO","mensaje":"Verifica tu teléfono"}""").login("a@b.mx", "x")
        assertEquals("Verifica tu teléfono", assertIs<ResultadoLogin.Error>(r).mensaje)
    }

    @Test
    fun credencialesInvalidas() = runTest {
        val r = repo(HttpStatusCode.Unauthorized, "{}").login("a@b.mx", "x")
        assertTrue(assertIs<ResultadoLogin.Error>(r).mensaje.startsWith("Credenciales inválidas"))
    }
}
