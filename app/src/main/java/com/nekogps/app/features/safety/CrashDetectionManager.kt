package com.nekogps.app.features.safety

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.nekogps.app.features.safety.EmergencySOSManager

/**
 * Crash Detection Manager using accelerometer sensor.
 * Detects crash patterns: sudden deceleration + impact.
 * Triggers emergency dialog with countdown before auto-sending SOS.
 */
class CrashDetectionManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "CrashDetectionManager"
        // Thresholds for crash detection (in m/s²)
        private const val IMPACT_THRESHOLD = 30.0f // ~3g impact
        private const val DECELERATION_THRESHOLD = 15.0f // ~1.5g sudden deceleration
        private const val COUNTDOWN_SECONDS = 30 // Countdown before auto-SOS
        private const val SAMPLE_RATE_US = 20000 // 50Hz sampling
        private const val MIN_SPEED_KMH = 20.0f // Minimum speed to consider crash detection
        private const val POST_CRASH_COOLDOWN_MS = 60000 // 1 minute cooldown

        @Volatile
        private var instance: CrashDetectionManager? = null

        fun getInstance(context: Context): CrashDetectionManager {
            return instance ?: synchronized(this) {
                instance ?: CrashDetectionManager(context).also { instance = it }
            }
        }
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val handler = Handler(Looper.getMainLooper())

    private var isMonitoring = false
    private var isCountdownActive = false
    private var currentSpeedKmh = 0f
    private var lastCrashTime = 0L
    private var countdownSecondsRemaining = COUNTDOWN_SECONDS
    private var countdownRunnable: Runnable? = null
    private var alertDialog: AlertDialog? = null

    // Sensor data buffer for pattern detection
    private val accelerationBuffer = CircularBuffer<Float>(50) // 1 second at 50Hz
    private var previousMagnitude = 0f

    interface CrashDetectionListener {
        fun onCrashDetected(impactG: Float, decelerationG: Float)
        fun onCountdownStarted(seconds: Int)
        fun onCountdownTick(secondsRemaining: Int)
        fun onCountdownCancelled()
        fun onAutoSOSTriggered()
        fun onMonitoringChanged(isMonitoring: Boolean)
    }

    private val listeners = mutableListOf<CrashDetectionListener>()

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let { processSensorData(it) }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun addListener(listener: CrashDetectionListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: CrashDetectionListener) {
        listeners.remove(listener)
    }

    fun setCurrentSpeed(speedKmh: Float) {
        currentSpeedKmh = speedKmh
    }

    fun startMonitoring() {
        if (isMonitoring) return
        if (accelerometer == null) {
            Log.w(TAG, "Accelerometer not available")
            return
        }

        isMonitoring = true
        sensorManager.registerListener(sensorEventListener, accelerometer, SAMPLE_RATE_US)
        listeners.forEach { it.onMonitoringChanged(true) }
        Log.d(TAG, "Crash detection started")
    }

    fun stopMonitoring() {
        if (!isMonitoring) return
        isMonitoring = false
        sensorManager.unregisterListener(sensorEventListener)
        cancelCountdown()
        listeners.forEach { it.onMonitoringChanged(false) }
        Log.d(TAG, "Crash detection stopped")
    }

    fun isMonitoring(): Boolean = isMonitoring

    private fun processSensorData(event: SensorEvent) {
        // Only process if driving at sufficient speed
        if (currentSpeedKmh < MIN_SPEED_KMH) return

        // Check cooldown
        val now = System.currentTimeMillis()
        if (now - lastCrashTime < POST_CRASH_COOLDOWN_MS) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calculate magnitude of acceleration vector
        val magnitude = kotlin.math.sqrt(x * x + y * y + z * z)

        // Calculate change in magnitude (jerk)
        val deltaMagnitude = kotlin.math.abs(magnitude - previousMagnitude)
        previousMagnitude = magnitude

        // Add to buffer
        accelerationBuffer.add(magnitude)

        // Check for impact pattern: high magnitude spike
        val isImpact = magnitude > IMPACT_THRESHOLD

        // Check for sudden deceleration: rapid decrease in magnitude
        val isSuddenDeceleration = deltaMagnitude > DECELERATION_THRESHOLD && magnitude < previousMagnitude

        // Crash detected if both impact and sudden deceleration occur
        if (isImpact && isSuddenDeceleration) {
            val impactG = magnitude / 9.81f
            val decelerationG = deltaMagnitude / 9.81f
            onCrashDetected(impactG, decelerationG)
        }
    }

    private fun onCrashDetected(impactG: Float, decelerationG: Float) {
        Log.w(TAG, "Crash detected! Impact: ${impactG}g, Deceleration: ${decelerationG}g")
        lastCrashTime = System.currentTimeMillis()

        listeners.forEach { it.onCrashDetected(impactG, decelerationG) }

        // Show countdown dialog
        showCountdownDialog()
    }

    private fun showCountdownDialog() {
        if (isCountdownActive) return
        isCountdownActive = true
        countdownSecondsRemaining = COUNTDOWN_SECONDS

        handler.post {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("⚠️ Crash Detected")
            builder.setMessage("A potential crash was detected. Emergency SOS will be sent in $COUNTDOWN_SECONDS seconds unless cancelled.")
            builder.setCancelable(false)

            builder.setPositiveButton("Cancel SOS") { _, _ ->
                cancelCountdown()
            }

            builder.setNegativeButton("Send SOS Now") { _, _ ->
                cancelCountdown()
                triggerEmergencySOS()
            }

            alertDialog = builder.create()
            alertDialog?.setOnDismissListener {
                if (isCountdownActive) {
                    cancelCountdown()
                }
            }
            alertDialog?.show()

            startCountdown()
        }
    }

    private fun startCountdown() {
        listeners.forEach { it.onCountdownStarted(COUNTDOWN_SECONDS) }

        countdownRunnable = object : Runnable {
            override fun run() {
                countdownSecondsRemaining--
                if (countdownSecondsRemaining > 0) {
                    // Update dialog message
                    alertDialog?.setMessage("A potential crash was detected. Emergency SOS will be sent in $countdownSecondsRemaining seconds unless cancelled.")
                    listeners.forEach { it.onCountdownTick(countdownSecondsRemaining) }
                    handler.postDelayed(this, 1000)
                } else {
                    // Countdown expired - trigger SOS
                    isCountdownActive = false
                    alertDialog?.dismiss()
                    alertDialog = null
                    triggerEmergencySOS()
                }
            }
        }
        handler.postDelayed(countdownRunnable!!, 1000)
    }

    private fun cancelCountdown() {
        if (!isCountdownActive) return
        isCountdownActive = false
        countdownRunnable?.let { handler.removeCallbacks(it) }
        countdownRunnable = null
        alertDialog?.dismiss()
        alertDialog = null
        listeners.forEach { it.onCountdownCancelled() }
        Log.d(TAG, "Crash SOS countdown cancelled")
    }

    private fun triggerEmergencySOS() {
        Log.w(TAG, "Auto-triggering Emergency SOS after crash detection")
        listeners.forEach { it.onAutoSOSTriggered() }

        val sosManager = EmergencySOSManager.getInstance(context)
        sosManager.triggerSOS()
    }

    // Circular buffer for sensor data
    private class CircularBuffer<T>(private val capacity: Int) {
        private val buffer = mutableListOf<T>()

        fun add(item: T) {
            buffer.add(item)
            if (buffer.size > capacity) buffer.removeAt(0)
        }

        fun get(index: Int): T? {
            if (index < 0 || index >= buffer.size) return null
            return buffer[index]
        }

        fun getSize(): Int = buffer.size
    }
}