package com.nekogps.app.features.performance

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.util.Log

/**
 * Phase 10: Smart location updates with adaptive interval based on speed,
 * Doze mode handling, and battery usage stats.
 */
class BatteryOptimizationManager(private val context: Context) {

    data class BatteryStats(
        val levelPercent: Int = -1,
        val isCharging: Boolean = false,
        val isPowerSaveMode: Boolean = false,
        val isDozeMode: Boolean = false,
        val locationIntervalMs: Long = DEFAULT_INTERVAL_MS,
        val estimatedHoursRemaining: Double = -1.0
    )

    interface Listener {
        fun onIntervalChanged(intervalMs: Long) {}
        fun onBatteryStats(stats: BatteryStats) {}
        fun onPowerSaveModeChanged(enabled: Boolean) {}
    }

    var listener: Listener? = null

    private val prefs: SharedPreferences =
        context.getSharedPreferences("battery_opt_prefs", Context.MODE_PRIVATE)
    private val powerManager: PowerManager? =
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    var currentIntervalMs: Long = DEFAULT_INTERVAL_MS
        private set

    private var lastLocation: Location? = null
    private var tracking = false
    private var lastLevel = -1
    private var lastLevelTime = 0L

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent == null) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            trackDrain(pct)
            listener?.onBatteryStats(getStats(pct, charging))
        }
    }

    private val powerSaveReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            val saving = powerManager?.isPowerSaveMode ?: false
            listener?.onPowerSaveModeChanged(saving)
            // Re-evaluate interval under new power constraints.
            lastLocation?.let { onLocationUpdate(it) }
            listener?.onBatteryStats(getStats())
        }
    }

    fun startTracking() {
        if (tracking) return
        tracking = true
        try {
            context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                context.registerReceiver(powerSaveReceiver, filter)
            }
        } catch (e: Exception) { Log.w(TAG, "register failed: " + e.message) }
    }

    fun stopTracking() {
        if (!tracking) return
        tracking = false
        try { context.unregisterReceiver(batteryReceiver) } catch (e: Exception) {
            Log.w("BatteryOptimizationManager", "stopTracking: suppressed Exception", e)
            /* ignore */ }
        try { context.unregisterReceiver(powerSaveReceiver) } catch (e: Exception) {
            Log.w("BatteryOptimizationManager", "stopTracking: suppressed Exception", e)
            /* ignore */ }
    }

    /**
     * Feed each location fix; returns the recommended update interval.
     * Adaptive: stationary -> slow, walking -> medium, driving -> fast.
     * Power-save / Doze stretches the interval to save battery.
     */
    fun onLocationUpdate(location: Location): Long {
        val speed = if (location.hasSpeed()) location.speed else estimateSpeed(location)
        var interval = when {
            speed < SPEED_STATIONARY_M_S -> INTERVAL_STATIONARY_MS
            speed < SPEED_WALK_M_S -> INTERVAL_WALK_MS
            speed < SPEED_DRIVE_M_S -> INTERVAL_DEFAULT_MS
            else -> INTERVAL_FAST_MS
        }
        if (isPowerSaveMode()) interval = (interval * 2).coerceAtMost(MAX_INTERVAL_MS)
        if (isDozeMode()) interval = (interval * 2).coerceAtMost(MAX_INTERVAL_MS)
        lastLocation = Location(location)
        if (interval != currentIntervalMs) {
            currentIntervalMs = interval
            prefs.edit().putLong(KEY_INTERVAL, interval).apply()
            listener?.onIntervalChanged(interval)
        }
        return currentIntervalMs
    }

    fun isPowerSaveMode(): Boolean = powerManager?.isPowerSaveMode ?: false

    /** True when the device is idle (Doze) and background work is deferred. */
    fun isDozeMode(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                powerManager?.isDeviceIdleMode ?: false
            } else false
        } catch (e: Exception) {
            Log.w("BatteryOptimizationManager", "isDozeMode: suppressed Exception", e)
            false }
    }

    /** Suggest whether background tracking should back off right now. */
    fun shouldDeferBackgroundWork(): Boolean = isDozeMode() || (isPowerSaveMode() && !isChargingNow())

    fun getStats(levelPct: Int = lastLevel, charging: Boolean = isChargingNow()): BatteryStats {
        val lvl = if (levelPct >= 0) levelPct else readLevel()
        return BatteryStats(
            levelPercent = lvl,
            isCharging = charging,
            isPowerSaveMode = isPowerSaveMode(),
            isDozeMode = isDozeMode(),
            locationIntervalMs = currentIntervalMs,
            estimatedHoursRemaining = estimateHoursRemaining(lvl)
        )
    }

    private fun estimateSpeed(location: Location): Float {
        val prev = lastLocation ?: return 0f
        val dt = (location.time - prev.time) / 1000f
        if (dt <= 0f) return 0f
        return try { prev.distanceTo(location) / dt } catch (e: Exception) {
            Log.w("BatteryOptimizationManager", "estimateSpeed: suppressed Exception", e)
            0f }
    }

    private fun trackDrain(pct: Int) {
        if (pct < 0) return
        val now = System.currentTimeMillis()
        if (lastLevel >= 0 && pct < lastLevel && lastLevelTime > 0) {
            prefs.edit().putFloat(KEY_DRAIN_PER_H, drainPerHour(lastLevel - pct, now - lastLevelTime)).apply()
        }
        lastLevel = pct
        lastLevelTime = now
    }

    private fun drainPerHour(dropPct: Int, elapsedMs: Long): Float {
        if (elapsedMs <= 0) return -1f
        return (dropPct * 3_600_000f / elapsedMs).coerceIn(0f, 100f)
    }

    private fun estimateHoursRemaining(levelPct: Int): Double {
        val drain = prefs.getFloat(KEY_DRAIN_PER_H, -1f)
        if (levelPct < 0 || drain <= 0f) return -1.0
        return (levelPct / drain).toDouble()
    }

    private fun readLevel(): Int {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: lastLevel
            } else lastLevel
        } catch (e: Exception) {
            Log.w("BatteryOptimizationManager", "readLevel: suppressed Exception", e)
            lastLevel }
    }

    private fun isChargingNow(): Boolean {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bm?.isCharging ?: false
            } else false
        } catch (e: Exception) {
            Log.w("BatteryOptimizationManager", "isChargingNow: suppressed Exception", e)
            false }
    }

    companion object {
        private const val TAG = "BatteryOptManager"
        private const val KEY_INTERVAL = "location_interval_ms"
        private const val KEY_DRAIN_PER_H = "drain_per_hour"
        const val DEFAULT_INTERVAL_MS = 5_000L
        private const val INTERVAL_FAST_MS = 1_000L
        private const val INTERVAL_DEFAULT_MS = 5_000L
        private const val INTERVAL_WALK_MS = 10_000L
        private const val INTERVAL_STATIONARY_MS = 30_000L
        private const val MAX_INTERVAL_MS = 60_000L
        private const val SPEED_STATIONARY_M_S = 0.5f
        private const val SPEED_WALK_M_S = 2.0f
        private const val SPEED_DRIVE_M_S = 15.0f
    }
}
