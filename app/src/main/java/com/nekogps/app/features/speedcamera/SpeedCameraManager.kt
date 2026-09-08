package com.nekogps.app.features.speedcamera

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nekogps.app.utils.DistanceCalculator
import org.osmdroid.util.GeoPoint

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
class SpeedCameraManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val listeners = mutableListOf<SpeedCameraListener>()

    companion object {
        private const val PREFS_NAME = "speed_cameras"
        private const val KEY_CAMERAS = "cameras"
        private const val KEY_ALERT_DISTANCE = "alert_distance"
        private const val DEFAULT_ALERT_DISTANCE_METERS = 500.0

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
     * Get all stored speed cameras.
     */
    fun getCameras(): List<SpeedCamera> {
        val json = prefs.getString(KEY_CAMERAS, null) ?: return emptyList()
        val type = object : TypeToken<List<SpeedCamera>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Add a new speed camera.
     */
    fun addCamera(camera: SpeedCamera) {
        val cameras = getCameras().toMutableList()
        cameras.add(camera)
        saveCameras(cameras)
    }

    /**
     * Remove a speed camera by ID.
     */
    fun removeCamera(cameraId: String) {
        val cameras = getCameras().toMutableList()
        cameras.removeAll { it.id == cameraId }
        saveCameras(cameras)
    }

    /**
     * Get the alert distance threshold in meters.
     */
    fun getAlertDistance(): Double {
        return prefs.getFloat(KEY_ALERT_DISTANCE, DEFAULT_ALERT_DISTANCE_METERS.toFloat()).toDouble()
    }

    /**
     * Set the alert distance threshold in meters.
     */
    fun setAlertDistance(meters: Double) {
        prefs.edit().putFloat(KEY_ALERT_DISTANCE, meters.toFloat()).apply()
    }

    /**
     * Check proximity to speed cameras and trigger alerts.
     * Call this when location updates.
     */
    fun checkProximity(currentLocation: GeoPoint, currentSpeedKmh: Double = 0.0) {
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
            } else if (distance > alertDistance * 1.5) {
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

    private fun saveCameras(cameras: List<SpeedCamera>) {
        val json = gson.toJson(cameras)
        prefs.edit().putString(KEY_CAMERAS, json).apply()
    }

    /**
     * Initialize with sample speed camera locations.
     * In production, this would load from a database or API.
     */
    private fun initializeDefaultCameras() {
        val defaultCameras = listOf(
            // Sample cameras - replace with real data
            SpeedCamera("cam_001", 40.7580, -73.9855, CameraType.FIXED, 50, "Times Square - Fixed Camera"),
            SpeedCamera("cam_002", 40.7484, -73.9857, CameraType.RED_LIGHT, 50, "Empire State - Red Light"),
            SpeedCamera("cam_003", 40.7614, -73.9776, CameraType.MOBILE, 40, "5th Ave - Mobile Zone"),
            SpeedCamera("cam_004", 40.7527, -73.9772, CameraType.AVERAGE_SPEED, 40, "Park Ave - Average Speed"),
            SpeedCamera("cam_005", 40.7061, -74.0087, CameraType.FIXED, 30, "Financial District - Fixed"),
            SpeedCamera("cam_006", 40.7282, -73.9942, CameraType.TRAFFIC_LIGHT, 40, "Union Square - Traffic Light"),
            SpeedCamera("cam_007", 40.7411, -73.9897, CameraType.FIXED, 35, "Flatiron - Fixed Camera"),
            SpeedCamera("cam_008", 40.7589, -73.9851, CameraType.MOBILE, 50, "Broadway - Mobile Zone")
        )
        saveCameras(defaultCameras)
    }

    /**
     * Clear all stored cameras.
     */
    fun clearAllCameras() {
        prefs.edit().remove(KEY_CAMERAS).apply()
    }

    /**
     * Get camera count.
     */
    fun getCameraCount(): Int = getCameras().size
}
