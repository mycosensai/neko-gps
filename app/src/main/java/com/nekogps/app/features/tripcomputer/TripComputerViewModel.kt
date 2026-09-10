package com.nekogps.app.features.tripcomputer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nekogps.app.utils.DistanceCalculator
import org.osmdroid.util.GeoPoint
import java.util.concurrent.TimeUnit

/**
 * ViewModel for tracking trip statistics in real-time.
 * Tracks distance traveled, elapsed time, average speed, max speed, and fuel cost estimate.
 */
class TripComputerViewModel : TripStatsBase() {

    companion object {
        private const val MIN_MOVING_SPEED_KMH = 0.5
        private const val MILLIS_PER_HOUR = 3_600_000.0
        private const val METERS_PER_KILOMETER = 1000.0
    }

    private val _tripDistanceMeters = MutableLiveData(0.0)
    val tripDistanceMeters: LiveData<Double> = _tripDistanceMeters

    private val _elapsedTimeMillis = MutableLiveData(0L)
    val elapsedTimeMillis: LiveData<Long> = _elapsedTimeMillis

    private val _averageSpeedKmh = MutableLiveData(0.0)
    val averageSpeedKmh: LiveData<Double> = _averageSpeedKmh

    private val _maxSpeedKmh = MutableLiveData(0.0)
    val maxSpeedKmh: LiveData<Double> = _maxSpeedKmh

    private val _currentSpeedKmh = MutableLiveData(0.0)
    val currentSpeedKmh: LiveData<Double> = _currentSpeedKmh

    private val _fuelCostEstimate = MutableLiveData(0.0)
    val fuelCostEstimate: LiveData<Double> = _fuelCostEstimate

    private val _isTripActive = MutableLiveData(false)
    val isTripActive: LiveData<Boolean> = _isTripActive

    private val _topSpeedKmh = MutableLiveData(0.0)
    val topSpeedKmh: LiveData<Double> = _topSpeedKmh

    // Trip tracking data
    private var lastLocation: GeoPoint? = null
    private var speedReadings = mutableListOf<Double>()

    /**
     * Start a new trip.
     */
    fun startTrip() {
        tripStartTime = System.currentTimeMillis()
        totalDistance = 0.0
        speedReadings.clear()
        pausedDuration = 0L
        lastLocation = null
        _isTripActive.value = true
        updateStats()
    }

    /**
     * Pause the trip (e.g., when stopped at traffic).
     */
    fun pauseTrip() {
        if (_isTripActive.value == true) {
            pauseStartTime = System.currentTimeMillis()
            _isTripActive.value = false
        }
    }

    /**
     * Resume a paused trip.
     */
    fun resumeTrip() {
        if (pauseStartTime > 0) {
            pausedDuration += System.currentTimeMillis() - pauseStartTime
            pauseStartTime = 0
        }
        _isTripActive.value = true
    }

    /**
     * Stop the current trip and reset stats.
     */
    fun stopTrip() {
        _isTripActive.value = false
        tripStartTime = 0L
        totalDistance = 0.0
        speedReadings.clear()
        pausedDuration = 0L
        pauseStartTime = 0L
        lastLocation = null
        updateStats()
    }

    /**
     * Update trip stats with new location.
     */
    fun updateLocation(location: GeoPoint, speedKmh: Double) {
        if (_isTripActive.value != true) return

        _currentSpeedKmh.value = speedKmh

        // Update max speed
        if (speedKmh > (_maxSpeedKmh.value ?: 0.0)) {
            _maxSpeedKmh.value = speedKmh
        }

        // Update top speed (all-time record)
        if (speedKmh > (_topSpeedKmh.value ?: 0.0)) {
            _topSpeedKmh.value = speedKmh
        }

        // Calculate distance from last point
        lastLocation?.let { last ->
            val segmentDistance = DistanceCalculator.haversineDistance(
                last.latitude, last.longitude,
                location.latitude, location.longitude
            )
            // Only add if movement is reasonable (filter GPS noise)
            if (segmentDistance > 2.0) { // minimum 2 meters
                totalDistance += segmentDistance
            }
        }
        lastLocation = location

        // Track speed readings for average calculation
        if (speedKmh > MIN_MOVING_SPEED_KMH) { // Only count when actually moving
            speedReadings.add(speedKmh)
        }

        updateStats()
    }

    /**
     * Set fuel consumption rate (liters per 100km).
     */
    override fun setFuelConsumption(litersPer100km: Double) {
        super.setFuelConsumption(litersPer100km)
        updateStats()
    }

    /**
     * Set fuel price per liter.
     */
    override fun setFuelPrice(pricePerLiter: Double) {
        super.setFuelPrice(pricePerLiter)
        updateStats()
    }

    /**
     * Get trip summary as a data object.
     */
    fun getTripSummary(): TripSummary {
        return TripSummary(
            distanceMeters = totalDistance,
            elapsedTimeMillis = getActiveElapsedTimeMillis(),
            averageSpeedKmh = _averageSpeedKmh.value ?: 0.0,
            maxSpeedKmh = _maxSpeedKmh.value ?: 0.0,
            fuelCost = _fuelCostEstimate.value ?: 0.0
        )
    }

    private fun updateStats() {
        _tripDistanceMeters.value = totalDistance
        _elapsedTimeMillis.value = getActiveElapsedTimeMillis()

        // Calculate average speed
        val avgSpeed = if (speedReadings.isNotEmpty()) {
            speedReadings.average()
        } else {
            val elapsedHours = getActiveElapsedTimeMillis() / MILLIS_PER_HOUR
            if (elapsedHours > 0) {
                (totalDistance / METERS_PER_KILOMETER) / elapsedHours
            } else {
                0.0
            }
        }
        _averageSpeedKmh.value = avgSpeed

        // Calculate fuel cost
        val distanceKm = totalDistance / METERS_PER_KILOMETER
        _fuelCostEstimate.value = calculateFuelCost(distanceKm)
    }

    override fun onCleared() {
        super.onCleared()
        stopTrip()
    }
}

/**
 * Data class for trip summary.
 */
data class TripSummary(
    val distanceMeters: Double,
    val elapsedTimeMillis: Long,
    val averageSpeedKmh: Double,
    val maxSpeedKmh: Double,
    val fuelCost: Double
)
