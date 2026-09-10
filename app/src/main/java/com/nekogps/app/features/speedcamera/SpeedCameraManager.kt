package com.nekogps.app.features.speedcamera

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nekogps.app.utils.DistanceCalculator
import org.osmdroid.util.GeoPoint
import android.util.Log

/**
 * Data class representing a speed camera location.
 */
data class SpeedCamera(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val type: CameraType,
    val speedLimitKmh: Int,
    val description: String = ""
)

enum class CameraType {
    FIXED,
    MOBILE,
    RED_LIGHT,
    AVERAGE_SPEED,
    TRAFFIC_LIGHT
}

/**
 * Manages speed camera data and proximity alerts.
 * Stores camera locations in SharedPreferences and provides
 * visual + audio alerts when approaching cameras.
 */
class SpeedCameraManager private constructor(context: Context) : SpeedCameraStore(context) {

    private val listeners = mutableListOf<SpeedCameraListener>()

    companion object {
        private const val PASSED_CAMERA_DISTANCE_FACTOR = 1.5

        @Volatile
        private var instance: SpeedCameraManager? = null

        fun getInstance(context: Context): SpeedCameraManager {
            return instance ?: synchronized(this) {
                instance ?: SpeedCameraManager(context).also { instance = it }
            }
        }
    }

    interface SpeedCameraListener {
        fun onCameraAlert(camera: SpeedCamera, distanceMeters: Double)
        fun onCameraPassed(camera: SpeedCamera)
    }

    init {
        // Initialize with default cameras if none exist
        if (getCameras().isEmpty()) {
            initializeDefaultCameras()
        }
    }

    /**
     * Add a listener for speed camera alerts.
     */
    fun addListener(listener: SpeedCameraListener) {
        listeners.add(listener)
    }

    /**
     * Remove a listener.
     */
    fun removeListener(listener: SpeedCameraListener) {
        listeners.remove(listener)
    }

    /**
     * Check proximity to speed cameras and trigger alerts.
     * Call this when location updates.
     */
    fun checkProximity(currentLocation: GeoPoint) {
        val cameras = getCameras()
        val alertDistance = getAlertDistance()

        for (camera in cameras) {
            val distance = DistanceCalculator.haversineDistance(
                currentLocation.latitude, currentLocation.longitude,
                camera.latitude, camera.longitude
            )

            if (distance <= alertDistance) {
                // Trigger alert
                listeners.forEach { it.onCameraAlert(camera, distance) }
            } else if (distance > alertDistance * PASSED_CAMERA_DISTANCE_FACTOR) {
                // Camera passed
                listeners.forEach { it.onCameraPassed(camera) }
            }
        }
    }

    /**
     * Get the nearest speed camera within alert distance.
     */
    fun getNearestCamera(currentLocation: GeoPoint): Pair<SpeedCamera, Double>? {
        val cameras = getCameras()
        val alertDistance = getAlertDistance()

        var nearest: SpeedCamera? = null
        var nearestDistance = Double.MAX_VALUE

        for (camera in cameras) {
            val distance = DistanceCalculator.haversineDistance(
                currentLocation.latitude, currentLocation.longitude,
                camera.latitude, camera.longitude
            )
            if (distance <= alertDistance && distance < nearestDistance) {
                nearest = camera
                nearestDistance = distance
            }
        }

        return if (nearest != null) Pair(nearest, nearestDistance) else null
    }

    /**
     * Check if current speed exceeds the speed limit of nearby camera.
     */
    fun isSpeeding(currentLocation: GeoPoint, currentSpeedKmh: Double): Boolean {
        val cameras = getCameras()
        val alertDistance = getAlertDistance()

        for (camera in cameras) {
            val distance = DistanceCalculator.haversineDistance(
                currentLocation.latitude, currentLocation.longitude,
                camera.latitude, camera.longitude
            )
            if (distance <= alertDistance && currentSpeedKmh > camera.speedLimitKmh) {
                return true
            }
        }
        return false
    }

    /**
     * Get speed limit at current location (from nearest camera).
     */
    fun getSpeedLimitAtLocation(currentLocation: GeoPoint): Int? {
        val cameras = getCameras()
        val alertDistance = getAlertDistance()

        var nearest: SpeedCamera? = null
        var nearestDistance = Double.MAX_VALUE

        for (camera in cameras) {
            val distance = DistanceCalculator.haversineDistance(
                currentLocation.latitude, currentLocation.longitude,
                camera.latitude, camera.longitude
            )
            if (distance <= alertDistance && distance < nearestDistance) {
                nearest = camera
                nearestDistance = distance
            }
        }

        return nearest?.speedLimitKmh
    }
}
