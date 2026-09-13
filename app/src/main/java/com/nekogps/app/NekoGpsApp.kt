package com.nekogps.app

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class NekoGpsApp : Application() {
    companion object {
        const val CHANNEL_ID = "neko_gps_channel"
        const val CHANNEL_NAME = "GPS Tracking"
        private const val DEFAULT_LATITUDE = 40.7128
        private const val DEFAULT_LONGITUDE = -74.0060
        private const val TAG = "NekoGpsApp"
    }

    private var fusedClient: FusedLocationProviderClient? = null
    private var lastKnownLocation: Location? = null

    override fun onCreate() {
        super.onCreate()
        // osmdroid: load configuration so tile caches live in app-internal
        // storage (the default external path is not writable on API 29+).
        Configuration.getInstance().load(
            this,
            getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = packageName
        createNotificationChannel()
        setupLocationClient()
        fetchLastLocation()
    }

    /** Create the fused client only when Google Play Services is present. */
    private fun setupLocationClient() {
        val gmsAvailable = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS
        if (gmsAvailable) {
            try {
                fusedClient = LocationServices.getFusedLocationProviderClient(this)
            } catch (e: RuntimeException) {
                android.util.Log.w(TAG, "FusedLocationProviderClient unavailable", e)
                fusedClient = null
            }
        }
    }

    private fun fetchLastLocation() {
        val fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!fineGranted) {
            lastKnownLocation = fallbackLocation()
            return
        }

        val fused = fusedClient
        if (fused != null) {
            fused.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) lastKnownLocation = location
                }
                .addOnFailureListener { lastKnownLocation = fallbackLocation() }
        } else {
            // GMS-less device: read the platform's last known fix.
            lastKnownLocation = platformLastKnown() ?: fallbackLocation()
        }
    }

    private fun platformLastKnown(): Location? {
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        for (provider in providers) {
            try {
                if (manager.isProviderEnabled(provider)) {
                    manager.getLastKnownLocation(provider)?.let { return it }
                }
            } catch (e: SecurityException) {
                android.util.Log.w(TAG, "getLastKnownLocation($provider) denied", e)
            } catch (e: RuntimeException) {
                android.util.Log.w(TAG, "getLastKnownLocation($provider) failed", e)
            }
        }
        return null
    }

    private fun fallbackLocation(): Location = Location("").apply {
        latitude = DEFAULT_LATITUDE
        longitude = DEFAULT_LONGITUDE
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
