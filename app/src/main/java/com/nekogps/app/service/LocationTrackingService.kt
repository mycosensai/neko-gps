package com.nekogps.app.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.nekogps.app.MainActivity
import com.nekogps.app.NekoGpsApp
import com.nekogps.app.R
import java.util.concurrent.TimeUnit

/**
 * Foreground service for real-time GPS tracking.
 * - Uses FusedLocationProviderClient for optimal accuracy
 * - Posts persistent notification (required for Android 14+ foreground location)
 * - Emits location updates to registered listeners
 */
class LocationTrackingService : Service() {
    private val binder = LocalBinder()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var listener: LocationListener? = null

    interface LocationListener {
        fun onLocationUpdated(location: Location)
    }

    interface LocationUpdateCallback {
        fun onLocationUpdate(location: Location)
    }

    inner class LocalBinder : Binder() {
        fun getService(): LocationTrackingService = this@LocationTrackingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startForeground(NOTIFICATION_ID, createNotification())
        requestLocationUpdates()
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NekoGpsApp.CHANNEL_ID)
            .setContentTitle("Neko GPS")
            .setContentText("Tracking your location nya~")
            .setSmallIcon(R.drawable.ic_location)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = TimeUnit.SECONDS.toMillis(2)
            fastestInterval = TimeUnit.SECONDS.toMillis(1)
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    listener?.onLocationUpdated(location)
                    Log.d("NekoGPS", "Location: ${location.latitude}, ${location.longitude}, acc=${location.accuracy}m")
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        }
    }

    fun setListener(listener: LocationListener?) {
        this.listener = listener
    }

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}
