package com.htogo.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.htogo.app.data.notificaciones.DestinoNotificacion
import com.htogo.app.data.notificaciones.DispositivoFcm
import com.htogo.app.data.notificaciones.NotificacionesEnVivo
import com.htogo.app.data.notificaciones.Notificador
import androidx.navigation.compose.rememberNavController
import com.htogo.app.navigation.HToGoNavHost
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme

class MainActivity : ComponentActivity() {

    private val pedirPermisoNotificaciones =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* sin permiso: no se muestran */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Notificador.crearCanal(this)
        DestinoNotificacion.desdeIntent(intent)
        if (!Notificador.puedeNotificar(this) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pedirPermisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { HToGoRoot() }
    }

    // App ya abierta y se toca una notificación (launchMode singleTop).
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        DestinoNotificacion.desdeIntent(intent)
    }

    // Notificaciones por WebSocket solo en primer plano; en segundo plano las entrega FCM.
    override fun onStart() {
        super.onStart()
        NotificacionesEnVivo.iniciar(this, lifecycleScope)
        DispositivoFcm.registrar(this)
    }

    override fun onStop() {
        NotificacionesEnVivo.detener()
        super.onStop()
    }
}

@Composable
private fun HToGoRoot() {
    HToGoTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = HToGoColors.Background
        ) {
            val navController = rememberNavController()
            HToGoNavHost(navController = navController)
        }
    }
}
