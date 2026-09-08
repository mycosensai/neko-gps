package com.nekogps.app

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class NekoGpsApp : Application() {
    companion object {
        const val CHANNEL_ID = "neko_gps_channel"
        const val CHANNEL_NAME = "GPS Tracking"
    }

    private var fusedClient: FusedLocationProviderClient? = null
    private var lastKnownLocation: Location? = null

    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().userAgentValue = packageName
        createNotificationChannel()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        fetchLastLocation()
    }

    private fun fetchLastLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedClient?.lastLocation?.addOnSuccessListener { location ->
                lastKnownLocation = location
            }?.addOnFailureListener {
                lastKnownLocation = Location("").apply { latitude = 40.7128; longitude = -74.0060 }
            }
        } else {
            lastKnownLocation = Location("").apply { latitude = 40.7128; longitude = -74.0060 }
        }
    }

    fun getLastKnownLocation(): Location? = lastKnownLocation

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "GPS location tracking notification" }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
