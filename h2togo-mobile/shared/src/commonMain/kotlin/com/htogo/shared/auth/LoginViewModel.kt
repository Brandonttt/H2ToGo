package com.htogo.shared.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htogo.app.ui.screens.LoginEstado
import com.htogo.app.data.dto.SesionResponse
import com.htogo.app.data.local.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Lógica del login compartida (la usa iOS; Android migrará a ella en la siguiente fase). */
class LoginViewModel(
    private val repositorio: AuthRepositorio,
    private val sesion: SessionManager,
    private val alIniciar: (SesionResponse) -> Unit
) : ViewModel() {

    private val _estado = MutableStateFlow<LoginEstado>(LoginEstado.Inactivo)
    val estado: StateFlow<LoginEstado> = _estado.asStateFlow()

    fun login(correo: String, password: String, forzar: Boolean) {
        _estado.value = LoginEstado.Cargando
        viewModelScope.launch {
            _estado.value = when (val r = repositorio.login(correo, password, forzar)) {
                is ResultadoLogin.Exito -> {
                    val p = r.sesion.perfil
                    sesion.guardarSesion(r.sesion.token, r.sesion.rol, p.idUsuario,
                        "${p.nombre} ${p.apellidos}".trim(), p.correo, p.telefono)
                    alIniciar(r.sesion)
                    LoginEstado.Inactivo
                }
                ResultadoLogin.SesionActiva -> LoginEstado.Error("Ya hay una sesión activa en otro dispositivo.", sesionActiva = true)
                is ResultadoLogin.Error -> LoginEstado.Error(r.mensaje)
            }
        }
    }

    fun limpiarError() {
        if (_estado.value is LoginEstado.Error) _estado.value = LoginEstado.Inactivo
    }
}
