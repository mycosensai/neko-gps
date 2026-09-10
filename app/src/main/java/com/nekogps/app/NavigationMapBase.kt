package com.nekogps.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.nekogps.app.databinding.ActivityNavigationBinding
import com.nekogps.app.features.SpeedLimitManager
import com.nekogps.app.features.speedcamera.SpeedCameraManager
import com.nekogps.app.features.voice.TextToSpeechService
import com.nekogps.app.ui.MapStateManager
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.concurrent.TimeUnit

/**
 * Map, location, and alert-manager foundation for [NavigationActivity].
 * Owns map setup, location updates, and shared navigation state so the
 * activity stays focused on route calculation and guidance.
 */
open class NavigationMapBase : AppCompatActivity() {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
        private const val DEFAULT_MAP_ZOOM = 15.0
        private const val TRACK_LINE_WIDTH = 8f
        private const val MS_TO_KMH = 3.6f
        private const val SPEED_WARNING_HIDE_DELAY_MS = 3000L
    }

    protected lateinit var binding: ActivityNavigationBinding
    protected lateinit var fusedLocationClient: FusedLocationProviderClient
    protected lateinit var speedLimitManager: SpeedLimitManager
    protected lateinit var ttsService: TextToSpeechService
    protected lateinit var speedCameraManager: SpeedCameraManager
    protected val handler = Handler(Looper.getMainLooper())

    protected var currentLocation: GeoPoint? = null
    protected var currentSpeed: Float = 0f
    protected var isNavigating = false
    protected var navigationStep = 0

    protected fun setupMap() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)
        binding.mapView.controller.setZoom(DEFAULT_MAP_ZOOM)

        // Restore map state
        MapStateManager.currentLocation.value?.let { loc ->
            currentLocation = loc
            binding.mapView.controller.setCenter(loc)
        }

        MapStateManager.recordedTracks.value.forEach { track ->
            val polyline = Polyline().apply {
                setPoints(track)
                outlinePaint.color = ContextCompat.getColor(
                    this@NavigationMapBase,
                    R.color.lavender_glow
                )
                outlinePaint.strokeWidth = TRACK_LINE_WIDTH
            }
            binding.mapView.overlays.add(polyline)
        }
    }

    protected fun setupSpeedLimitManager() {
        speedLimitManager.onSpeedLimitChanged = { limit ->
            runOnUiThread {
                if (limit != null) {
                    binding.tvSpeedLimit.text = "$limit km/h"
                    binding.tvSpeedLimit.visibility = View.VISIBLE
                    binding.tvSpeedLimitLabel.visibility = View.VISIBLE
                } else {
                    binding.tvSpeedLimit.text = "--"
                }
            }
        }

        speedLimitManager.onSpeedWarning = { speed, limit ->
            runOnUiThread {
                binding.tvSpeedWarning.visibility = View.VISIBLE
                binding.tvSpeedWarning.text = "⚠️ OVER LIMIT: $speed / $limit km/h"
                handler.postDelayed({
                    binding.tvSpeedWarning.visibility = View.GONE
                }, SPEED_WARNING_HIDE_DELAY_MS)
            }
        }
    }

    protected fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST
            )
        } else {
            requestLocationUpdates()
        }
    }

    protected fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = TimeUnit.SECONDS.toMillis(2)
            fastestInterval = TimeUnit.SECONDS.toMillis(1)
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    val loc = result.lastLocation ?: return
                    val geoPoint = GeoPoint(loc.latitude, loc.longitude)
                    currentLocation = geoPoint
                    currentSpeed = loc.speed * MS_TO_KMH // m/s to km/h
                    MapStateManager.updateCurrentLocation(geoPoint)

                    if (MapStateManager.autoCenterEnabled.value) {
                        binding.mapView.controller.animateTo(geoPoint)
                    }

                    renderLocationUpdate(geoPoint)
                    onLocationUpdate(geoPoint)
                }
            },
            Looper.getMainLooper()
        )
    }

    /**
     * Hook for subclasses to react to location updates (alerts, guidance).
     */
    protected open fun onLocationUpdate(point: GeoPoint) {
        // Default: no-op; subclasses override to feed alert managers.
    }

    protected fun renderLocationUpdate(point: GeoPoint) {
        binding.mapView.overlays.removeAll { it is Marker && it.id == "current_location" }
        val marker = Marker(binding.mapView).apply {
            id = "current_location"
            position = point
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Current Location"
        }
        binding.mapView.overlays.add(marker)
        binding.mapView.invalidate()

        runOnUiThread {
            binding.tvSpeed.text = "${currentSpeed.toInt()} km/h"
        }
    }

    protected fun stopNavigationSession() {
        isNavigating = false
        handler.removeCallbacksAndMessages(null)
        binding.btnStopNav.visibility = View.GONE
        binding.btnStartNav.visibility = View.VISIBLE
        binding.tvInstruction.text = "Navigation stopped"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationUpdates()
        } else {
            android.widget.Toast.makeText(
                this,
                "Location permission required for navigation",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
        isNavigating = false
        handler.removeCallbacksAndMessages(null)
    }
}
