package com.htogo.app.plataforma

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.htogo.app.data.api.GeocodingAndroid
import com.htogo.app.data.api.GeocodingHelper
import com.htogo.app.data.location.EntregaEnCursoService
import com.htogo.app.data.location.Posicion
import com.htogo.app.data.location.UbicacionAndroid
import kotlinx.coroutines.flow.StateFlow

/** Servicios de Android para el código compartido (se registra en HToGoApp). */
class ServiciosAndroid(context: Context) : ServiciosPlataforma {
    private val app = context.applicationContext

    override suspend fun buscarCoordenadas(texto: String): GeocodingHelper.GeoResult? =
        GeocodingAndroid.buscarCoordenadas(texto)

    override suspend fun direccionDeCoordenadas(lat: Double, lon: Double): GeocodingHelper.GeoResult? =
        GeocodingAndroid.obtenerDireccionDeCoordenadas(lat, lon)

    override fun tienePermisoUbicacion(): Boolean = UbicacionAndroid.tienePermiso(app)
    override fun iniciarEntrega(idPedido: Int) = EntregaEnCursoService.iniciar(app, idPedido)
    override fun detenerEntrega() = EntregaEnCursoService.detener(app)
    override val posicionEntrega: StateFlow<Posicion?> get() = EntregaEnCursoService.posicion

    override fun abrirAjustesApp() {
        app.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", app.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    override fun abrirNavegacion(app: AppNavegacion, lat: Double, lon: Double, etiqueta: String) {
        val destino = "$lat,$lon"
        fun abrir(uri: String, paquete: String? = null) = this.app.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply { paquete?.let(::setPackage) }
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        try {
            when (app) {
                AppNavegacion.GOOGLE_MAPS -> abrir("google.navigation:q=$destino", "com.google.android.apps.maps")
                AppNavegacion.WAZE -> abrir("waze://?ll=$destino&navigate=yes")
            }
        } catch (_: ActivityNotFoundException) {
            // Sin la app: cualquier app de mapas (geo:) o el enlace web de Waze.
            when (app) {
                AppNavegacion.GOOGLE_MAPS -> abrir("geo:$destino?q=$destino(${Uri.encode(etiqueta)})")
                AppNavegacion.WAZE -> abrir("https://waze.com/ul?ll=$destino&navigate=yes")
            }
        }
    }
}

/** Visor web de los mapas (Leaflet), conectividad y permisos de Android. */
object UiPlataformaAndroid : UiPlataforma {

    @Composable
    override fun VistaWeb(
        html: String,
        modifier: Modifier,
        interactiva: Boolean,
        alCrear: (ControlWeb) -> Unit,
        alMensaje: (String) -> Unit
    ) = VistaWebAndroid(html, modifier, interactiva, alCrear, alMensaje)

    @Composable
    override fun rememberHayConexion(): State<Boolean> {
        val context = LocalContext.current
        val cm = remember { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
        val estado = remember {
            mutableStateOf(
                cm.getNetworkCapabilities(cm.activeNetwork)
                    ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            )
        }
        DisposableEffect(cm) {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { estado.value = true }
                override fun onLost(network: Network) {
                    estado.value = cm.activeNetwork != null && cm.activeNetwork != network
                }
            }
            cm.registerDefaultNetworkCallback(callback)
            onDispose { cm.unregisterNetworkCallback(callback) }
        }
        return estado
    }

    @Composable
    override fun rememberSolicitudUbicacion(alResponder: (Boolean) -> Unit): () -> Unit {
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
            alResponder(r[Manifest.permission.ACCESS_FINE_LOCATION] == true)
        }
        return {
            launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
}
