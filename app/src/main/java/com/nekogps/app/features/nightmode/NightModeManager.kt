package com.nekogps.app.features.nightmode

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs

/**
 * NightModeManager - auto-switches to night map style based on time (sunset/sunrise calculation)
 * or ambient light sensor. Provides darker color scheme for map tiles.
 */
class NightModeManager(private val context: Context) {

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private var lightSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)

    private var isNightMode = false
    private var lightSensorListener: SensorEventListener? = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                if (it.sensor.type == Sensor.TYPE_LIGHT) {
                    val lux = it.values[0]
                    // Below 10 lux is considered dark
                    if (lux < LIGHT_SENSOR_THRESHOLD && !isNightMode) {
                        enableNightMode()
                    } else if (lux >= LIGHT_SENSOR_THRESHOLD && isNightMode) {
                        disableNightMode()
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun isNightModeEnabled(): Boolean = isNightMode

    fun enableNightMode() {
        isNightMode = true
        // Apply night mode tile source or overlay
    }

    fun disableNightMode() {
        isNightMode = false
        // Revert to day mode tile source
    }

    fun toggleNightMode() {
        if (isNightMode) disableNightMode() else enableNightMode()
    }

    /**
     * Calculate if it's night based on time of day and approximate sunrise/sunset.
     * Uses simplified calculation based on latitude/longitude.
     */
    fun isNightTime(
        latitude: Double,
        longitude: Double,
        timeInMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = timeInMillis

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val month = calendar.get(Calendar.MONTH)

        // Simplified sunrise/sunset calculation
        // Varies by latitude and season
        val sunriseHour = calculateSunrise(latitude, month)
        val sunsetHour = calculateSunset(latitude, month)

        return hour < sunriseHour || hour >= sunsetHour
    }

    private fun calculateSunrise(latitude: Double, month: Int): Int {
        // Simplified: sunrise between 5-8 AM depending on latitude and season
        val baseSunrise = 6
        val latOffset = (abs(latitude) / 30).toInt().coerceAtMost(2)
        val seasonalOffset = when (month) {
            in 3..5 -> -1 // Spring: earlier sunrise
            in 6..8 -> -2 // Summer: earliest sunrise
            in 9..11 -> 0 // Fall: normal
            else -> 1 // Winter: later sunrise
        }
        return (baseSunrise + latOffset + seasonalOffset).coerceIn(4, 9)
    }

    private fun calculateSunset(latitude: Double, month: Int): Int {
        // Simplified: sunset between 5-9 PM depending on latitude and season
        val baseSunset = 18
        val latOffset = (abs(latitude) / 30).toInt().coerceAtMost(2)
        val seasonalOffset = when (month) {
            in 3..5 -> 1 // Spring: later sunset
            in 6..8 -> 2 // Summer: latest sunset
            in 9..11 -> 0 // Fall: normal
            else -> -1 // Winter: earlier sunset
        }
        return (baseSunset + latOffset + seasonalOffset).coerceIn(16, 22)
    }

    /**
     * Get the night mode tile source for osmdroid.
     * Returns a darker tile source configuration.
     */
    fun getNightTileSource(): NightTileSource {
        return NightTileSource()
    }

    /**
     * Get night mode color scheme for UI elements.
     */
    fun getNightColors(): NightColors {
        return NightColors(
            backgroundColor = 0x121212,
            surfaceColor = 0x1E1E1E,
            textPrimaryColor = 0xE0E0E0,
            textSecondaryColor = 0x9E9E9E,
            accentColor = 0x7C4DFF,
            mapOverlayColor = 0x80000000.toInt()
        )
    }

    fun startLightSensor() {
        lightSensor?.let { sensor ->
            lightSensorListener?.let { listener ->
                sensorManager?.registerListener(
                    listener,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }
        }
    }

    fun stopLightSensor() {
        lightSensorListener?.let { listener ->
            sensorManager?.unregisterListener(listener)
        }
    }

    fun isLightSensorAvailable(): Boolean = lightSensor != null

    companion object {
        private const val LIGHT_SENSOR_THRESHOLD = 10.0f // lux
    }
}

/**
 * Night mode tile source configuration.
 */
class NightTileSource {
    val tileSourceName = "Night Mode"
    val requiresDarkOverlay = true
    val overlayAlpha = 0.5f
}

/**
 * Night mode color scheme.
 */
data class NightColors(
    val backgroundColor: Int,
    val surfaceColor: Int,
    val textPrimaryColor: Int,
    val textSecondaryColor: Int,
    val accentColor: Int,
    val mapOverlayColor: Int
)
