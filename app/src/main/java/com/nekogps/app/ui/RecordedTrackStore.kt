package com.nekogps.app.ui

import com.nekogps.app.utils.DistanceCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.osmdroid.util.GeoPoint

/**
 * Track-recording foundation for [MapStateManager].
 * Owns the recorded-track list and the in-progress recording session so the
 * singleton stays focused on live location and preference state.
 */
open class RecordedTrackStore {

    protected val recordingTracksState = MutableStateFlow<List<List<GeoPoint>>>(emptyList())
    val recordedTracks: StateFlow<List<List<GeoPoint>>> = recordingTracksState.asStateFlow()

    protected val isRecordingState = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = isRecordingState.asStateFlow()

    protected val currentRecordingPointsState = mutableListOf<GeoPoint>()

    /**
     * Add a completed track to the recorded tracks list.
     */
    fun addTrack(points: List<GeoPoint>) {
        if (points.isNotEmpty()) {
            recordingTracksState.value = recordingTracksState.value + listOf(points)
        }
    }

    /**
     * Clear all recorded tracks.
     */
    fun clearTracks() {
        recordingTracksState.value = emptyList()
    }

    /**
     * Remove a specific track by index.
     */
    fun removeTrack(index: Int) {
        val tracks = recordingTracksState.value.toMutableList()
        if (index in tracks.indices) {
            tracks.removeAt(index)
            recordingTracksState.value = tracks
        }
    }

    /**
     * Start recording a new track.
     */
    fun startRecording() {
        currentRecordingPointsState.clear()
        isRecordingState.value = true
    }

    /**
     * Stop recording and save the current track.
     * @return The recorded track points, or empty if recording was not active.
     */
    fun stopRecording(): List<GeoPoint> {
        isRecordingState.value = false
        val points = currentRecordingPointsState.toList()
        if (points.isNotEmpty()) {
            addTrack(points)
        }
        currentRecordingPointsState.clear()
        return points
    }

    /**
     * Get the number of recorded tracks.
     */
    fun getTrackCount(): Int = recordingTracksState.value.size

    /**
     * Get total distance across all recorded tracks in meters.
     */
    fun getTotalDistance(): Double {
        var total = 0.0
        recordingTracksState.value.forEach { track ->
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
