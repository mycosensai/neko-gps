package com.nekogps.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import org.osmdroid.config.Configuration

class NekoGpsApp : Application() {
    companion object {
        const val CHANNEL_ID = "neko_gps_channel"
        const val CHANNEL_NAME = "GPS Tracking"
    }

    override fun onCreate() {
        super.onCreate()
        // Configure osmdroid
        Configuration.getInstance().userAgentValue = packageName
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "GPS location tracking notification"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
