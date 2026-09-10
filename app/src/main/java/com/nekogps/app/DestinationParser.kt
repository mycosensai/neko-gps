package com.nekogps.app

import org.osmdroid.util.GeoPoint

/**
 * Parses destination input ("lat,lng") into map points for [NavigationActivity].
 * Pure logic extracted so the activity stays under the function-count limit.
 */
object DestinationParser {

    private const val MIN_LATITUDE = -90.0
    private const val MAX_LATITUDE = 90.0
    private const val MIN_LONGITUDE = -180.0
    private const val MAX_LONGITUDE = 180.0
    private const val EXPECTED_PART_COUNT = 2


    fun parse(text: String): GeoPoint? {
        val parts = text.split(",").map { it.trim() }
        if (parts.size != EXPECTED_PART_COUNT) return null
        return toGeoPoint(parts[0].toDoubleOrNull(), parts[1].toDoubleOrNull())
    }

    private fun toGeoPoint(lat: Double?, lng: Double?): GeoPoint? {
        if (lat == null || lng == null) return null
        return if (isValidLatitude(lat) && isValidLongitude(lng)) GeoPoint(lat, lng) else null
    }

    private fun isValidLatitude(lat: Double): Boolean {
        return lat in MIN_LATITUDE..MAX_LATITUDE
    }

    private fun isValidLongitude(lng: Double): Boolean {
        return lng in MIN_LONGITUDE..MAX_LONGITUDE
    }
}
