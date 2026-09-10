package com.nekogps.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nekogps.app.features.ElevationProfileView
import com.nekogps.app.features.TrafficLayer
import com.nekogps.app.features.TrafficOverlay
import com.nekogps.app.features.WeatherOverlay
import com.nekogps.app.features.speedcamera.SpeedCamera
import com.nekogps.app.features.speedcamera.SpeedCameraManager
import com.nekogps.app.service.LocationTrackingService
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.ScaleBarOverlay

/**
 * Map-setup and overlay foundation for [MapsActivity].
 * Owns map initialization, feature overlays, camera alerts, service binding,
 * and overlay lifecycle so the activity stays focused on user controls.
 */
open class MapsFoundationActivity : AppCompatActivity() {

    companion object {
        private const val DEFAULT_MAP_ZOOM = 15.0
        private const val MIN_MAP_ZOOM = 3.0
        private const val MAX_MAP_ZOOM = 19.0
        private const val SCALE_BAR_OFFSET_X = 50
        private const val SCALE_BAR_OFFSET_Y = 20
        private const val DEFAULT_LATITUDE = 40.7128
        private const val DEFAULT_LONGITUDE = -74.0060
    }

    protected lateinit var mapView: MapView
    protected var locationOverlay: MyLocationNewOverlay? = null
    protected var compassOverlay: CompassOverlay? = null
    protected var scaleBarOverlay: ScaleBarOverlay? = null

    // Feature overlays
    protected var weatherOverlay: WeatherOverlay? = null
    protected var elevationProfileView: ElevationProfileView? = null
    protected var trafficOverlay: TrafficOverlay? = null
    protected var trafficLegend: TrafficLayer? = null
    protected lateinit var speedCameraManager: SpeedCameraManager
    protected var lastCameraAlertId: String? = null

    protected var locationService: LocationTrackingService? = null
    protected var isBound = false

    protected val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocationTrackingService.LocalBinder
            locationService = binder.getService()
            isBound = true
            Toast.makeText(this@MapsFoundationActivity, "Service connected nya~", Toast.LENGTH_SHORT)
                .show()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            locationService = null
            isBound = false
        }
    }

    protected fun setupMap() {
        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(DEFAULT_MAP_ZOOM)
        mapView.minZoomLevel = MIN_MAP_ZOOM
        mapView.maxZoomLevel = MAX_MAP_ZOOM
        mapView.setHorizontalMapRepetitionEnabled(false)
        mapView.setVerticalMapRepetitionEnabled(false)

        // My location overlay
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView)
        locationOverlay?.enableMyLocation()
        locationOverlay?.enableFollowLocation()
        locationOverlay?.setDrawAccuracyEnabled(true)
        mapView.overlays.add(locationOverlay)

        // Compass
        compassOverlay = CompassOverlay(this, InternalCompassOrientationProvider(this), mapView)
        compassOverlay?.enableCompass()
        mapView.overlays.add(compassOverlay)

        // Scale bar
        scaleBarOverlay = ScaleBarOverlay(mapView)
        scaleBarOverlay?.setAlignBottom(true)
        scaleBarOverlay?.setAlignRight(true)
        scaleBarOverlay?.setScaleBarOffset(SCALE_BAR_OFFSET_X, SCALE_BAR_OFFSET_Y)
        mapView.overlays.add(scaleBarOverlay)

        // Default position (will update with GPS)
        mapView.controller.setCenter(GeoPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)) // NYC
    }

    protected fun setupFeatureOverlays() {
        // Weather overlay
        weatherOverlay = WeatherOverlay(this)
        val weatherContainer = findViewById<LinearLayout>(R.id.weatherContainer)
        weatherContainer?.addView(weatherOverlay)

        // Elevation profile view
        elevationProfileView = ElevationProfileView(this)
        val elevationContainer = findViewById<LinearLayout>(R.id.elevationContainer)
        elevationContainer?.addView(elevationProfileView)

        // Traffic overlay
        trafficOverlay = TrafficOverlay(this)
        trafficLegend = TrafficLayer(this)
    }

    protected fun setupSpeedCameraAlerts() {
        speedCameraManager = SpeedCameraManager.getInstance(this)
        speedCameraManager.addListener(object : SpeedCameraManager.SpeedCameraListener {
            override fun onCameraAlert(camera: SpeedCamera, distanceMeters: Double) {
                if (lastCameraAlertId != camera.id) {
                    lastCameraAlertId = camera.id
                    runOnUiThread {
                        val warningView = findViewById<android.widget.TextView>(
                            R.id.tvSpeedCameraWarning
                        )
                        warningView.visibility = View.VISIBLE
                        warningView.text = "⚠️ Speed camera in " +
                            "${distanceMeters.toInt()}m - Limit: ${camera.speedLimitKmh} km/h"
                    }
                }
            }

            override fun onCameraPassed(camera: SpeedCamera) {
                if (lastCameraAlertId == camera.id) {
                    lastCameraAlertId = null
                    runOnUiThread {
                        findViewById<android.widget.TextView>(R.id.tvSpeedCameraWarning)
                            .visibility = View.GONE
                    }
                }
            }
        })
    }

    protected fun bindLocationService() {
        val intent = Intent(this, LocationTrackingService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        locationOverlay?.enableMyLocation()
        compassOverlay?.enableCompass()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        locationOverlay?.disableMyLocation()
        compassOverlay?.disableCompass()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        weatherOverlay?.cleanup()
    }
}
