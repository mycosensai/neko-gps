package com.nekogps.app.features.stats

data class TripRecord(
    val id: Long = System.currentTimeMillis(),
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = 0,
    val totalDistanceMeters: Float = 0f,
    val totalTimeSeconds: Long = 0,
    val averageSpeedKmh: Float = 0f,
    val maxSpeedKmh: Float = 0f,
    val startLocation: String = "",
    val endLocation: String = "",
    val trackPoints: List<Pair<Double, Double>> = emptyList()
)
