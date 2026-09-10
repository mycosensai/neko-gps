package com.nekogps.app.features.tripcomputer

import androidx.lifecycle.ViewModel
import com.nekogps.app.utils.DistanceCalculator
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Fuel and elapsed-time foundation for [TripComputerViewModel].
 * Owns fuel settings and active-time tracking so the ViewModel stays focused
 * on live trip recording and stat publication.
 */
open class TripStatsBase : ViewModel() {

    companion object {
        private const val DEFAULT_FUEL_CONSUMPTION_PER_100KM = 7.0
        private const val DEFAULT_FUEL_PRICE_PER_LITER = 1.50
        private const val MINUTES_PER_HOUR = 60L
        private const val SECONDS_PER_MINUTE = 60L
        private const val FUEL_REFERENCE_DISTANCE_KM = 100.0
    }

    // Fuel calculation settings (liters per 100km and currency per liter).
    protected var fuelConsumptionPer100km = DEFAULT_FUEL_CONSUMPTION_PER_100KM
    protected var fuelPricePerLiter = DEFAULT_FUEL_PRICE_PER_LITER

    // Distance accumulation shared with trip recording.
    protected var totalDistance = 0.0

    // Elapsed-time tracking (excluding paused duration).
    protected var tripStartTime: Long = 0L
    protected var pausedDuration = 0L
    protected var pauseStartTime = 0L

    /**
     * Set fuel consumption rate (liters per 100km).
     */
    open fun setFuelConsumption(litersPer100km: Double) {
        fuelConsumptionPer100km = litersPer100km
    }

    /**
     * Set fuel price per liter.
     */
    open fun setFuelPrice(pricePerLiter: Double) {
        fuelPricePerLiter = pricePerLiter
    }

    /**
     * Estimate fuel cost for a traveled distance.
     */
    protected fun calculateFuelCost(distanceKm: Double): Double {
        val fuelUsed = (distanceKm / FUEL_REFERENCE_DISTANCE_KM) * fuelConsumptionPer100km
        return fuelUsed * fuelPricePerLiter
    }

    /**
     * Get active elapsed time (excluding paused duration).
     */
    fun getActiveElapsedTimeMillis(): Long {
        if (tripStartTime == 0L) return 0L
        val current = if (pauseStartTime > 0) pauseStartTime else System.currentTimeMillis()
        return current - tripStartTime - pausedDuration
    }

    /**
     * Get formatted distance string.
     */
    fun getFormattedDistance(useImperial: Boolean = false): String {
        return DistanceCalculator.formatDistance(totalDistance, useImperial)
    }

    /**
     * Get formatted elapsed time string.
     */
    fun getFormattedElapsedTime(): String {
        val elapsed = getActiveElapsedTimeMillis()
        val hours = TimeUnit.MILLISECONDS.toHours(elapsed)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed) % MINUTES_PER_HOUR
        val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsed) % SECONDS_PER_MINUTE
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }
}
