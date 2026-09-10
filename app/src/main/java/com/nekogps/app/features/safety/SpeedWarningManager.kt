package com.nekogps.app.features.safety

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.util.Log
import com.nekogps.app.features.SpeedLimitManager
import kotlinx.coroutines.*

/**
 * Speed Warning Manager - Audio + visual warnings when exceeding speed limit.
 * Uses SpeedLimitManager for speed limit data. Configurable thresholds.
 */
class SpeedWarningManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "SpeedWarningManager"
        private const val PREFS_NAME = "speed_warning"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_VISUAL_WARNING = "visual_warning"
        private const val KEY_AUDIO_WARNING = "audio_warning"
        private const val KEY_VOICE_WARNING = "voice_warning"
        private const val KEY_THRESHOLD_KMH = "threshold_kmh" // km/h over limit
        private const val KEY_REPEAT_INTERVAL = "repeat_interval_seconds"
        private const val KEY_STRICT_MODE = "strict_mode" // Warn at limit, not over

        private const val DEFAULT_THRESHOLD_KMH = 5
        private const val DEFAULT_REPEAT_INTERVAL = 15 // seconds
        private const val MIN_REPEAT_INTERVAL = 3

        @Volatile
        private var instance: SpeedWarningManager? = null

        fun getInstance(context: Context): SpeedWarningManager {
            return instance ?: synchronized(this) {
                instance ?: SpeedWarningManager(context).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Audio
    private var toneGenerator: ToneGenerator? = null
    private var soundPool: SoundPool? = null
    private var warningSoundId = -1
    private var isSoundPoolLoaded = false

    // State
    private var isWarningActive = false
    private var lastWarningTime = 0L
    private var currentSpeedLimit: Int? = null
    private var currentSpeedKmh = 0

    // References
    private var speedLimitManager: SpeedLimitManager? = null
    private var textToSpeechService: com.nekogps.app.features.voice.TextToSpeechService? = null

    interface SpeedWarningListener {
        fun onSpeedLimitChanged(limit: Int?)
        fun onWarningTriggered(currentSpeed: Int, limit: Int, overBy: Int)
        fun onWarningCleared()
        fun onSettingsChanged()
    }

    private val listeners = mutableListOf<SpeedWarningListener>()

    init {
        initializeAudio()
        loadSettings()
    }

    private fun initializeAudio() {
        // ToneGenerator for simple beeps
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 80)
        } catch (e: Exception) {
            Log.w(TAG, "Could not initialize ToneGenerator", e)
        }

        // SoundPool for custom warning sounds
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(audioAttributes)
                .build()

            soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    isSoundPoolLoaded = true
                    Log.d(TAG, "Warning sound loaded")
                }
            }

            // Load a default warning sound (we'll generate one programmatically if needed)
            // For now, we'll use tone generator as fallback
        } catch (e: Exception) {
            Log.w(TAG, "Could not initialize SoundPool", e)
        }
    }

    private fun loadSettings() {
        // Settings loaded on demand via getters
    }

    fun addListener(listener: SpeedWarningListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: SpeedWarningListener) {
        listeners.remove(listener)
    }

    fun setSpeedLimitManager(manager: SpeedLimitManager) {
        speedLimitManager = manager
        speedLimitManager?.onSpeedLimitChanged = { limit ->
            currentSpeedLimit = limit
            listeners.forEach { it.onSpeedLimitChanged(limit) }
            checkSpeedWarning()
        }
        speedLimitManager?.onSpeedWarning = { currentSpeed, limit ->
            // This is called by SpeedLimitManager when it detects speeding
            onSpeedUpdate(currentSpeed, limit)
        }
    }

    fun setTextToSpeechService(service: com.nekogps.app.features.voice.TextToSpeechService) {
        textToSpeechService = service
    }

    fun onLocationUpdate(location: org.osmdroid.util.GeoPoint, speedKmh: Float) {
        currentSpeedKmh = speedKmh.toInt()
        speedLimitManager?.updateLocation(location, speedKmh)
        checkSpeedWarning()
    }

    fun onSpeedUpdate(speedKmh: Int, limit: Int?) {
        currentSpeedKmh = speedKmh
        if (limit != null) {
            currentSpeedLimit = limit
            listeners.forEach { it.onSpeedLimitChanged(limit) }
        }
        checkSpeedWarning()
    }

    private fun checkSpeedWarning() {
        val limit = currentSpeedLimit ?: return
        val threshold = getThresholdKmh()
        val strictMode = getStrictMode()

        val warningSpeed = if (strictMode) limit else (limit + threshold)
        val isSpeeding = currentSpeedKmh > warningSpeed

        if (isSpeeding && !isWarningActive) {
            activateWarning()
        } else if (!isSpeeding && isWarningActive) {
            deactivateWarning()
        }

        // Check for repeat warning
        if (isWarningActive && isSpeeding) {
            val now = System.currentTimeMillis()
            val repeatInterval = getRepeatInterval() * 1000L
            if (now - lastWarningTime >= repeatInterval) {
                triggerWarning()
            }
        }
    }

    private fun activateWarning() {
        isWarningActive = true
        triggerWarning()
    }

    private fun deactivateWarning() {
        isWarningActive = false
        listeners.forEach { it.onWarningCleared() }
        Log.d(TAG, "Speed warning cleared")
    }

    private fun triggerWarning() {
        val limit = currentSpeedLimit ?: return
        val overBy = currentSpeedKmh - limit
        lastWarningTime = System.currentTimeMillis()

        Log.w(TAG, "Speed warning: ${currentSpeedKmh} km/h (limit: $limit, over by: $overBy)")

        // Visual warning via listeners
        listeners.forEach { it.onWarningTriggered(currentSpeedKmh, limit, overBy) }

        // Audio warning
        if (getAudioWarningEnabled()) {
            playWarningTone()
        }

        // Voice warning
        if (getVoiceWarningEnabled()) {
            speakWarning(limit, overBy)
        }
    }

    private fun playWarningTone() {
        // Try SoundPool first, fallback to ToneGenerator
        if (isSoundPoolLoaded && warningSoundId != -1) {
            soundPool?.play(warningSoundId, 1f, 1f, 1, 0, 1f)
        } else {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 300)
        }
    }

    private fun speakWarning(limit: Int, overBy: Int) {
        scope.launch {
            val message = if (overBy > 20) {
                "DANGER! You are exceeding the speed limit by $overBy kilometers per hour! Slow down immediately!"
            } else if (overBy > 10) {
                "Warning! You are $overBy kilometers per hour over the speed limit of $limit. Please slow down."
            } else {
                "Speed limit $limit. You are $overBy over. Please reduce speed."
            }
            textToSpeechService?.speakAlert(message)
        }
    }

    // Settings
    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, true)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (!enabled) deactivateWarning()
        listeners.forEach { it.onSettingsChanged() }
    }

    fun getVisualWarningEnabled(): Boolean = prefs.getBoolean(KEY_VISUAL_WARNING, true)

    fun setVisualWarningEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VISUAL_WARNING, enabled).apply()
        listeners.forEach { it.onSettingsChanged() }
    }

    fun getAudioWarningEnabled(): Boolean = prefs.getBoolean(KEY_AUDIO_WARNING, true)

    fun setAudioWarningEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUDIO_WARNING, enabled).apply()
        listeners.forEach { it.onSettingsChanged() }
    }

    fun getVoiceWarningEnabled(): Boolean = prefs.getBoolean(KEY_VOICE_WARNING, true)

    fun setVoiceWarningEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOICE_WARNING, enabled).apply()
        listeners.forEach { it.onSettingsChanged() }
    }

    fun getThresholdKmh(): Int = prefs.getInt(KEY_THRESHOLD_KMH, DEFAULT_THRESHOLD_KMH)

    fun setThresholdKmh(threshold: Int) {
        val clamped = threshold.coerceIn(0, 50)
        prefs.edit().putInt(KEY_THRESHOLD_KMH, clamped).apply()
        listeners.forEach { it.onSettingsChanged() }
    }

    fun getRepeatInterval(): Int = prefs.getInt(KEY_REPEAT_INTERVAL, DEFAULT_REPEAT_INTERVAL)

    fun setRepeatInterval(seconds: Int) {
        val clamped = seconds.coerceIn(MIN_REPEAT_INTERVAL, 300)
        prefs.edit().putInt(KEY_REPEAT_INTERVAL, clamped).apply()
        listeners.forEach { it.onSettingsChanged() }
    }

    fun getStrictMode(): Boolean = prefs.getBoolean(KEY_STRICT_MODE, false)

    fun setStrictMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STRICT_MODE, enabled).apply()
        listeners.forEach { it.onSettingsChanged() }
    }

    fun getCurrentSpeedLimit(): Int? = currentSpeedLimit

    fun getCurrentSpeed(): Int = currentSpeedKmh

    fun isWarningActive(): Boolean = isWarningActive

    fun cleanup() {
        scope.cancel()
        toneGenerator?.release()
        toneGenerator = null
        soundPool?.release()
        soundPool = null
        isSoundPoolLoaded = false
    }
}