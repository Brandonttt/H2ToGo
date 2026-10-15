package com.htogo.app.plataforma

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/** Recibe los mensajes de enviarNativo(m) de la página y los pasa al hilo principal. */
private class PuenteNativo(private val alMensaje: (String) -> Unit) {
    private val principal = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun postMessage(mensaje: String) {
        principal.post { alMensaje(mensaje) }
    }
}

/** WebView para los mapas Leaflet compartidos. */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun VistaWebAndroid(
    html: String,
    modifier: Modifier,
    interactiva: Boolean,
    alCrear: (ControlWeb) -> Unit,
    alMensaje: (String) -> Unit
) {
    val mensaje by rememberUpdatedState(alMensaje)
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = false
                settings.useWideViewPort = false
                // La política de tiles de OpenStreetMap exige identificar la app.
                settings.userAgentString = "HToGo-App/1.0 (Android; info@h2togo.mx) AppleWebKit/537.36"
                addJavascriptInterface(PuenteNativo { mensaje(it) }, "HToGoNativo")
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        Log.d("HToGoMapa", "[JS] ${consoleMessage?.message()}")
                        return true
                    }
                }
                webViewClient = WebViewClient()
                // Dentro de pantallas con scroll, el mapa se queda con los gestos mientras se usa.
                if (interactiva) {
                    setOnTouchListener { v, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_POINTER_DOWN ->
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                                v.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                        false
                    }
                }
                loadDataWithBaseURL("https://openstreetmap.org", html, "text/html", "UTF-8", null)
                val web = this
                alCrear(object : ControlWeb {
                    override fun ejecutar(js: String) = web.evaluateJavascript(js, null)
                })
            }
        }
    )
}
