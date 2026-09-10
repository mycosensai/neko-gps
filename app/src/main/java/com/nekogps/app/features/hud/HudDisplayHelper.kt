package com.nekogps.app.features.hud

import android.content.Context
import android.view.View
import com.nekogps.app.databinding.ActivityHudBinding

/**
 * Renders the HUD turn arrow and speed-warning styling for [HUDActivity].
 * Extracted so the activity stays under the function-count limit.
 */
class HudDisplayHelper(
    private val binding: ActivityHudBinding,
    private val context: Context
) {

    companion object {
        private const val ARROW_STRAIGHT = "↑"
        private const val ARROW_LEFT = "←"
        private const val ARROW_RIGHT = "→"
        private const val ARROW_SLIGHT_LEFT = "↖"
        private const val ARROW_SLIGHT_RIGHT = "↗"
        private const val ARROW_SHARP_LEFT = "⬅"
        private const val ARROW_SHARP_RIGHT = "➡"
        private const val ARROW_UTURN = "↩"
        private const val ARROW_ARRIVE = "🏁"
        private const val SPEED_WARNING_THRESHOLD_KMH = 100
        private const val SPEED_CAUTION_THRESHOLD_KMH = 80
    }

    fun updateTurnArrow(nextTurnDirection: String) {
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

    fun updateSpeedWarning(currentSpeedKmh: Double) {
        // Show warning if speeding (example: over the warning threshold)
        if (currentSpeedKmh > SPEED_WARNING_THRESHOLD_KMH) {
            binding.tvSpeed.setTextColor(context.getColor(android.R.color.holo_red_light))
            binding.tvSpeedWarning.visibility = View.VISIBLE
        } else if (currentSpeedKmh > SPEED_CAUTION_THRESHOLD_KMH) {
            binding.tvSpeed.setTextColor(context.getColor(android.R.color.holo_orange_light))
            binding.tvSpeedWarning.visibility = View.GONE
        } else {
            binding.tvSpeed.setTextColor(context.getColor(android.R.color.white))
            binding.tvSpeedWarning.visibility = View.GONE
        }
    }
}
