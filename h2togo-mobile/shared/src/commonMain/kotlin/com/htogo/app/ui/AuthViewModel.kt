package com.htogo.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htogo.shared.data.mensajeDeError
import com.htogo.app.plataforma.ahoraMs
import com.htogo.app.plataforma.Plataforma
import com.htogo.app.data.api.ApiClient
import com.htogo.app.data.dto.CambioContrasenaRequest
import com.htogo.app.data.dto.LoginRequest
import com.htogo.app.data.dto.RecuperacionConfirmacionRequest
import com.htogo.app.data.dto.RecuperacionRequest
import com.htogo.app.data.dto.OtpRequest
import com.htogo.app.data.dto.ReenviarOtpRequest
import com.htogo.app.data.dto.RegistroNegocioRequest
import com.htogo.app.data.dto.RegistroRequest
import com.htogo.app.data.dto.RegistroResponse
import com.htogo.app.data.dto.SesionResponse
import com.htogo.app.data.local.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val sesion: SesionResponse) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

sealed class RegistroUiState {
    object Idle : RegistroUiState()
    object Loading : RegistroUiState()
    data class Success(val response: RegistroResponse) : RegistroUiState()
    data class Error(val message: String) : RegistroUiState()
}

sealed class OtpUiState {
    object Idle : OtpUiState()
    object Loading : OtpUiState()
    object Success : OtpUiState()
    data class Error(val message: String) : OtpUiState()
}

class AuthViewModel : ViewModel() {

    private val apiClient = ApiClient.getInstance()
    private val sessionManager = SessionManager.getInstance()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _registroState = MutableStateFlow<RegistroUiState>(RegistroUiState.Idle)
    val registroState: StateFlow<RegistroUiState> = _registroState.asStateFlow()

    private val _otpState = MutableStateFlow<OtpUiState>(OtpUiState.Idle)
    val otpState: StateFlow<OtpUiState> = _otpState.asStateFlow()

    private val _purificadoras = MutableStateFlow<List<com.htogo.app.data.dto.NegocioCercanoResponse>>(emptyList())
    val purificadoras: StateFlow<List<com.htogo.app.data.dto.NegocioCercanoResponse>> = _purificadoras.asStateFlow()

