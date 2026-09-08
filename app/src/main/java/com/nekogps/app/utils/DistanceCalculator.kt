package com.nekogps.app.utils

import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Utility class for distance calculations, bearing computation, and ETA estimation.
 */
object DistanceCalculator {

    private const val EARTH_RADIUS_METERS = 6_371_000.0
    private const val EARTH_RADIUS_MILES = 3_959.0
    private const val METERS_PER_KM = 1000.0
    private const val METERS_PER_MILE = 1609.344

    /**
     * Calculate the great-circle distance between two coordinates using the Haversine formula.
     * @param lat1 Latitude of point 1 in degrees
     * @param lon1 Longitude of point 1 in degrees
     * @param lat2 Latitude of point 2 in degrees
     * @param lon2 Longitude of point 2 in degrees
     * @return Distance in meters
     */
    fun haversineDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Calculate the initial bearing from point 1 to point 2.
     * @param lat1 Latitude of point 1 in degrees
     * @param lon1 Longitude of point 1 in degrees
     * @param lat2 Latitude of point 2 in degrees
     * @param lon2 Longitude of point 2 in degrees
     * @return Bearing in degrees (0-360, where 0 is North)
     */
    fun calculateBearing(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val dLon = Math.toRadians(lon2 - lon1)

        val y = sin(dLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) -
                sin(lat1Rad) * cos(lat2Rad) * cos(dLon)

        val bearingRad = atan2(y, x)
        val bearingDeg = Math.toDegrees(bearingRad)
        return (bearingDeg + 360) % 360
    }

    /**
     * Estimate time of arrival based on distance and average speed.
     * @param distanceMeters Distance in meters
     * @param avgSpeedKmh Average speed in km/h
     * @return ETA in minutes
     */
    fun estimateETA(distanceMeters: Double, avgSpeedKmh: Double): Double {
        if (avgSpeedKmh <= 0) return Double.MAX_VALUE
        val distanceKm = distanceMeters / METERS_PER_KM
        return (distanceKm / avgSpeedKmh) * 60.0
    }

    /**
     * Format distance for display.
     * @param meters Distance in meters
     * @param useImperial Whether to use imperial units
     * @return Formatted string (e.g., "1.23 km" or "0.76 mi")
     */
    fun formatDistance(meters: Double, useImperial: Boolean = false): String {
        return if (useImperial) {
            val miles = meters / METERS_PER_MILE
            if (miles < 0.1) {
                String.format(Locale.US, "%.0f ft", meters * 3.28084)
            } else {
                String.format(Locale.US, "%.2f mi", miles)
            }
        } else {
            if (meters < 1000) {
                String.format(Locale.US, "%.0f m", meters)
            } else {
                String.format(Locale.US, "%.2f km", meters / METERS_PER_KM)
            }
        }
    }

    /**
     * Format ETA for display.
     * @param minutes ETA in minutes
     * @return Formatted string (e.g., "5 min", "1h 23min")
     */
    fun formatETA(minutes: Double): String {
        return when {
            minutes < 1.0 -> "< 1 min"
            minutes < 60.0 -> String.format(Locale.US, "%.0f min", minutes)
            else -> {
                val hours = (minutes / 60).toInt()
                val mins = (minutes % 60).toInt()
                String.format(Locale.US, "%dh %dm", hours, mins)
            }
        }
    }

    /**
     * Calculate total distance of a path defined by a list of coordinates.
     * @param points List of coordinate pairs (lat, lon)
     * @return Total distance in meters
     */
    fun calculatePathDistance(points: List<Pair<Double, Double>>): Double {
        var total = 0.0
        for (i in 0 until points.size - 1) {
            total += haversineDistance(
                points[i].first, points[i].second,
                points[i + 1].first, points[i + 1].second
            )
        }
        return total
    }

    /**
     * Convert meters to feet.
     */
    fun metersToFeet(meters: Double): Double = meters * 3.28084

    /**
     * Convert meters to miles.
     */
    fun metersToMiles(meters: Double): Double = meters / METERS_PER_MILE

    /**
     * Convert km/h to m/s.
     */
    fun kmhToMs(kmh: Double): Double = kmh / 3.6
}
