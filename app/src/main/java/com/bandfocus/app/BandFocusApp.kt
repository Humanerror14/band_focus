package com.bandfocus.app

import android.app.Application
import com.bandfocus.app.core.notification.createDownloadNotificationChannel
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BandFocusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createDownloadNotificationChannel()
    }
}