    fun cargarPurificadoras() {
        viewModelScope.launch {
            try {
                val resp = apiClient.negociosApi.buscarCercanos(cerca = null, limite = 50)
                if (resp.isSuccessful && resp.body() != null) {
                    _purificadoras.value = resp.body()!!
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    // Datos temporales para iniciar sesión automáticamente post-verificación OTP
    var tempCorreo: String = ""
        private set
    var tempTelefono: String = ""
        private set
    var tempPass: String = ""
        private set

    fun login(correo: String, pass: String, forzar: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = apiClient.authApi.login(
                    LoginRequest(
                        correo = correo.trim(),
                        password = pass,
                        forzar = forzar
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val sesion = response.body()!!
                    sessionManager.guardarSesion(
                        token = sesion.token,
                        rol = sesion.rol,
                        idUsuario = sesion.perfil.idUsuario,
                        nombre = "${sesion.perfil.nombre} ${sesion.perfil.apellidos}".trim(),
                        correo = sesion.perfil.correo,
                        telefono = sesion.perfil.telefono
                    )
                    Plataforma.registrarDispositivo()
                    if (sesion.rol.equals("repartidor", ignoreCase = true)) {
                        try {
                            val negocioResp = apiClient.negociosApi.obtenerMiNegocio()
                            if (negocioResp.isSuccessful && negocioResp.body() != null) {
                                sessionManager.guardarNombreNegocio(negocioResp.body()!!.nombreComercial)
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                    _uiState.value = AuthUiState.Success(sesion)
                } else if (response.code() == 409) {
                    _uiState.value = AuthUiState.Error("SESION_ACTIVA: Ya hay una sesión abierta en otro dispositivo.")
                } else if (response.code() == 401) {
                    _uiState.value = AuthUiState.Error("Credenciales inválidas. Verifica tu correo y contraseña.")
                } else {
                    val errMsg = parseError(response.errorBody()?.string())
                    _uiState.value = AuthUiState.Error(errMsg ?: "Error al iniciar sesión (${response.code()})")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Error de conexión con el servidor")
            }
        }
    }

    fun registrar(
        nombre: String,
        apellidos: String,
        correo: String,
        pass: String,
        telefono: String,
        rol: String,
        idNegocioExistente: Int? = null,
        nombreNegocio: String? = null,
        calle: String? = null,
        numeroExterior: String? = null,
        colonia: String? = null,
        codigoPostal: String? = null,
        referencias: String? = null,
        lat: Double? = null,
        lon: Double? = null,
        horarioApertura: String? = null,
        horarioCierre: String? = null
    ) {
        viewModelScope.launch {
            _registroState.value = RegistroUiState.Loading
            try {
                val negocioReq = when {
                    idNegocioExistente != null -> RegistroNegocioRequest(idExistente = idNegocioExistente)
                    !nombreNegocio.isNullOrBlank() -> RegistroNegocioRequest(
                        nombreComercial = nombreNegocio.trim(),
                        calle = calle?.trim(),
                        numeroExterior = numeroExterior?.trim(),
                        colonia = colonia?.trim(),
                        codigoPostal = codigoPostal?.trim(),
                        referencias = referencias?.trim(),
                        lat = lat,
                        lon = lon,
                        horarioApertura = horarioApertura?.trim(),
                        horarioCierre = horarioCierre?.trim()
                    )
                    else -> null
                }
                val request = RegistroRequest(
                    nombre = nombre.trim(),
                    apellidos = apellidos.trim().ifBlank { "." },
                    correo = correo.trim(),
                    password = pass,
                    telefono = telefono.trim(),
                    rol = rol.lowercase(),
                    negocio = negocioReq
                )

                val response = apiClient.authApi.registrar(request)
                if (response.isSuccessful && response.body() != null) {
                    tempCorreo = correo.trim()
                    tempTelefono = telefono.trim()
                    tempPass = pass
                    _registroState.value = RegistroUiState.Success(response.body()!!)
                } else {
                    val errMsg = parseError(response.errorBody()?.string())
                    _registroState.value = RegistroUiState.Error(
                        errMsg ?: "No se pudo completar el registro (Código ${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _registroState.value = RegistroUiState.Error(
                    e.message ?: "Error de conexión al registrarse"
                )
            }
        }
    }

    fun verificarOtp(codigo: String) {
        viewModelScope.launch {
            _otpState.value = OtpUiState.Loading
            try {
                val response = apiClient.authApi.verificarTelefono(
                    OtpRequest(correo = tempCorreo, codigo = codigo.trim())
                )
                if (response.isSuccessful) {
                    // Si tenemos la contraseña temporal guardada, iniciamos sesión automáticamente antes de notificar éxito
                    if (tempPass.isNotBlank() && tempCorreo.isNotBlank()) {
                        try {
                            val loginResp = apiClient.authApi.login(
                                LoginRequest(correo = tempCorreo, password = tempPass, forzar = true)
                            )
                            if (loginResp.isSuccessful && loginResp.body() != null) {
                                val sesion = loginResp.body()!!
                                sessionManager.guardarSesion(
                                    token = sesion.token,
                                    rol = sesion.rol,
                                    idUsuario = sesion.perfil.idUsuario,
                                    nombre = "${sesion.perfil.nombre} ${sesion.perfil.apellidos}".trim(),
                                    correo = sesion.perfil.correo,
                                    telefono = sesion.perfil.telefono
                                )
                                Plataforma.registrarDispositivo()
                                _uiState.value = AuthUiState.Success(sesion)
                            }
                        } catch (e: Exception) {
                            // En caso de error de login inmediato, se continuará
                        }
                    }
                    _otpState.value = OtpUiState.Success
                } else {
                    val errMsg = parseError(response.errorBody()?.string())
                    _otpState.value = OtpUiState.Error(
                        errMsg ?: "Código inválido o expirado. Verifica el código SMS."
                    )
                }
            } catch (e: Exception) {
                _otpState.value = OtpUiState.Error(
                    e.message ?: "Error de red al verificar el código"
                )
            }
        }
    }

    fun reenviarOtp() {
        viewModelScope.launch {
            try {
                if (tempCorreo.isNotBlank()) {
                    apiClient.authApi.reenviarOtp(ReenviarOtpRequest(correo = tempCorreo))
                }
            } catch (e: Exception) {
                // Silencioso o log
            }
        }
    }

    fun logout(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                apiClient.authApi.logout()
            } catch (e: Exception) {
                // Si falla la red, igualmente se limpia el token local (CU-003 E1)
            } finally {
                sessionManager.cerrarSesion()
                _uiState.value = AuthUiState.Idle
                _registroState.value = RegistroUiState.Idle
                _otpState.value = OtpUiState.Idle
                onComplete()
            }
        }
    }

    // ---------------------------------------------------------------- Contraseña
    // onResult(null) = éxito; onResult(mensaje) = error para mostrar en la pantalla.

    /** Con la sesión iniciada: pide la contraseña actual para confirmar que es el dueño de la cuenta. */
    fun cambiarContrasena(actual: String, nueva: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val resp = apiClient.authApi.cambiarContrasena(CambioContrasenaRequest(actual, nueva))
                onResult(if (resp.isSuccessful) null
                else parseError(resp.errorBody()?.string()) ?: "No se pudo cambiar la contraseña (${resp.code()})")
            } catch (e: Exception) {
                onResult(e.message ?: "Error de red al cambiar la contraseña")
            }
        }
    }

    /** "¿Olvidaste tu contraseña?" paso 1: el backend manda un código por SMS al teléfono de la cuenta. */
    fun solicitarRecuperacion(correo: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val resp = apiClient.authApi.solicitarRecuperacion(RecuperacionRequest(correo.trim()))
                onResult(if (resp.isSuccessful) null
                else parseError(resp.errorBody()?.string()) ?: "No se pudo enviar el código (${resp.code()})")
            } catch (e: Exception) {
                onResult(e.message ?: "Error de red al enviar el código")
            }
        }
    }

    /** Paso 2: código del SMS y contraseña nueva; el backend cierra las sesiones abiertas. */
    fun confirmarRecuperacion(correo: String, codigo: String, nueva: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val resp = apiClient.authApi.confirmarRecuperacion(
                    RecuperacionConfirmacionRequest(correo.trim(), codigo.trim(), nueva)
                )
                onResult(if (resp.isSuccessful) null
                else parseError(resp.errorBody()?.string()) ?: "Código inválido o expirado")
            } catch (e: Exception) {
                onResult(e.message ?: "Error de red al cambiar la contraseña")
            }
        }
    }

    fun setRegistrationTarget(correo: String, telefono: String) {
        tempCorreo = correo
        tempTelefono = telefono
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
        _registroState.value = RegistroUiState.Idle
        _otpState.value = OtpUiState.Idle
    }

    fun obtenerRolActual(): String? = sessionManager.obtenerRol()

    private fun parseError(errorJson: String?): String? {
        return mensajeDeError(errorJson)
    }
}
