package com.htogo.app.data.notificaciones

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.htogo.app.MainActivity
import com.htogo.app.R
import com.htogo.app.data.local.SessionManager

/**
 * Muestra las notificaciones del sistema, lleguen por WebSocket o por FCM. Descarta duplicados
 * por id: el backend puede reenviar tras un reintento de RabbitMQ, o mandar la misma por los
 * dos canales.
 */
object Notificador {

    /** Mismo id que usa el backend en el payload de FCM (android.notification.channel_id). */
    const val CANAL = "pedidos"
    private const val MAX_RECIENTES = 50
    private val recientes = LinkedHashSet<String>()

    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(CANAL, "Pedidos", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Estado de tus pedidos y avisos de H2ToGo"
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    /** Android 13+ exige el permiso POST_NOTIFICATIONS en tiempo de ejecución. */
    fun puedeNotificar(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** @param datos contexto del backend ({@code tipo}, {@code idPedido}): define qué pantalla abre al tocarla. */
    @Synchronized
    fun mostrar(context: Context, id: String, titulo: String, mensaje: String, datos: Map<String, String> = emptyMap()) {
        if (!recientes.add(id)) return
        if (recientes.size > MAX_RECIENTES) recientes.remove(recientes.first())
        if (!SessionManager.getInstance(context).estanNotificacionesActivas() || !puedeNotificar(context)) return

        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        datos.forEach { (k, v) -> intent.putExtra(k, v) }
        // requestCode distinto por notificación: si no, Android reutiliza el PendingIntent y sus extras.
        val abrirApp = PendingIntent.getActivity(
            context, id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificacion = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(abrirApp)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id.hashCode(), notificacion)
        } catch (_: SecurityException) {
            // El usuario revocó el permiso entre la verificación y el envío.
        }
    }
}
