package com.htogo.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.htogo.app.data.api.ApiClient
import com.htogo.app.data.dto.LoginRequest
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

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val apiClient = ApiClient.getInstance(application)
    private val sessionManager = SessionManager.getInstance(application)
    private val gson = Gson()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _registroState = MutableStateFlow<RegistroUiState>(RegistroUiState.Idle)
    val registroState: StateFlow<RegistroUiState> = _registroState.asStateFlow()

    private val _otpState = MutableStateFlow<OtpUiState>(OtpUiState.Idle)
    val otpState: StateFlow<OtpUiState> = _otpState.asStateFlow()

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
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Error de conexión con el servidor")
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
        nombreNegocio: String? = null
    ) {
        viewModelScope.launch {
            _registroState.value = RegistroUiState.Loading
            try {
                val negocioReq = when {
                    idNegocioExistente != null -> RegistroNegocioRequest(idExistente = idNegocioExistente)
                    !nombreNegocio.isNullOrBlank() -> RegistroNegocioRequest(nombreComercial = nombreNegocio.trim())
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
                    e.localizedMessage ?: "Error de conexión al registrarse"
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
                    e.localizedMessage ?: "Error de red al verificar el código"
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
        if (errorJson.isNullOrBlank()) return null
        return try {
            val map = gson.fromJson(errorJson, Map::class.java)
            map["mensaje"]?.toString() ?: map["message"]?.toString()
        } catch (e: Exception) {
            null
        }
    }
}
