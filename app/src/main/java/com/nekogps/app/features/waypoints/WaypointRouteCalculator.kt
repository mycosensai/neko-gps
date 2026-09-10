package com.nekogps.app.features.waypoints

import android.location.Location

/**
 * Route-math foundation for [WaypointManager].
 * Owns distance, ETA, and summary calculations so the manager stays focused
 * on waypoint persistence and ordering.
 */
open class WaypointRouteCalculator {

    companion object {
        private const val METERS_PER_KILOMETER_F = 1000f
        private const val METERS_PER_KILOMETER = 1000
        private const val MINUTES_PER_HOUR = 60
    }

    /**
     * Calculate total route distance in meters.
     */
    fun calculateTotalDistance(waypoints: List<WaypointEntity>): Float {
        if (waypoints.size < 2) return 0f

        var totalDistance = 0f
        val results = FloatArray(1)

        for (i in 0 until waypoints.size - 1) {
            Location.distanceBetween(
                waypoints[i].latitude, waypoints[i].longitude,
                waypoints[i + 1].latitude, waypoints[i + 1].longitude,
                results
            )
            totalDistance += results[0]
        }

        return totalDistance
    }

    /**
     * Calculate total route distance including start and end points.
     */
    fun calculateTotalDistanceWithEndpoints(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        waypoints: List<WaypointEntity>
    ): Float {
        if (waypoints.isEmpty()) {
            val results = FloatArray(1)
            Location.distanceBetween(startLat, startLon, endLat, endLon, results)
            return results[0]
        }

        var totalDistance = 0f
        val results = FloatArray(1)

        // Start to first waypoint
        Location.distanceBetween(
            startLat, startLon,
            waypoints.first().latitude, waypoints.first().longitude,
            results
        )
        totalDistance += results[0]

        // Between waypoints
        totalDistance += calculateTotalDistance(waypoints)

        // Last waypoint to end
        Location.distanceBetween(
            waypoints.last().latitude, waypoints.last().longitude,
            endLat, endLon,
            results
        )
        totalDistance += results[0]

        return totalDistance
    }

    /**
     * Estimate ETA in minutes based on distance and average speed.
     * @param distanceMeters total distance in meters
     * @param averageSpeedKmh average speed in km/h (default 50 km/h for driving)
     */
    fun estimateETA(distanceMeters: Float, averageSpeedKmh: Float = 50f): Int {
        val distanceKm = distanceMeters / METERS_PER_KILOMETER_F
        val timeHours = distanceKm / averageSpeedKmh
        return (timeHours * MINUTES_PER_HOUR).toInt().coerceAtLeast(1)
    }

    /**
     * Format distance for display.
     */
    fun formatDistance(distanceMeters: Float): String {
        return if (distanceMeters >= METERS_PER_KILOMETER) {
            String.format(
                java.util.Locale.getDefault(),
                "%.1f km",
                distanceMeters / METERS_PER_KILOMETER
            )
        } else {
            String.format(java.util.Locale.getDefault(), "%.0f m", distanceMeters)
        }
    }

    /**
     * Format ETA for display.
     */
    fun formatETA(etaMinutes: Int): String {
        return when {
            etaMinutes >= MINUTES_PER_HOUR ->
                "${etaMinutes / MINUTES_PER_HOUR}h ${etaMinutes % MINUTES_PER_HOUR}m"
            etaMinutes > 0 -> "$etaMinutes min"
            else -> "< 1 min"
        }
    }

    /**
     * Get route summary with total distance and ETA.
     */
    fun getRouteSummary(
        waypoints: List<WaypointEntity>,
        averageSpeedKmh: Float = 50f
    ): RouteSummary {
        val totalDistance = calculateTotalDistance(waypoints)
        val eta = estimateETA(totalDistance, averageSpeedKmh)
        return RouteSummary(
            totalDistanceMeters = totalDistance,
            estimatedTimeMinutes = eta,
            waypointCount = waypoints.size
        )
    }
}
