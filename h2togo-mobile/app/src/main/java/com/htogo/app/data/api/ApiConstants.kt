package com.htogo.app.data.api

object ApiConstants {
    /**
     * Túnel directo vía ADB (adb reverse tcp:8088 tcp:8088).
     * Permite conectar tu teléfono físico por cable USB directamente a tu backend en la PC
     * sin bloqueos de Firewall, sin depender de IPs locales ni del router Wi-Fi.
     */
    const val BASE_URL = "http://localhost:8088/api/v1/"
}
