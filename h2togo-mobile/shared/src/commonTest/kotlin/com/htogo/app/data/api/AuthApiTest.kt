package com.htogo.app.data.api

import com.htogo.app.data.dto.LoginRequest
import com.htogo.shared.data.crearClienteHttp
import com.htogo.shared.data.mensajeDeError
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthApiTest {

    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun api(status: HttpStatusCode, cuerpo: String, alPedir: (String, String, String?) -> Unit = { _, _, _ -> }) =
        AuthApi(crearClienteHttp("https://api.test/api/v1/", token = { "tok" }, engine = MockEngine { req ->
            alPedir(req.url.toString(), req.body.toByteArray().decodeToString(), req.headers[HttpHeaders.Authorization])
            respond(cuerpo, status, json)
        }))

    @Test
    fun loginExitosoLeeLaSesionYMandaElToken() = runTest {
        var url = ""; var enviado = ""; var auth: String? = null
        val r = api(HttpStatusCode.OK, """
            {"token":"abc","rol":"cliente","campoNuevo":1,
             "perfil":{"id":7,"correo":"a@b.mx","nombre":"Ana","apellidos":"Ruiz","telefono":"555","rol":"cliente","urlFotoPerfil":null}}
        """) { u, b, a -> url = u; enviado = b; auth = a }.login(LoginRequest("a@b.mx", "secreta1"))

        assertTrue(r.isSuccessful)
        assertEquals(7, r.body()?.perfil?.idUsuario)
        assertEquals("https://api.test/api/v1/auth/login", url)
        assertTrue(enviado.contains("\"correo\":\"a@b.mx\""), enviado)
        assertEquals("Bearer tok", auth)
    }

    @Test
    fun errorDeNegocioConservaElCuerpo() = runTest {
        val r = api(HttpStatusCode.UnprocessableEntity, """{"codigo":"TELEFONO_NO_VERIFICADO","mensaje":"Verifica tu teléfono"}""")
            .login(LoginRequest("a@b.mx", "x"))
        assertEquals(422, r.code())
        assertNull(r.body())
        assertEquals("Verifica tu teléfono", mensajeDeError(r.errorBody()?.string()))
    }

    @Test
    fun respuestaSinContenidoEsExitosa() = runTest {
        val r = api(HttpStatusCode.NoContent, "").logout()
        assertTrue(r.isSuccessful)
    }
}
