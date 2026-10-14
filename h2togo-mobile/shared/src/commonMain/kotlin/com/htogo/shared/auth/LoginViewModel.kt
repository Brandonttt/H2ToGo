package com.htogo.shared.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htogo.app.ui.screens.LoginEstado
import com.htogo.shared.data.SesionDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Lógica del login compartida (la usa iOS; Android migrará a ella en la siguiente fase). */
class LoginViewModel(
    private val repositorio: AuthRepositorio,
    private val almacen: SesionAlmacen,
    private val alIniciar: (SesionDto) -> Unit
) : ViewModel() {

    private val _estado = MutableStateFlow<LoginEstado>(LoginEstado.Inactivo)
    val estado: StateFlow<LoginEstado> = _estado.asStateFlow()

    fun login(correo: String, password: String, forzar: Boolean) {
        _estado.value = LoginEstado.Cargando
        viewModelScope.launch {
            _estado.value = when (val r = repositorio.login(correo, password, forzar)) {
                is ResultadoLogin.Exito -> {
                    almacen.guardar(r.sesion)
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
