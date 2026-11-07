package io.github.omaringa28.yarvis

import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.IBinder

class YarvisService : Service() {

    private val channelId = "yarvis_service"

    override fun onCreate() {
        super.onCreate()
        crearNotificacion()
    }

    private fun crearNotificacion() {
        val manager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Servicio YARVIS",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("YARVIS está activo")
                .setContentText("Servicio de asistente en ejecución")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("YARVIS está activo")
                .setContentText("Servicio de asistente en ejecución")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .build()
        }

        startForeground(1, notification)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
