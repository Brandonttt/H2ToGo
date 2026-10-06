package com.htogo.app.data.location

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.htogo.app.MainActivity
import com.htogo.app.R
import com.htogo.app.data.api.ApiClient
import com.htogo.app.data.notificaciones.DestinoNotificacion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano (tipo "location") mientras hay una entrega en curso. Mantiene el GPS
 * y el reporte al backend aunque el repartidor cambie a Waze o Google Maps: sin él, Android
 * limita la ubicación de las apps en segundo plano a unas pocas veces por hora y el cliente
 * dejaría de ver al repartidor moverse. Muestra la notificación fija "Entrega en curso".
 *
 * Se inicia desde la pantalla de ruta (con la app visible y el permiso concedido) y se detiene
 * al cerrar la entrega, desde la acción "Terminar" de la notificación o a las 4 h como resguardo.
 */
class EntregaEnCursoService : Service() {

    companion object {
        private const val CANAL = "entrega_en_curso"
        private const val ID_NOTIFICACION = 4201
        private const val EXTRA_PEDIDO = "idPedido"
        private const val ACCION_TERMINAR = "com.htogo.app.TERMINAR_ENTREGA"
        private const val REPORTE_MS = 5_000L
        private const val DURACION_MAX_MS = 4 * 60 * 60 * 1000L

        private val _posicion = MutableStateFlow<Posicion?>(null)
        /** Última posición GPS; la pantalla de ruta la observa para el mapa y la ruta. */
        val posicion: StateFlow<Posicion?> = _posicion.asStateFlow()

        fun iniciar(context: Context, idPedido: Int) {
            val intent = Intent(context, EntregaEnCursoService::class.java).putExtra(EXTRA_PEDIDO, idPedido)
            ContextCompat.startForegroundService(context, intent)
        }

        fun detener(context: Context) {
            context.stopService(Intent(context, EntregaEnCursoService::class.java))
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var seguimiento: Job? = null
    private var idPedidoActual = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACCION_TERMINAR) {
            stopSelf()
            return START_NOT_STICKY
        }
        val idPedido = intent?.getIntExtra(EXTRA_PEDIDO, -1) ?: -1
        // startForeground debe llamarse pronto tras startForegroundService, incluso si ya corría.
        ServiceCompat.startForeground(
            this, ID_NOTIFICACION, notificacion(idPedido),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        )
        if (idPedido != idPedidoActual || seguimiento?.isActive != true) {
            idPedidoActual = idPedido
            iniciarSeguimiento()
        }
        return START_NOT_STICKY
    }

    private fun iniciarSeguimiento() {
        seguimiento?.cancel()
        val api = ApiClient.getInstance(this).repartidorApi
        seguimiento = scope.launch {
            launch { delay(DURACION_MAX_MS); stopSelf() }
            var ultimoReporte = 0L
            UbicacionTracker.posiciones(this@EntregaEnCursoService).collect { p ->
                _posicion.value = p
                val ahora = System.currentTimeMillis()
                if (ahora - ultimoReporte >= REPORTE_MS) {
                    ultimoReporte = ahora
                    // El backend lo reenvía al cliente por WebSocket y evalúa el aviso de 500 m.
                    launch(Dispatchers.IO) {
                        try {
                            api.reportarUbicacion(mapOf("lat" to p.lat, "lon" to p.lon))
                        } catch (_: Exception) {
                            // Sin red: el siguiente fix lo reintenta.
                        }
                    }
                }
            }
        }
    }

    private fun notificacion(idPedido: Int): android.app.Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(CANAL, "Entrega en curso", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Se muestra mientras compartes tu ubicación con el cliente"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
        val abrirRuta = PendingIntent.getActivity(
            this, ID_NOTIFICACION,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(DestinoNotificacion.EXTRA_TIPO, DestinoNotificacion.TIPO_ENTREGA_EN_CURSO)
                .putExtra(DestinoNotificacion.EXTRA_PEDIDO, idPedido.toString()),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val terminar = PendingIntent.getService(
            this, ID_NOTIFICACION + 1,
            Intent(this, EntregaEnCursoService::class.java).setAction(ACCION_TERMINAR),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle("Entrega en curso" + if (idPedido > 0) " · pedido #$idPedido" else "")
            .setContentText("Compartiendo tu ubicación con el cliente")
            .setOngoing(true)
            .setContentIntent(abrirRuta)
            .addAction(0, "Terminar", terminar)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        _posicion.value = null
        super.onDestroy()
    }
}
