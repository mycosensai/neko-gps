package com.nekogps.app.features.safety

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.nekogps.app.utils.DistanceCalculator
import org.osmdroid.util.GeoPoint
import java.util.*

/**
 * Fatigue Detection Manager - Tracks driving time, suggests breaks every 2 hours.
 * Shows alerts with rest stop suggestions.
 */
class FatigueDetectionManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "FatigueDetectionManager"
        private const val PREFS_NAME = "fatigue_detection"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_BREAK_INTERVAL_MINUTES = "break_interval_minutes"
        private const val KEY_WARNING_BEFORE_MINUTES = "warning_before_minutes"
        private const val KEY_DRIVING_START_TIME = "driving_start_time"
        private const val KEY_TOTAL_DRIVING_TIME_MS = "total_driving_time_ms"
        private const val KEY_LAST_BREAK_TIME = "last_break_time"
        private const val KEY_PAUSED_TIME_MS = "paused_time_ms"
        private const val KEY_IS_PAUSED = "is_paused"
        private const val KEY_AUTO_PAUSE_THRESHOLD_KMH = "auto_pause_threshold_kmh"
        private const val KEY_SHOW_REST_STOPS = "show_rest_stops"

        private const val DEFAULT_BREAK_INTERVAL_MINUTES = 120 // 2 hours
        private const val DEFAULT_WARNING_BEFORE_MINUTES = 15
        private const val DEFAULT_AUTO_PAUSE_THRESHOLD_KMH = 5.0f
        private const val MIN_SPEED_FOR_DRIVING_KMH = 10.0f

        @Volatile
        private var instance: FatigueDetectionManager? = null

        fun getInstance(context: Context): FatigueDetectionManager {
            return instance ?: synchronized(this) {
                instance ?: FatigueDetectionManager(context).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())

    // State
    private var isMonitoring = false
    private var isPaused = false
    private var drivingStartTime: Long = 0
    private var totalDrivingTimeMs: Long = 0
    private var lastBreakTime: Long = 0
    private var pausedTimeMs: Long = 0
    private var currentSpeedKmh = 0f
    private var currentLocation: Location? = null
    private var breakAlertShown = false
    private var warningAlertShown = false

    // Auto-pause detection
    private var lastMovementTime = 0L
    private var wasMoving = false

    interface FatigueDetectionListener {
        fun onDrivingStarted()
        fun onDrivingPaused()
        fun onDrivingResumed()
        fun onDrivingStopped(totalTimeMs: Long)
        fun onBreakWarning(minutesRemaining: Int)
        fun onBreakRequired(totalDrivingMinutes: Int)
        fun onBreakTaken()
        fun onRestStopsFound(restStops: List<RestStop>)
        fun onSettingsChanged()
        fun onDrivingTimeUpdate(minutes: Int)
    }

    private val listeners = mutableListOf<FatigueDetectionListener>()

    data class RestStop(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val distanceKm: Double,
        val address: String = "",
        val type: RestStopType = RestStopType.REST_AREA
    ) {
        enum class RestStopType {
            REST_AREA, GAS_STATION, RESTAURANT, HOTEL, PARKING
        }
    }

    init {
        loadState()
    }

    private fun loadState() {
        isPaused = prefs.getBoolean(KEY_IS_PAUSED, false)
        drivingStartTime = prefs.getLong(KEY_DRIVING_START_TIME, 0)
        totalDrivingTimeMs = prefs.getLong(KEY_TOTAL_DRIVING_TIME_MS, 0)
        lastBreakTime = prefs.getLong(KEY_LAST_BREAK_TIME, 0)
        pausedTimeMs = prefs.getLong(KEY_PAUSED_TIME_MS, 0)

        // If we were driving, check if we should auto-pause
        if (drivingStartTime > 0L && !isPaused) {
            // Resume tracking
            lastMovementTime = System.currentTimeMillis()
        }
    }

    private fun saveState() {
        prefs.edit()
            .putBoolean(KEY_IS_PAUSED, isPaused)
            .putLong(KEY_DRIVING_START_TIME, drivingStartTime)
            .putLong(KEY_TOTAL_DRIVING_TIME_MS, totalDrivingTimeMs)
            .putLong(KEY_LAST_BREAK_TIME, lastBreakTime)
            .putLong(KEY_PAUSED_TIME_MS, pausedTimeMs)
            .apply()
    }

    fun addListener(listener: FatigueDetectionListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: FatigueDetectionListener) {
        listeners.remove(listener)
    }

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, true)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (!enabled) {
            stopMonitoring()
        }
        listeners.forEach { it.onSettingsChanged() }
    }

    fun getBreakIntervalMinutes(): Int = prefs.getInt(KEY_BREAK_INTERVAL_MINUTES, DEFAULT_BREAK_INTERVAL_MINUTES)

    fun setBreakIntervalMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(MIN_BREAK_INTERVAL_MINUTES, MAX_BREAK_INTERVAL_MINUTES)
        prefs.edit().putInt(KEY_BREAK_INTERVAL_MINUTES, clamped).apply()
        listeners.forEach { it.onSettingsChanged() }
    }

    fun getWarningBeforeMinutes(): Int = prefs.getInt(KEY_WARNING_BEFORE_MINUTES, DEFAULT_WARNING_BEFORE_MINUTES)

    fun setWarningBeforeMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(MIN_WARNING_BEFORE_MINUTES, MAX_WARNING_BEFORE_MINUTES)
        prefs.edit().putInt(KEY_WARNING_BEFORE_MINUTES, clamped).apply()
        listeners.forEach { it.onSettingsChanged() }
    }

    fun getAutoPauseThresholdKmh(): Float = prefs.getFloat(KEY_AUTO_PAUSE_THRESHOLD_KMH, DEFAULT_AUTO_PAUSE_THRESHOLD_KMH)

    fun setAutoPauseThresholdKmh(threshold: Float) {
        val clamped = threshold.coerceIn(MIN_AUTO_PAUSE_THRESHOLD_KMH, MAX_AUTO_PAUSE_THRESHOLD_KMH)
        prefs.edit().putFloat(KEY_AUTO_PAUSE_THRESHOLD_KMH, clamped).apply()
        listeners.forEach { it.onSettingsChanged() }
    }

    fun getShowRestStops(): Boolean = prefs.getBoolean(KEY_SHOW_REST_STOPS, true)

    fun setShowRestStops(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_REST_STOPS, enabled).apply()
        listeners.forEach { it.onSettingsChanged() }
    }

    fun startMonitoring() {
        if (isMonitoring || !isEnabled()) return
        isMonitoring = true
        lastMovementTime = System.currentTimeMillis()
        wasMoving = false
        updateDrivingState()
        Log.d(TAG, "Fatigue detection monitoring started")
    }

    fun stopMonitoring() {
        if (!isMonitoring) return
        isMonitoring = false
        if (drivingStartTime > 0L && !isPaused) {
            // Finalize driving session
            val sessionTime = System.currentTimeMillis() - drivingStartTime
            totalDrivingTimeMs += sessionTime
            drivingStartTime = 0
            saveState()
            listeners.forEach { it.onDrivingStopped(totalDrivingTimeMs) }
        }
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "Fatigue detection monitoring stopped")
    }

    fun isMonitoring(): Boolean = isMonitoring

    fun onLocationUpdate(location: Location, speedKmh: Float) {
        if (!isMonitoring || !isEnabled()) return

        currentLocation = location
        currentSpeedKmh = speedKmh
        val now = System.currentTimeMillis()

        val isMoving = speedKmh >= MIN_SPEED_FOR_DRIVING_KMH

        // Auto-pause/resume logic
        if (isMoving && !wasMoving) {
            // Started moving
            if (isPaused) {
                resumeDriving()
            } else if (drivingStartTime == 0L) {
                startDriving()
            }
            lastMovementTime = now
        } else if (!isMoving && wasMoving) {
            // Stopped moving - check if should auto-pause
            handler.postDelayed(autoPauseCheck, MILLIS_PER_MINUTE)
        } else if (isMoving) {
            lastMovementTime = now
        }

        wasMoving = isMoving

        // Update driving time
        updateDrivingTime()

        // Check for break warnings
        checkBreakWarnings()

        // Find rest stops if enabled and break is due
        if (getShowRestStops() && shouldSuggestRestStops()) {
            findNearbyRestStops()
        }
    }

    private fun updateDrivingState() {
        if (currentSpeedKmh >= MIN_SPEED_FOR_DRIVING_KMH && drivingStartTime == 0L && !isPaused) {
            startDriving()
        } else if (currentSpeedKmh < getAutoPauseThresholdKmh() && drivingStartTime > 0L && !isPaused) {
            // Will be handled by auto-pause check
        }
    }

    private fun startDriving() {
        drivingStartTime = System.currentTimeMillis()
        isPaused = false
        breakAlertShown = false
        warningAlertShown = false
        saveState()
        listeners.forEach { it.onDrivingStarted() }
        Log.d(TAG, "Driving session started")
        startPeriodicUpdates()
    }

    private fun pauseDriving() {
        if (drivingStartTime == 0L || isPaused) return

        val sessionTime = System.currentTimeMillis() - drivingStartTime
        totalDrivingTimeMs += sessionTime
        drivingStartTime = 0
        isPaused = true
        pausedTimeMs = 0
        saveState()
        listeners.forEach { it.onDrivingPaused() }
        Log.d(TAG, "Driving paused, total: ${totalDrivingTimeMs / MILLIS_PER_MINUTE} min")
    }

    private fun resumeDriving() {
        if (drivingStartTime > 0L || !isPaused) return

        isPaused = false
        drivingStartTime = System.currentTimeMillis()
        saveState()
        listeners.forEach { it.onDrivingResumed() }
        Log.d(TAG, "Driving resumed")
    }

    private val autoPauseCheck = Runnable {
        if (!isMonitoring || isPaused || drivingStartTime == 0L) return@Runnable

        val now = System.currentTimeMillis()
        val timeSinceMovement = now - lastMovementTime
        val threshold = getAutoPauseThresholdKmh()

        // If stopped for more than 1 minute and speed is below threshold
        if (timeSinceMovement > MILLIS_PER_MINUTE && currentSpeedKmh < threshold) {
            pauseDriving()
        }
    }

    private fun updateDrivingTime() {
        if (drivingStartTime == 0L || isPaused) return

        val currentSessionTime = System.currentTimeMillis() - drivingStartTime
        val totalMinutes = (totalDrivingTimeMs + currentSessionTime) / MILLIS_PER_MINUTE
        listeners.forEach { it.onDrivingTimeUpdate(totalMinutes.toInt()) }
    }

    private fun checkBreakWarnings() {
        val totalMinutes = getTotalDrivingMinutes()
        val breakInterval = getBreakIntervalMinutes()
        val warningBefore = getWarningBeforeMinutes()

        // Check for warning (15 min before break)
        val minutesUntilBreak = breakInterval - (totalMinutes % breakInterval)
        if (minutesUntilBreak <= warningBefore && minutesUntilBreak > 0 && !warningAlertShown) {
            warningAlertShown = true
            breakAlertShown = false // Reset break alert
            listeners.forEach { it.onBreakWarning(minutesUntilBreak) }
            showBreakWarningDialog(minutesUntilBreak)
        }

        // Check for break required
        if (totalMinutes > 0 && totalMinutes % breakInterval == 0 && !breakAlertShown) {
            breakAlertShown = true
            warningAlertShown = false
            listeners.forEach { it.onBreakRequired(totalMinutes) }
            showBreakRequiredDialog(totalMinutes)
        }

        // Reset alerts when in new interval
        if (totalMinutes % breakInterval > warningBefore) {
            warningAlertShown = false
        }
        if (totalMinutes % breakInterval != 0) {
            breakAlertShown = false
        }
    }

    private fun showBreakWarningDialog(minutesRemaining: Int) {
        val msg = "You've been driving for ${getTotalDrivingMinutes()} minutes. " +
            "Consider taking a break in $minutesRemaining minutes."
        handler.post {
            AlertDialog.Builder(context)
                .setTitle("☕ Break Recommended")
                .setMessage(msg)
                .setPositiveButton("OK") { _, _ -> }
                .setNegativeButton("Find Rest Stops") { _, _ -> findNearbyRestStops() }
                .show()
        }
    }

    private fun showBreakRequiredDialog(totalMinutes: Int) {
        val msg = "You have been driving for $totalMinutes minutes " +
            "(${getBreakIntervalMinutes()} min limit). Please take a break now."
        handler.post {
            AlertDialog.Builder(context)
                .setTitle("🛑 Break Required")
                .setMessage(msg)
                .setPositiveButton("Take Break") { _, _ -> onBreakTaken() }
                .setNegativeButton("Find Rest Stops") { _, _ -> findNearbyRestStops() }
                .setNeutralButton("Continue (15 min)") { _, _ ->
                    // Snooze for 15 minutes
                    warningAlertShown = false
                    breakAlertShown = false
                }
                .show()
        }
    }

    private fun shouldSuggestRestStops(): Boolean {
        val totalMinutes = getTotalDrivingMinutes()
        val breakInterval = getBreakIntervalMinutes()
        return totalMinutes > 0 && totalMinutes % breakInterval == 0 && breakAlertShown
    }

    private fun findNearbyRestStops() {
        if (currentLocation == null) return

        // In a real implementation, this would query a POI API (Overpass, Google Places, etc.)
        // For now, we'll generate mock rest stops near the current location
        val mockRestStops = generateMockRestStops(currentLocation!!)
        listeners.forEach { it.onRestStopsFound(mockRestStops) }
    }

    private fun generateMockRestStops(location: Location): List<RestStop> {
        val stops = mutableListOf<RestStop>()
        val lat = location.latitude
        val lng = location.longitude

        // Generate some mock rest stops around the current location
        val names = listOf("Rest Area", "Service Plaza", "Truck Stop", "Highway Oasis", "Travel Center")
        val types = listOf(RestStop.RestStopType.REST_AREA, RestStop.RestStopType.GAS_STATION,
            RestStop.RestStopType.RESTAURANT, RestStop.RestStopType.PARKING)

        for (i in 0 until MOCK_REST_STOP_COUNT) {
            // Random offset within ~5-20 km
            val distanceKm = (MOCK_MIN_DISTANCE_KM + Math.random() * MOCK_DISTANCE_RANGE_KM).toDouble()
            val bearing = Math.random() * FULL_CIRCLE_DEGREES

            val newLat = lat + (distanceKm / KM_PER_DEGREE_LATITUDE) * kotlin.math.cos(Math.toRadians(bearing))
            val newLng = lng + (distanceKm / (KM_PER_DEGREE_LATITUDE * kotlin.math.cos(Math.toRadians(lat)))) *
                kotlin.math.sin(Math.toRadians(bearing))

            stops.add(RestStop(
                name = "${names[i % names.size]} ${i + 1}",
                latitude = newLat,
                longitude = newLng,
                distanceKm = distanceKm,
                address = "Highway ${100 + i}",
                type = types[i % types.size]
            ))
        }

        return stops.sortedBy { it.distanceKm }
    }

    fun onBreakTaken() {
        lastBreakTime = System.currentTimeMillis()
        totalDrivingTimeMs = 0 // Reset driving time after break
        drivingStartTime = if (isPaused) 0 else System.currentTimeMillis()
        breakAlertShown = false
        warningAlertShown = false
        saveState()
        listeners.forEach { it.onBreakTaken() }
        Log.d(TAG, "Break taken, driving time reset")
    }

    fun skipBreak() {
        // Add break interval to last break time to snooze
        lastBreakTime = System.currentTimeMillis() - (getBreakIntervalMinutes() * MILLIS_PER_MINUTE)
        breakAlertShown = false
        warningAlertShown = false
        saveState()
    }

    fun getTotalDrivingMinutes(): Int {
        var total = totalDrivingTimeMs
        if (drivingStartTime > 0L && !isPaused) {
            total += System.currentTimeMillis() - drivingStartTime
        }
        return (total / MILLIS_PER_MINUTE).toInt()
    }

    fun getTotalDrivingTimeMs(): Long {
        var total = totalDrivingTimeMs
        if (drivingStartTime > 0L && !isPaused) {
            total += System.currentTimeMillis() - drivingStartTime
        }
        return total
    }

    fun getTimeSinceLastBreakMinutes(): Int {
        if (lastBreakTime == 0L) return getTotalDrivingMinutes()
        return ((System.currentTimeMillis() - lastBreakTime) / MILLIS_PER_MINUTE).toInt()
    }

    fun isDriving(): Boolean = drivingStartTime > 0L && !isPaused

    fun isPaused(): Boolean = isPaused

    fun forcePause() {
        pauseDriving()
    }

    fun forceResume() {
        resumeDriving()
    }

    fun reset() {
        totalDrivingTimeMs = 0
        drivingStartTime = 0
        lastBreakTime = 0
        pausedTimeMs = 0
        isPaused = false
        breakAlertShown = false
        warningAlertShown = false
        saveState()
    }

    private fun startPeriodicUpdates() {
        val updateRunnable = object : Runnable {
            override fun run() {
                if (isMonitoring) {
                    updateDrivingTime()
                    checkBreakWarnings()
                    handler.postDelayed(this, PERIODIC_UPDATE_INTERVAL_MS) // Update every 30 seconds
                }
            }
        }
        handler.postDelayed(updateRunnable, PERIODIC_UPDATE_INTERVAL_MS)
    }

    fun cleanup() {
        stopMonitoring()
        handler.removeCallbacksAndMessages(null)
    }
}
