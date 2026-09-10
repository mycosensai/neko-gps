package com.nekogps.app.ui

import com.nekogps.app.utils.DistanceCalculator

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.osmdroid.util.GeoPoint

/**
 * Singleton managing shared map state across the application.
 * Holds current location, recorded tracks, and map preferences using StateFlow.
 */
object MapStateManager : RecordedTrackStore() {

    private const val DEFAULT_GPS_INTERVAL_MS = 2000L

    private val _currentLocation = MutableStateFlow<GeoPoint?>(null)
    val currentLocation: StateFlow<GeoPoint?> = _currentLocation.asStateFlow()

    private val _mapLayer = MutableStateFlow("Standard")
    val mapLayer: StateFlow<String> = _mapLayer.asStateFlow()

    private val _gpsInterval = MutableStateFlow(DEFAULT_GPS_INTERVAL_MS)
    val gpsInterval: StateFlow<Long> = _gpsInterval.asStateFlow()

    private val _distanceUnits = MutableStateFlow("Metric")
    val distanceUnits: StateFlow<String> = _distanceUnits.asStateFlow()

    private val _theme = MutableStateFlow("Dark")
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _autoCenterEnabled = MutableStateFlow(true)
    val autoCenterEnabled: StateFlow<Boolean> = _autoCenterEnabled.asStateFlow()

    /**
     * Update the current GPS location.
     */
    fun updateCurrentLocation(point: GeoPoint) {
        _currentLocation.value = point

        // Add to current recording if active
        if (isRecordingState.value) {
            currentRecordingPointsState.add(point)
        }
    }

    /**
     * Set the map layer preference.
     */
    fun setMapLayer(layer: String) {
        _mapLayer.value = layer
    }

    /**
     * Set GPS update interval in milliseconds.
     */
    fun setGpsInterval(intervalMs: Long) {
        _gpsInterval.value = intervalMs
    }

    /**
     * Set distance units (Metric/Imperial).
     */
    fun setDistanceUnits(units: String) {
        _distanceUnits.value = units
    }

    /**
     * Set app theme (Dark/Light/System).
     */
    fun setTheme(theme: String) {
        _theme.value = theme
    }

     /**
     * Enable or disable auto-center on current location.
     */
    fun setAutoCenter(enabled: Boolean) {
        _autoCenterEnabled.value = enabled
    }
}
