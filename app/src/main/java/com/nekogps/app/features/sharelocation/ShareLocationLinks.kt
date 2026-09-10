package com.nekogps.app.features.sharelocation

import java.util.Locale

/**
 * Link and coordinate formatting foundation for [ShareLocation].
 * Holds the pure formatting helpers so the sharer stays under the function-count limit.
 */
open class ShareLocationLinks {

    fun getGoogleMapsLink(latitude: Double, longitude: Double): String {
        return "https://maps.google.com/?q=$latitude,$longitude"
    }

    fun getShortLink(latitude: Double, longitude: Double): String {
        return "https://maps.goo.gle/?q=$latitude,$longitude"
    }

    fun getCoordinatesString(latitude: Double, longitude: Double): String {
        return String.format(Locale.US, "%.6f, %.6f", latitude, longitude)
    }
}
