package com.nekogps.app.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.nekogps.app.MainActivity
import com.nekogps.app.NekoGpsApp
import com.nekogps.app.R
import java.util.concurrent.TimeUnit

/**
 * Foreground service for real-time GPS tracking.
 * - Uses FusedLocationProviderClient when Google Play Services is available,
 *   falling back to the platform LocationManager on GMS-less devices.
 * - Posts a persistent notification (required for Android 14+ foreground
 *   location) and emits updates to bound listeners.
 *
 * Must be started with `startForegroundService` and bound for the client API.
 */
class LocationTrackingService : Service() {

    private val binder = LocalBinder()
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationManager: LocationManager? = null
    private var locationCallback: LocationCallback? = null
    private var gpsListener: android.location.LocationListener? = null
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
        startForeground(NOTIFICATION_ID, createNotification())
        setupLocationProvider()
        requestLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Notification action: stop tracking.
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Keep the tracker alive across process restarts; re-run the location
        // request in case the callback was lost with the process.
        requestLocationUpdates()
        return START_STICKY
    }

    /** Prefer Play Services; degrade to the platform provider when absent. */
    private fun setupLocationProvider() {
        val gmsAvailable = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS
        if (gmsAvailable) {
            try {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
                return
            } catch (e: RuntimeException) {
                Log.w(TAG, "FusedLocationProviderClient unavailable, falling back to LocationManager", e)
                fusedLocationClient = null
            }
        }
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, LocationTrackingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NekoGpsApp.CHANNEL_ID)
            .setContentTitle("Neko GPS")
            .setContentText("Tracking your location nya~")
            .setSmallIcon(R.drawable.ic_location)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(0, "Stop tracking", stopIntent)
            .build()
    }

    private fun requestLocationUpdates() {
        val fineGranted = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted) return

        val fused = fusedLocationClient
        if (fused != null) {
            val locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = TimeUnit.SECONDS.toMillis(2)
                fastestInterval = TimeUnit.SECONDS.toMillis(1)
            }
            locationCallback?.let { fused.removeLocationUpdates(it) }
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { emitLocation(it) }
                }
            }
            try {
                fused.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
            } catch (e: RuntimeException) {
                Log.w(TAG, "Fused location updates failed, falling back to LocationManager", e)
                fusedLocationClient = null
                requestLocationUpdates()
            }
        } else {
            startPlatformUpdates()
        }
    }

    /** LocationManager-based tracking for GMS-less devices. */
    private fun startPlatformUpdates() {
        val manager = locationManager ?: return
        val provider = if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            LocationManager.GPS_PROVIDER
        } else {
            LocationManager.NETWORK_PROVIDER
        }
        gpsListener = object : android.location.LocationListener {
            override fun onLocationChanged(location: Location) {
                emitLocation(location)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) = Unit
        }
        try {
            manager.requestLocationUpdates(provider, 2000L, 1f, gpsListener!!, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.w(TAG, "Platform location updates denied", e)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Platform provider unavailable: $provider", e)
        }
    }

    private fun emitLocation(location: Location) {
        listener?.onLocationUpdated(location)
        Log.d(TAG, "Location: ${location.latitude}, ${location.longitude}, acc=${location.accuracy}m")
    }

    fun setListener(listener: LocationListener?) {
        this.listener = listener
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
            gpsListener?.let { locationManager?.removeUpdates(it) }
        } catch (e: RuntimeException) {
            Log.w(TAG, "Location cleanup failed", e)
        }
        locationCallback = null
        gpsListener = null
        listener = null
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.nekogps.app.action.STOP_TRACKING"
        private const val TAG = "LocationTrackingService"

        /** Convenience stop for UI callers. */
        fun stop(context: Context) {
            context.stopService(Intent(context, LocationTrackingService::class.java))
        }
    }
}
