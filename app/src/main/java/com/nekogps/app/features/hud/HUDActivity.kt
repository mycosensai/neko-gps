package com.nekogps.app.features.hud

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationRequest
import com.nekogps.app.R
import com.nekogps.app.databinding.ActivityHudBinding
import com.nekogps.app.features.SpeedLimitManager
import com.nekogps.app.utils.DistanceCalculator
import org.osmdroid.util.GeoPoint
import java.util.concurrent.TimeUnit

/**
 * HUD (Heads-Up Display) Mode for Neko GPS.
 * Shows a mirror-friendly display with large turn arrows, distance to next turn,
 * current speed, and ETA. Uses high-contrast white-on-black design for dashboard use.
 * Integrates SpeedLimitManager for speed limit display and warnings.
 */
class HUDActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHudBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var speedLimitManager: SpeedLimitManager
    private val handler = Handler(Looper.getMainLooper())

    private var currentSpeedKmh = 0.0
    private var currentLocation: GeoPoint? = null
    private var destination: GeoPoint? = null
    private var nextTurnDistanceMeters = 0.0
    private var nextTurnDirection = ""
    private var etaMinutes = 0.0
    private var isMirrored = true

    companion object {
        private const val TAG = "HUDActivity"
        private const val ARROW_STRAIGHT = "↑"
        private const val ARROW_LEFT = "←"
        private const val ARROW_RIGHT = "→"
        private const val ARROW_SLIGHT_LEFT = "↖"
        private const val ARROW_SLIGHT_RIGHT = "↗"
        private const val ARROW_SHARP_LEFT = "⬅"
        private const val ARROW_SHARP_RIGHT = "➡"
        private const val ARROW_UTURN = "↩"
        private const val ARROW_ARRIVE = "🏁"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on for HUD mode
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Set fullscreen
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        )

        binding = ActivityHudBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        speedLimitManager = SpeedLimitManager(this)

        setupUI()
        setupSpeedLimitManager()
        requestLocationUpdates()
    }

    private fun setupUI() {
        // Toggle mirror mode
        binding.btnToggleMirror.setOnClickListener {
            isMirrored = !isMirrored
            applyMirrorMode()
        }

        // Exit HUD mode
        binding.btnExitHud.setOnClickListener {
            finish()
        }

        // Initial mirror mode setup
        applyMirrorMode()
    }

    private fun setupSpeedLimitManager() {
        speedLimitManager.onSpeedLimitChanged = { limit ->
            runOnUiThread {
                if (limit != null) {
                    binding.tvSpeedLimit.text = limit.toString()
                    binding.tvSpeedLimit.visibility = View.VISIBLE
                    binding.tvSpeedLimitLabel.visibility = View.VISIBLE
                } else {
                    binding.tvSpeedLimit.text = "--"
                    binding.tvSpeedLimit.visibility = View.GONE
                    binding.tvSpeedLimitLabel.visibility = View.GONE
                }
            }
        }

        speedLimitManager.onSpeedWarning = { speed, limit ->
            runOnUiThread {
                binding.tvSpeedWarning.visibility = View.VISIBLE
                binding.tvSpeedWarning.text = "⚠️ OVER LIMIT: $speed / $limit km/h"
                handler.postDelayed({
                    binding.tvSpeedWarning.visibility = View.GONE
                }, 3000)
            }
        }
    }

    private fun applyMirrorMode() {
        if (isMirrored) {
            binding.root.scaleX = -1f
            // Scale back child elements so text is readable
            binding.tvArrow.scaleX = -1f
            binding.tvDistance.scaleX = -1f
            binding.tvSpeed.scaleX = -1f
            binding.tvEta.scaleX = -1f
            binding.tvInstruction.scaleX = -1f
            binding.tvSpeedLimit?.scaleX = -1f
            binding.tvSpeedLimitLabel?.scaleX = -1f
        } else {
            binding.root.scaleX = 1f
            binding.tvArrow.scaleX = 1f
            binding.tvDistance.scaleX = 1f
            binding.tvSpeed.scaleX = 1f
            binding.tvEta.scaleX = 1f
            binding.tvInstruction.scaleX = 1f
            binding.tvSpeedLimit?.scaleX = 1f
            binding.tvSpeedLimitLabel?.scaleX = 1f
        }
    }

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = TimeUnit.SECONDS.toMillis(1)
            fastestInterval = TimeUnit.MILLISECONDS.toMillis(500)
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    val loc = result.lastLocation ?: return
                    currentLocation = GeoPoint(loc.latitude, loc.longitude)
                    currentSpeedKmh = loc.speed * 3.6 // m/s to km/h
                    updateDisplay()
                    speedLimitManager.updateLocation(currentLocation!!, currentSpeedKmh.toFloat())
                }
            },
            Looper.getMainLooper()
        )
    }

    private fun updateDisplay() {
        // Update speed
        binding.tvSpeed.text = String.format("%.0f", currentSpeedKmh)

        // Update distance to next turn
        binding.tvDistance.text = DistanceCalculator.formatDistance(nextTurnDistanceMeters)

        // Update ETA
        binding.tvEta.text = DistanceCalculator.formatETA(etaMinutes)

        // Update instruction text
        binding.tvInstruction.text = nextTurnDirection

        // Update turn arrow
        updateTurnArrow()

        // Update speed warning indicator
        updateSpeedWarning()
    }

    private fun updateTurnArrow() {
        val arrow = when {
            nextTurnDirection.contains("arrive", ignoreCase = true) -> ARROW_ARRIVE
            nextTurnDirection.contains("uturn", ignoreCase = true) -> ARROW_UTURN
            nextTurnDirection.contains("sharp left", ignoreCase = true) -> ARROW_SHARP_LEFT
            nextTurnDirection.contains("sharp right", ignoreCase = true) -> ARROW_SHARP_RIGHT
            nextTurnDirection.contains("slight left", ignoreCase = true) -> ARROW_SLIGHT_LEFT
            nextTurnDirection.contains("slight right", ignoreCase = true) -> ARROW_SLIGHT_RIGHT
            nextTurnDirection.contains("left", ignoreCase = true) -> ARROW_LEFT
            nextTurnDirection.contains("right", ignoreCase = true) -> ARROW_RIGHT
            else -> ARROW_STRAIGHT
        }
        binding.tvArrow.text = arrow
    }

    private fun updateSpeedWarning() {
        // Show warning if speeding (example: > 100 km/h)
        if (currentSpeedKmh > 100) {
            binding.tvSpeed.setTextColor(getColor(android.R.color.holo_red_light))
            binding.tvSpeedWarning.visibility = View.VISIBLE
        } else if (currentSpeedKmh > 80) {
            binding.tvSpeed.setTextColor(getColor(android.R.color.holo_orange_light))
            binding.tvSpeedWarning.visibility = View.GONE
        } else {
            binding.tvSpeed.setTextColor(getColor(android.R.color.white))
            binding.tvSpeedWarning.visibility = View.GONE
        }
    }

    /**
     * Set navigation data to display.
     */
    fun setNavigationData(
        turnDirection: String,
        turnDistanceMeters: Double,
        etaMinutes: Double
    ) {
        this.nextTurnDirection = turnDirection
        this.nextTurnDistanceMeters = turnDistanceMeters
        this.etaMinutes = etaMinutes
        updateDisplay()
    }

    /**
     * Set destination for ETA calculation.
     */
    fun setDestination(dest: GeoPoint) {
        destination = dest
    }

    override fun onDestroy() {
        super.onDestroy()
        speedLimitManager.cleanup()
        fusedLocationClient.removeLocationUpdates(object : com.google.android.gms.location.LocationCallback() {})
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onResume() {
        super.onResume()
        // Re-apply fullscreen
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        )
    }
}
