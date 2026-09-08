package com.nekogps.app.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.osmdroid.util.GeoPoint

/**
 * Singleton managing shared map state across the application.
 * Holds current location, recorded tracks, and map preferences using StateFlow.
 */
object MapStateManager {

    private val _currentLocation = MutableStateFlow<GeoPoint?>(null)
    val currentLocation: StateFlow<GeoPoint?> = _currentLocation.asStateFlow()

    private val _recordedTracks = MutableStateFlow<List<List<GeoPoint>>>(emptyList())
    val recordedTracks: StateFlow<List<List<GeoPoint>>> = _recordedTracks.asStateFlow()

    private val _mapLayer = MutableStateFlow("Standard")
    val mapLayer: StateFlow<String> = _mapLayer.asStateFlow()

    private val _gpsInterval = MutableStateFlow(2000L)
    val gpsInterval: StateFlow<Long> = _gpsInterval.asStateFlow()

    private val _distanceUnits = MutableStateFlow("Metric")
    val distanceUnits: StateFlow<String> = _distanceUnits.asStateFlow()

    private val _theme = MutableStateFlow("Dark")
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _autoCenterEnabled = MutableStateFlow(true)
    val autoCenterEnabled: StateFlow<Boolean> = _autoCenterEnabled.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _currentRecordingPoints = mutableListOf<GeoPoint>()

    /**
     * Update the current GPS location.
     */
    fun updateCurrentLocation(point: GeoPoint) {
        _currentLocation.value = point

        // Add to current recording if active
        if (_isRecording.value) {
            _currentRecordingPoints.add(point)
        }
    }

    /**
     * Add a completed track to the recorded tracks list.
     */
    fun addTrack(points: List<GeoPoint>) {
        if (points.isNotEmpty()) {
            _recordedTracks.value = _recordedTracks.value + listOf(points)
        }
    }

    /**
     * Clear all recorded tracks.
     */
    fun clearTracks() {
        _recordedTracks.value = emptyList()
    }

    /**
     * Remove a specific track by index.
     */
    fun removeTrack(index: Int) {
        val tracks = _recordedTracks.value.toMutableList()
        if (index in tracks.indices) {
            tracks.removeAt(index)
            _recordedTracks.value = tracks
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

    /**
     * Start recording a new track.
     */
    fun startRecording() {
        _currentRecordingPoints.clear()
        _isRecording.value = true
    }

    /**
     * Stop recording and save the current track.
     * @return The recorded track points, or empty if recording was not active.
     */
    fun stopRecording(): List<GeoPoint> {
        _isRecording.value = false
        val points = _currentRecordingPoints.toList()
        if (points.isNotEmpty()) {
            addTrack(points)
        }
        _currentRecordingPoints.clear()
        return points
    }

    /**
     * Get the number of recorded tracks.
     */
    fun getTrackCount(): Int = _recordedTracks.value.size

    /**
     * Get total distance across all recorded tracks in meters.
     */
    fun getTotalDistance(): Double {
        var total = 0.0
        _recordedTracks.value.forEach { track ->
            for (i in 0 until track.size - 1) {
                total += DistanceCalculator.haversineDistance(
                    track[i].latitude, track[i].longitude,
                    track[i + 1].latitude, track[i + 1].longitude
                )
            }
        }
        return total
    }
}
