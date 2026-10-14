package com.htogo.app

import android.app.Application

class HToGoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.htogo.app.data.api.GeocodingAndroid.inicializar(this)
        // Código compartido (KMP): avisos, sesión y API se inicializan con el Context de la app.
        com.htogo.app.plataforma.inicializarPlataforma(this)
        com.htogo.app.data.local.SessionManager.getInstance(this)
        com.htogo.app.data.api.ApiClient.getInstance(this)
        com.htogo.app.plataforma.Plataforma.servicios = com.htogo.app.plataforma.ServiciosAndroid(this)
        com.htogo.app.plataforma.Plataforma.registrarDispositivo = {
            com.htogo.app.data.notificaciones.DispositivoFcm.registrar(this)
        }
    }
}
