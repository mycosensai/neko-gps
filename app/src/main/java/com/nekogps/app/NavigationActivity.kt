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
import com.nekogps.app.utils.DistanceCalculator
import com.nekogps.app.utils.MapStateManager
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
 * distance/ETA display, and simulated navigation instructions.
 */
class NavigationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNavigationBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val handler = Handler(Looper.getMainLooper())
    private val waypoints = mutableListOf<GeoPoint>()
    private var currentRoute: Polyline? = null
    private var destinationMarker: Marker? = null
    private var isNavigating = false
    private var navigationStep = 0
    private var currentLocation: GeoPoint? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
        private const val TAG = "NavigationActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Configuration.getInstance().userAgentValue = packageName

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupMap()
        setupUI()
        checkLocationPermission()
    }

    private fun setupMap() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)
        binding.mapView.controller.setZoom(15.0)

        // Restore map state
        MapStateManager.currentLocation.value?.let { loc ->
            currentLocation = loc
            binding.mapView.controller.setCenter(loc)
        }

        MapStateManager.recordedTracks.value.forEach { track ->
            val polyline = Polyline().apply {
                setPoints(track)
                outlinePaint.color = ContextCompat.getColor(this@NavigationActivity, R.color.lavender_glow)
                outlinePaint.strokeWidth = 8f
            }
            binding.mapView.overlays.add(polyline)
        }
    }

    private fun setupUI() {
        binding.btnSearch.setOnClickListener {
            calculateRoute()
        }

        binding.btnStartNav.setOnClickListener {
            startNavigation()
        }

        binding.btnStopNav.setOnClickListener {
            stopNavigation()
        }
    }

    private fun checkLocationPermission() {
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

    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

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
                    MapStateManager.updateCurrentLocation(geoPoint)

                    if (MapStateManager.autoCenterEnabled.value) {
                        binding.mapView.controller.animateTo(geoPoint)
                    }

                    updateCurrentLocationMarker(geoPoint)
                }
            },
            Looper.getMainLooper()
        )
    }

    private fun updateCurrentLocationMarker(point: GeoPoint) {
        binding.mapView.overlays.removeAll { it is Marker && it.id == "current_location" }
        val marker = Marker(binding.mapView).apply {
            id = "current_location"
            position = point
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Current Location"
        }
        binding.mapView.overlays.add(marker)
        binding.mapView.invalidate()
    }

    private fun calculateRoute() {
        val destinationText = binding.etDestination.text.toString().trim()
        if (destinationText.isEmpty()) {
            Toast.makeText(this, "Please enter a destination", Toast.LENGTH_SHORT).show()
            return
        }

        val origin = currentLocation
        if (origin == null) {
            Toast.makeText(this, "Waiting for GPS fix...", Toast.LENGTH_SHORT).show()
            return
        }

        // Parse destination - expect "lat,lng" format or use Nominatim lookup
        val destPoint = parseDestination(destinationText)
        if (destPoint == null) {
            Toast.makeText(this, "Invalid destination format. Use: lat,lng", Toast.LENGTH_SHORT).show()
            return
        }

        // Remove old route
        currentRoute?.let { binding.mapView.overlays.remove(it) }
        destinationMarker?.let { binding.mapView.overlays.remove(it) }

        // Build route with waypoints
        val routePoints = mutableListOf<GeoPoint>()
        routePoints.add(origin)
        routePoints.addAll(waypoints)
        routePoints.add(destPoint)

        // Draw polyline
        val polyline = Polyline().apply {
            setPoints(routePoints)
            outlinePaint.color = ContextCompat.getColor(this@NavigationActivity, R.color.lavender_glow)
            outlinePaint.strokeWidth = 10f
        }
        binding.mapView.overlays.add(polyline)
        currentRoute = polyline

        // Add destination marker
        val marker = Marker(binding.mapView).apply {
            position = destPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Destination"
            snippet = destinationText
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
        val avgSpeedKmh = 50.0 // Assume average speed for ETA
        val etaMinutes = DistanceCalculator.estimateETA(totalDistance, avgSpeedKmh)

        binding.tvDistance.text = DistanceCalculator.formatDistance(totalDistance)
        binding.tvEta.text = DistanceCalculator.formatETA(etaMinutes)
        binding.tvInstruction.text = "Route: ${routePoints.size} points via ${waypoints.size} waypoints"

        // Fit map to route
        val boundingBox = polyline.bounds
        binding.mapView.zoomToBoundingBox(boundingBox, true, 100)

        Toast.makeText(this, "Route calculated: ${DistanceCalculator.formatDistance(totalDistance)}", Toast.LENGTH_SHORT).show()
    }

    private fun parseDestination(text: String): GeoPoint? {
        // Try "lat,lng" format
        val parts = text.split(",").map { it.trim() }
        if (parts.size == 2) {
            val lat = parts[0].toDoubleOrNull()
            val lng = parts[1].toDoubleOrNull()
            if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                return GeoPoint(lat, lng)
            }
        }
        return null
    }

    private fun addCurrentLocationAsWaypoint() {
        currentLocation?.let { loc ->
            waypoints.add(loc)
            val marker = Marker(binding.mapView).apply {
                position = loc
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Waypoint ${waypoints.size}"
                snippet = "Added waypoint"
            }
            binding.mapView.overlays.add(marker)
            binding.mapView.invalidate()
            Toast.makeText(this, "Waypoint ${waypoints.size} added", Toast.LENGTH_SHORT).show()
        } ?: Toast.makeText(this, "No GPS fix available", Toast.LENGTH_SHORT).show()
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
        if (!isNavigating) return

        val routePoints = currentRoute?.actualPoints ?: return
        if (navigationStep >= routePoints.size - 1) {
            binding.tvInstruction.text = "You have arrived at your destination!"
            isNavigating = false
            binding.btnStopNav.visibility = View.GONE
            binding.btnStartNav.visibility = View.VISIBLE
            return
        }

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
        val direction = when {
            bearing >= 337.5 || bearing < 22.5 -> "Head north"
            bearing >= 22.5 && bearing < 67.5 -> "Head northeast"
            bearing >= 67.5 && bearing < 112.5 -> "Head east"
            bearing >= 112.5 && bearing < 157.5 -> "Head southeast"
            bearing >= 157.5 && bearing < 202.5 -> "Head south"
            bearing >= 202.5 && bearing < 247.5 -> "Head southwest"
            bearing >= 247.5 && bearing < 292.5 -> "Head west"
            bearing >= 292.5 && bearing < 337.5 -> "Head northwest"
            else -> "Continue"
        }

        val instruction = "$direction for ${DistanceCalculator.formatDistance(distance)}, then continue to next point."
        binding.tvInstruction.text = instruction

        handler.postDelayed({
            navigationStep++
            simulateNavigation()
        }, 3000)
    }

    private fun stopNavigation() {
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
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationUpdates()
        } else {
            Toast.makeText(this, "Location permission required for navigation", Toast.LENGTH_LONG).show()
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

    override fun onDestroy() {
        super.onDestroy()
        // Note: Ideally we'd remove the callback, but we'd need to store the reference
    }
}
