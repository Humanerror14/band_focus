package com.bandfocus.app.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.bandfocus.app.core.notification.NotificationIds

class DownloadForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        startForeground(NotificationIds.DOWNLOAD_FOREGROUND_ID, buildNotification())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NotificationIds.DOWNLOAD_FOREGROUND_ID, buildNotification())
        return START_STICKY
    }

    private fun buildNotification(): Notification = NotificationCompat.Builder(this, NotificationIds.DOWNLOAD_CHANNEL_ID)
        .setContentTitle("BandFocus")
        .setContentText("Download service is ready")
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setOngoing(true)
        .build()

    companion object {
        const val ACTION_STOP = "com.bandfocus.app.action.STOP_DOWNLOAD_SERVICE"
    }
}
