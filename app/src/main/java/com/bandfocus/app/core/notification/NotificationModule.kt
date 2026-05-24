package com.bandfocus.app.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

fun Context.createDownloadNotificationChannel() {
    val manager = getSystemService<NotificationManager>() ?: return
    val channel = NotificationChannel(
        NotificationIds.DOWNLOAD_CHANNEL_ID,
        "Downloads",
        NotificationManager.IMPORTANCE_LOW
    ).apply {
        description = "Download progress and foreground service status"
    }
    manager.createNotificationChannel(channel)
}

@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {
    @Provides
    fun provideNotificationManager(@ApplicationContext context: Context): NotificationManager {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        context.createDownloadNotificationChannel()
        return manager
    }
}
