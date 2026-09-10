package com.nekogps.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationRequest
import com.nekogps.app.databinding.ActivityNavigationBinding
import com.nekogps.app.features.SpeedLimitManager
import com.nekogps.app.features.speedcamera.SpeedCamera
import com.nekogps.app.features.speedcamera.SpeedCameraManager
import com.nekogps.app.features.voice.TextToSpeechService
import com.nekogps.app.utils.DistanceCalculator
import com.nekogps.app.ui.MapStateManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Turn-by-turn navigation screen.
 * Features: osmdroid map, destination input, route calculation using waypoints,
 * distance/ETA display, simulated navigation instructions, and speed limit display.
 */
class NavigationActivity : NavigationMapBase() {

    private val waypoints = mutableListOf<GeoPoint>()
    private var currentRoute: Polyline? = null
    private var destinationMarker: Marker? = null
    private var lastSpokenInstruction: String? = null
    private var lastCameraAlertId: String? = null

    companion object {
        private const val ASSUMED_AVERAGE_SPEED_KMH = 50.0
        private const val ROUTE_LINE_WIDTH = 10f
        private const val ROUTE_FIT_PADDING_PX = 100
        private const val NAVIGATION_STEP_DELAY_MS = 3000L
    }

    /**
     * Validated inputs for building a route.
     */
    private data class RouteInput(
        val origin: GeoPoint,
        val destination: GeoPoint,
        val destinationText: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Configuration.getInstance().userAgentValue = packageName

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        speedLimitManager = SpeedLimitManager()
        ttsService = TextToSpeechService(this)
        speedCameraManager = SpeedCameraManager.getInstance(this)

        setupMap()

        binding.btnSearch.setOnClickListener {
            calculateRoute()
        }

        binding.btnStartNav.setOnClickListener {
            startNavigation()
        }

        binding.btnStopNav.setOnClickListener {
            stopNavigationSession()
        }

        setupSpeedLimitManager()
        setupSpeedCameraAlerts()
        checkLocationPermission()
    }

    private fun setupSpeedCameraAlerts() {
        speedCameraManager.addListener(object : SpeedCameraManager.SpeedCameraListener {
            override fun onCameraAlert(camera: SpeedCamera, distanceMeters: Double) {
                if (lastCameraAlertId != camera.id) {
                    lastCameraAlertId = camera.id
                    runOnUiThread {
                        binding.tvSpeedCameraWarning.visibility = View.VISIBLE
                        binding.tvSpeedCameraWarning.text = "⚠️ Speed camera in " +
                            "${distanceMeters.toInt()}m - Limit: ${camera.speedLimitKmh} km/h"
                        ttsService.speakSpeedCameraAlert(distanceMeters.toInt())
                    }
                }
            }

            override fun onCameraPassed(camera: SpeedCamera) {
                if (lastCameraAlertId == camera.id) {
                    lastCameraAlertId = null
                    runOnUiThread {
                        binding.tvSpeedCameraWarning.visibility = View.GONE
                    }
                }
            }
        })
    }

    override fun onLocationUpdate(point: GeoPoint) {
        speedLimitManager.updateLocation(point, currentSpeed)
        speedCameraManager.checkProximity(point)
    }

