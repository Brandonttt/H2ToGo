package com.htogo.app.data.api

import com.htogo.app.data.dto.CambiarTelefonoOtpRequest
import com.htogo.app.data.dto.EliminarCuentaRequest
import com.htogo.app.data.dto.IniciarVerificacionRequest
import com.htogo.app.data.dto.LoginRequest
import com.htogo.shared.data.codigoDeError
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
import kotlin.test.assertFalse
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
        val raw = r.errorBody()?.string()
        assertEquals(422, r.code())
        assertNull(r.body())
        assertEquals("TELEFONO_NO_VERIFICADO", codigoDeError(raw))
        assertEquals("Verifica tu teléfono", mensajeDeError(raw))
    }

    @Test
    fun iniciarVerificacionTelefonoDevuelveDatosDeCuenta() = runTest {
        var url = ""; var enviado = ""
        val r = api(HttpStatusCode.OK, """
            {"idUsuario":12,"correo":"juan@gmail.com","telefono":"5614588669","telefonoVerificado":false,"mensaje":"SMS enviado"}
        """) { u, b, _ -> url = u; enviado = b }
            .iniciarVerificacionTelefono(IniciarVerificacionRequest("juan@gmail.com", "secreta1"))

        assertTrue(r.isSuccessful)
        assertEquals("https://api.test/api/v1/auth/verificacion-telefono/iniciar", url)
        assertTrue(enviado.contains("\"correo\":\"juan@gmail.com\""), enviado)
        assertEquals("5614588669", r.body()?.telefono)
        assertFalse(r.body()?.telefonoVerificado ?: true)
    }

    @Test
    fun cambiarTelefonoOtpEnviaNuevoNumero() = runTest {
        var url = ""; var enviado = ""
        val r = api(HttpStatusCode.NoContent, "") { u, b, _ -> url = u; enviado = b }
            .cambiarTelefonoOtp(CambiarTelefonoOtpRequest("juan@gmail.com", "secreta1", "5512345678"))

        assertTrue(r.isSuccessful)
        assertEquals("https://api.test/api/v1/auth/verificacion-telefono/cambiar-numero", url)
        assertTrue(enviado.contains("\"nuevoTelefono\":\"5512345678\""), enviado)
    }

    @Test
    fun eliminarCuentaEnviaContrasenaAlEndpointDeBaja() = runTest {
        var url = ""; var enviado = ""; var auth: String? = null
        val r = api(HttpStatusCode.NoContent, "") { u, b, a -> url = u; enviado = b; auth = a }
            .eliminarCuenta(EliminarCuentaRequest("secreta123"))

        assertTrue(r.isSuccessful)
        assertEquals("https://api.test/api/v1/usuarios/me/baja", url)
        assertTrue(enviado.contains("\"password\":\"secreta123\""), enviado)
        assertEquals("Bearer tok", auth)
    }

    @Test
    fun respuestaSinContenidoEsExitosa() = runTest {
        val r = api(HttpStatusCode.NoContent, "").logout()
        assertTrue(r.isSuccessful)
    }
}
