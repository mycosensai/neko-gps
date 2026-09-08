package com.nekogps.app.features

import android.content.Context
import android.media.ToneGenerator
import android.media.AudioManager
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * SpeedLimitManager parses maxspeed tags from OpenStreetMap data via Overpass API.
 * Displays current road's speed limit and warns visually/audibly when exceeding limit.
 */
class SpeedLimitManager(private val context: Context) {

    companion object {
        private const val TAG = "SpeedLimitManager"
        private const val OVERPASS_URL = "https://overpass-api.de/api/interpreter"
        private const val SPEED_LIMIT_CACHE_MS = 60_000L // Cache for 1 minute
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
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 80)
        } catch (e: Exception) {
            Log.w(TAG, "Could not initialize tone generator", e)
        }
    }

    /**
     * Update the current location and fetch speed limit for that location.
     */
    fun updateLocation(location: GeoPoint, currentSpeedKmh: Float) {
        val now = System.currentTimeMillis()

        // Check if cache is still valid
        if (cachedSpeedLimit != null && cacheLocation != null &&
            (now - cacheTimestamp) < SPEED_LIMIT_CACHE_MS &&
            location.distanceToAsDouble(cacheLocation!!) < 100) {
            // Use cached value
            checkSpeedWarning(currentSpeedKmh.toInt(), cachedSpeedLimit!!)
            return
        }

        fetchSpeedLimit(location, currentSpeedKmh)
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
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch speed limit", e)
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
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse speed limit response", e)
            null
        }
    }

    private fun parseMaxSpeedValue(maxspeed: String?): Int? {
        if (maxspeed == null) return null

        return try {
            // Handle formats like "50", "50 mph", "50 km/h", "signals", "none", "walk"
            when {
                maxspeed == "signals" || maxspeed == "none" -> null
                maxspeed == "walk" -> 5
                maxspeed.contains("mph") -> {
                    val num = maxspeed.replace(Regex("[^0-9]"), "").toIntOrNull()
                    num?.let { (it * 1.609).toInt() } // Convert mph to km/h
                }
                maxspeed.contains("km/h") || maxspeed.contains("kmh") -> {
                    maxspeed.replace(Regex("[^0-9]"), "").toIntOrNull()
                }
                else -> maxspeed.toIntOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun checkSpeedWarning(currentSpeedKmh: Int, limit: Int?) {
        if (limit == null) return

        val threshold = limit + 5 // 5 km/h grace
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
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 500)
        } catch (e: Exception) {
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
