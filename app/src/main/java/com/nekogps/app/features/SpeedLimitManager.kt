package com.nekogps.app.features

import android.media.ToneGenerator
import android.media.AudioManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * SpeedLimitManager parses maxspeed tags from OpenStreetMap data via Overpass API.
 * Displays current road's speed limit and warns visually/audibly when exceeding limit.
 */
class SpeedLimitManager {

    companion object {
        private const val TAG = "SpeedLimitManager"
        private const val OVERPASS_URL = "https://overpass-api.de/api/interpreter"
        private const val SPEED_LIMIT_CACHE_MS = 60_000L // Cache for 1 minute
        private const val TONE_GENERATOR_VOLUME = 80
        private const val CACHE_RADIUS_METERS = 100.0
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 10000
        private const val WALK_SPEED_LIMIT_KMH = 5
        private const val MPH_TO_KMH_FACTOR = 1.609
        private const val SPEED_WARNING_GRACE_KMH = 5
        private const val WARNING_TONE_DURATION_MS = 500
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var cachedSpeedLimit: Int? = null
    private var cacheLocation: GeoPoint? = null
    private var cacheTimestamp: Long = 0
    private var isWarningActive = false
    private var toneGenerator: ToneGenerator? = null

    var onSpeedLimitChanged: ((Int?) -> Unit)? = null
    var onSpeedWarning: ((Int, Int) -> Unit)? = null // (currentSpeed, limit)

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, TONE_GENERATOR_VOLUME)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Could not initialize tone generator", e)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Could not initialize tone generator", e)
        }
    }

    /**
     * Update the current location and fetch speed limit for that location.
     */
    fun updateLocation(location: GeoPoint, currentSpeedKmh: Float) {
        val now = System.currentTimeMillis()

        // Check if cache is still valid
        if (isCacheValid(location, now)) {
            // Use cached value
            checkSpeedWarning(currentSpeedKmh.toInt(), cachedSpeedLimit)
            return
        }

        fetchSpeedLimit(location, currentSpeedKmh)
    }

    private fun isCacheValid(location: GeoPoint, now: Long): Boolean {
        val cachedLocation = cacheLocation
        val cachedLimit = cachedSpeedLimit
        return cachedLocation != null && cachedLimit != null &&
            (now - cacheTimestamp) < SPEED_LIMIT_CACHE_MS &&
            location.distanceToAsDouble(cachedLocation) < CACHE_RADIUS_METERS
    }

    private fun fetchSpeedLimit(location: GeoPoint, currentSpeedKmh: Float) {
        scope.launch {
            try {
                val limit = withContext(Dispatchers.IO) {
                    queryOverpassForSpeedLimit(location)
                }
                cachedSpeedLimit = limit
                cacheLocation = location
                cacheTimestamp = System.currentTimeMillis()
                onSpeedLimitChanged?.invoke(limit)
                checkSpeedWarning(currentSpeedKmh.toInt(), limit)
            } catch (e: IOException) {
                Log.w(TAG, "Failed to fetch speed limit", e)
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Failed to fetch speed limit", e)
            }
        }
    }

    private fun queryOverpassForSpeedLimit(location: GeoPoint): Int? {
        // Query Overpass API for ways with maxspeed near the location
        val query = """
            [out:json][timeout:10];
            way(around:50,${location.latitude},${location.longitude})["maxspeed"];
            out tags 1;
        """.trimIndent()

        val url = URL(OVERPASS_URL)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

        connection.outputStream.use { os ->
            os.write("data=$query".toByteArray())
        }

        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val response = reader.readText()
        reader.close()
        connection.disconnect()

        return parseSpeedLimitFromResponse(response)
    }

    private fun parseSpeedLimitFromResponse(response: String): Int? {
        return try {
            val json = JSONObject(response)
            val elements = json.getJSONArray("elements")
            if (elements.length() > 0) {
                val tags = elements.getJSONObject(0).optJSONObject("tags")
                val maxspeed = tags?.optString("maxspeed", null)
                parseMaxSpeedValue(maxspeed)
            } else null
        } catch (e: JSONException) {
            Log.w(TAG, "Failed to parse speed limit response", e)
            null
        }
    }

    private fun parseMaxSpeedValue(maxspeed: String?): Int? {
        if (maxspeed == null) return null

        return try {
            // Handle formats like "50", "50 mph", "50 km/h", "signals", "none", "walk"
            when {
                maxspeed == "signals" || maxspeed == "none" -> null
                maxspeed == "walk" -> WALK_SPEED_LIMIT_KMH
                maxspeed.contains("mph") -> {
                    val num = maxspeed.replace(Regex("[^0-9]"), "").toIntOrNull()
                    num?.let { (it * MPH_TO_KMH_FACTOR).toInt() } // Convert mph to km/h
                }
                maxspeed.contains("km/h") || maxspeed.contains("kmh") -> {
                    maxspeed.replace(Regex("[^0-9]"), "").toIntOrNull()
                }
                else -> maxspeed.toIntOrNull()
            }
        } catch (e: NumberFormatException) {
            Log.w("SpeedLimitManager", "parseMaxSpeedValue: suppressed Exception", e)
            null
        } catch (e: IllegalArgumentException) {
            Log.w("SpeedLimitManager", "parseMaxSpeedValue: suppressed Exception", e)
            null
        }
    }

    private fun checkSpeedWarning(currentSpeedKmh: Int, limit: Int?) {
        if (limit == null) return

        val threshold = limit + SPEED_WARNING_GRACE_KMH // 5 km/h grace
        if (currentSpeedKmh > threshold) {
            if (!isWarningActive) {
                isWarningActive = true
                triggerWarning(currentSpeedKmh, limit)
            }
        } else {
            isWarningActive = false
        }
    }

    private fun triggerWarning(currentSpeed: Int, limit: Int) {
        onSpeedWarning?.invoke(currentSpeed, limit)
        // Play warning tone
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, WARNING_TONE_DURATION_MS)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Could not play warning tone", e)
        }
    }

    fun getCachedSpeedLimit(): Int? = cachedSpeedLimit

    fun cleanup() {
        scope.cancel()
        toneGenerator?.release()
        toneGenerator = null
    }
}
