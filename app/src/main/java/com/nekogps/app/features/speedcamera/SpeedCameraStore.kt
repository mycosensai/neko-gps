package com.nekogps.app.features.speedcamera

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * SharedPreferences-backed storage foundation for [SpeedCameraManager].
 * Owns camera persistence and alert-distance settings so the manager stays
 * focused on proximity alerts and speed-limit queries.
 */
open class SpeedCameraStore(protected val appContext: Context) {

    companion object {
        private const val PREFS_NAME = "speed_cameras"
        private const val KEY_CAMERAS = "cameras"
        private const val KEY_ALERT_DISTANCE = "alert_distance"
        private const val DEFAULT_ALERT_DISTANCE_METERS = 500.0
    }

    protected val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    protected val gson = Gson()

    /**
     * Get all stored speed cameras.
     */
    fun getCameras(): List<SpeedCamera> {
        val json = prefs.getString(KEY_CAMERAS, null) ?: return emptyList()
        val type = object : TypeToken<List<SpeedCamera>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: com.google.gson.JsonSyntaxException) {
            Log.w("SpeedCameraManager", "getCameras: suppressed Exception", e)
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

    protected fun saveCameras(cameras: List<SpeedCamera>) {
        val json = gson.toJson(cameras)
        prefs.edit().putString(KEY_CAMERAS, json).apply()
    }

    /**
     * Initialize with sample speed camera locations.
     * In production, this would load from a database or API.
     */
    protected fun initializeDefaultCameras() {
        saveCameras(DefaultSpeedCameras.cameras())
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
