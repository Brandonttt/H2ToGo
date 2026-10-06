package com.htogo.app

import android.app.Application
import com.htogo.app.data.api.GeocodingHelper

class HToGoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        GeocodingHelper.inicializar(this)
    }
}