    private fun calculateRoute() {
        val routeInput = resolveRouteInput() ?: return

        // Remove old route
        currentRoute?.let { binding.mapView.overlays.remove(it) }
        destinationMarker?.let { binding.mapView.overlays.remove(it) }

        // Build route with waypoints
        val routePoints = mutableListOf<GeoPoint>()
        routePoints.add(routeInput.origin)
        routePoints.addAll(waypoints)
        routePoints.add(routeInput.destination)

        // Draw polyline
        val polyline = Polyline().apply {
            setPoints(routePoints)
            outlinePaint.color = ContextCompat.getColor(this@NavigationActivity, R.color.lavender_glow)
            outlinePaint.strokeWidth = ROUTE_LINE_WIDTH
        }
        binding.mapView.overlays.add(polyline)
        currentRoute = polyline

        // Add destination marker
        val marker = Marker(binding.mapView).apply {
            position = routeInput.destination
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Destination"
            snippet = routeInput.destinationText
        }
        binding.mapView.overlays.add(marker)
        destinationMarker = marker

        // Calculate total distance
        var totalDistance = 0.0
        for (i in 0 until routePoints.size - 1) {
            totalDistance += DistanceCalculator.haversineDistance(
                routePoints[i].latitude, routePoints[i].longitude,
                routePoints[i + 1].latitude, routePoints[i + 1].longitude
            )
        }

        // Display stats
        val etaMinutes = DistanceCalculator.estimateETA(totalDistance, ASSUMED_AVERAGE_SPEED_KMH)

        binding.tvDistance.text = DistanceCalculator.formatDistance(totalDistance)
        binding.tvEta.text = DistanceCalculator.formatETA(etaMinutes)
        binding.tvInstruction.text = "Route: ${routePoints.size} points via ${waypoints.size} waypoints"

        // Fit map to route
        val boundingBox = polyline.bounds
        binding.mapView.zoomToBoundingBox(boundingBox, true, ROUTE_FIT_PADDING_PX)

        val routeSummary = "Route calculated: ${DistanceCalculator.formatDistance(totalDistance)}"
        Toast.makeText(this, routeSummary, Toast.LENGTH_SHORT).show()
    }

    private fun resolveRouteInput(): RouteInput? {
        val destinationText = binding.etDestination.text.toString().trim()
        val origin = currentLocation
        val destPoint = if (origin == null) null else DestinationParser.parse(destinationText)
        if (destinationText.isEmpty() || origin == null || destPoint == null) {
            val error = if (destinationText.isEmpty()) {
                "Please enter a destination"
            } else if (origin == null) {
                "Waiting for GPS fix..."
            } else {
                "Invalid destination format. Use: lat,lng"
            }
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            return null
        }
        return RouteInput(origin, destPoint, destinationText)
    }

    private fun startNavigation() {
        if (currentRoute == null) {
            Toast.makeText(this, "Calculate a route first", Toast.LENGTH_SHORT).show()
            return
        }
        if (currentLocation == null) {
            Toast.makeText(this, "Waiting for GPS fix...", Toast.LENGTH_SHORT).show()
            return
        }

        isNavigating = true
        navigationStep = 0
        binding.btnStartNav.visibility = View.GONE
        binding.btnStopNav.visibility = View.VISIBLE
        binding.tvInstruction.visibility = View.VISIBLE

        simulateNavigation()
    }

    private fun simulateNavigation() {
        val routePoints = if (isNavigating) currentRoute?.actualPoints else null
        if (routePoints == null) return
        if (navigationStep >= routePoints.size - 1) {
            finishNavigationArrival()
            return
        }
        advanceNavigationStep(routePoints)
    }

    private fun advanceNavigationStep(routePoints: List<GeoPoint>) {
        val current = routePoints[navigationStep]
        val next = routePoints[navigationStep + 1]

        val distance = DistanceCalculator.haversineDistance(
            current.latitude, current.longitude,
            next.latitude, next.longitude
        )
        val bearing = DistanceCalculator.calculateBearing(
            current.latitude, current.longitude,
            next.latitude, next.longitude
        )

        // Generate instruction based on bearing
        val direction = BearingDirections.toDirection(bearing)

        val instruction = "$direction for ${DistanceCalculator.formatDistance(distance)}, then continue to next point."
        binding.tvInstruction.text = instruction

        // Speak the instruction via TTS
        if (instruction != lastSpokenInstruction) {
            lastSpokenInstruction = instruction
            ttsService.speakInstruction(instruction)
        }

        handler.postDelayed({
            navigationStep++
            simulateNavigation()
        }, NAVIGATION_STEP_DELAY_MS)
    }

    private fun finishNavigationArrival() {
        binding.tvInstruction.text = "You have arrived at your destination!"
        isNavigating = false
        binding.btnStopNav.visibility = View.GONE
        binding.btnStartNav.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        speedLimitManager.cleanup()
    }
}
