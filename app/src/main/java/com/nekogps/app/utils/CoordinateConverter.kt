package com.nekogps.app.utils

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Utility for converting between coordinate formats:
 * - Decimal Degrees (DD)
 * - Degrees Minutes Seconds (DMS)
 * - Universal Transverse Mercator (UTM)
 */
object CoordinateConverter {

    private const val EARTH_RADIUS = 6_371_000.0
    private const val UTM_SCALE_FACTOR = 0.9996
    private const val EQUATORIAL_RADIUS = 6_378_137.0
    private const val ECCENTRICITY_SQUARED = 0.00669438
    private const val K0 = 0.9996

    /**
     * DMS representation of a coordinate.
     */
    data class DMS(
        val degrees: Int,
        val minutes: Int,
        val seconds: Double,
        val direction: String // N, S, E, W
    ) {
        override fun toString(): String {
            return "${degrees}°${minutes}'${String.format("%.2f", seconds)}\"$direction"
        }
    }

    /**
     * UTM coordinate representation.
     */
    data class UTM(
        val zone: Int,
        val hemisphere: String, // N or S
        val easting: Double,
        val northing: Double
    ) {
        override fun toString(): String {
            return "$zone$hemisphere ${String.format("%.2f", easting)}E ${String.format("%.2f", northing)}N"
        }
    }

    /**
     * Convert decimal degrees to DMS.
     * @param decimalDegrees Coordinate in decimal degrees
     * @param isLatitude True for latitude, False for longitude
     * @return DMS representation
     */
    fun decimalToDMS(decimalDegrees: Double, isLatitude: Boolean): DMS {
        val absVal = abs(decimalDegrees)
        val degrees = floor(absVal).toInt()
        val minutesFull = (absVal - degrees) * 60
        val minutes = floor(minutesFull).toInt()
        val seconds = (minutesFull - minutes) * 60

        val direction = if (isLatitude) {
            if (decimalDegrees >= 0) "N" else "S"
        } else {
            if (decimalDegrees >= 0) "E" else "W"
        }

        return DMS(degrees, minutes, seconds, direction)
    }

    /**
     * Convert DMS to decimal degrees.
     * @param dms DMS representation
     * @return Decimal degrees
     */
    fun dmsToDecimal(dms: DMS): Double {
        val decimal = dms.degrees + dms.minutes / 60.0 + dms.seconds / 3600.0
        return when (dms.direction) {
            "S", "W" -> -decimal
            else -> decimal
        }
    }

    /**
     * Convert decimal degrees string in DMS format to decimal.
     * Accepts formats like: 40°26'46"N, 40 26 46 N, 40d 26m 46s N
     */
    fun dmsStringToDecimal(dmsStr: String): Double {
        val cleaned = dmsStr.trim()
            .replace("°", " ")
            .replace("'", " ")
            .replace("\"", " ")
            .replace("d", " ")
            .replace("m", " ")
            .replace("s", " ")
            .replace(",", " ")
            .replace(Regex("\\s+"), " ")

        val parts = cleaned.split(" ").filter { it.isNotEmpty() }
        if (parts.size < 4) return 0.0

        val degrees = parts[0].toDoubleOrNull() ?: 0.0
        val minutes = parts[1].toDoubleOrNull() ?: 0.0
        val seconds = parts[2].toDoubleOrNull() ?: 0.0
        val direction = parts[3].uppercase()

        val decimal = degrees + minutes / 60.0 + seconds / 3600.0
        return when (direction) {
            "S", "W" -> -decimal
            else -> decimal
        }
    }

    /**
     * Convert decimal degrees to UTM.
     * @param latitude Latitude in decimal degrees
     * @param longitude Longitude in decimal degrees
     * @return UTM representation
     */
    fun decimalToUTM(latitude: Double, longitude: Double): UTM {
        val zone = getUTMZone(longitude)
        val hemisphere = if (latitude >= 0) "N" else "S"

        val latRad = Math.toRadians(latitude)
        val lonRad = Math.toRadians(longitude)
        val lonOrigin = Math.toRadians((zone - 1) * 6.0 - 180.0 + 3.0)
        val eccPrimeSquared = ECCENTRICITY_SQUARED / (1 - ECCENTRICITY_SQUARED)

        val n = EQUATORIAL_RADIUS / sqrt(1 - ECCENTRICITY_SQUARED * sin(latRad) * sin(latRad))
        val t = Math.tan(latRad) * Math.tan(latRad)
        val c = eccPrimeSquared * cos(latRad) * cos(latRad)
        val a = cos(latRad) * (lonRad - lonOrigin)

        val m = EQUATORIAL_RADIUS * (
                (1 - ECCENTRICITY_SQUARED / 4
                        - 3 * ECCENTRICITY_SQUARED * ECCENTRICITY_SQUARED / 64
                        - 5 * ECCENTRICITY_SQUARED * ECCENTRICITY_SQUARED * ECCENTRICITY_SQUARED / 256) * latRad
                        - (3 * ECCENTRICITY_SQUARED / 8
                        + 3 * ECCENTRICITY_SQUARED * ECCENTRICITY_SQUARED / 32
                        + 45 * ECCENTRICITY_SQUARED * ECCENTRICITY_SQUARED * ECCENTRICITY_SQUARED / 1024) * sin(2 * latRad)
                        + (15 * ECCENTRICITY_SQUARED * ECCENTRICITY_SQUARED / 256
                        + 45 * ECCENTRICITY_SQUARED * ECCENTRICITY_SQUARED * ECCENTRICITY_SQUARED / 1024) * sin(4 * latRad)
                        - (35 * ECCENTRICITY_SQUARED * ECCENTRICITY_SQUARED * ECCENTRICITY_SQUARED / 3072) * sin(6 * latRad)
                )

        val easting = K0 * n * (a + (1 - t + c) * a * a * a / 6
                + (5 - 18 * t + t * t + 72 * c - 58 * eccPrimeSquared) * a * a * a * a * a / 120)
                + 500000.0

        var northing = K0 * (m + n * Math.tan(latRad) * (a * a / 2
                + (5 - t + 9 * c + 4 * c * c) * a * a * a * a / 24
                + (61 - 58 * t + t * t + 600 * c - 330 * eccPrimeSquared) * a * a * a * a * a * a / 720))

        if (latitude < 0) {
            northing += 10_000_000.0
        }

        return UTM(zone, hemisphere, easting, northing)
    }

    /**
     * Convert UTM to decimal degrees.
     * @param utm UTM coordinate
     * @return Pair of (latitude, longitude)
     */
    fun utmToDecimal(utm: UTM): Pair<Double, Double> {
        val easting = utm.easting - 500000.0
        val northing = if (utm.hemisphere == "S") utm.northing - 10_000_000.0 else utm.northing

        val m = northing / K0
        val mu = m / (EQUATORIAL_RADIUS * (1 - ECCENTRICITY_SQUARED / 4
                - 3 * ECCENTRICITY_SQUARED * ECCENTRICITY_SQUARED / 64
                - 5 * ECCENTRICITY_SQUARED * ECCENTRICITY_SQUARED * ECCENTRICITY_SQUARED / 256))

        val phi1Rad = mu + (3 * ECCENTRICITY_SQUARED / 2
                - 27 * ECCENTRICITY_SQUARED * ECCENTRICITY_SQUARED * ECCENTRICITY_SQUARED / 32) * sin(2 * mu)
                + (21 * ECCENTRICITY_SQUARED * ECCENTRICITY_SQUARED / 16
                - 55 * ECCENTRICITY_SQUARED * ECCENTRICITY_SQUARED * ECCENTRICITY_SQUARED / 32) * sin(4 * mu)
                + (151 * ECCENTRICITY_SQUARED * ECCENTRICITY_SQUARED * ECCENTRICITY_SQUARED / 96) * sin(6 * mu)

        val n1 = EQUATORIAL_RADIUS / sqrt(1 - ECCENTRICITY_SQUARED * sin(phi1Rad) * sin(phi1Rad))
        val t1 = Math.tan(phi1Rad) * Math.tan(phi1Rad)
        val c1 = ECCENTRICITY_SQUARED * cos(phi1Rad) * cos(phi1Rad) / (1 - ECCENTRICITY_SQUARED)
        val r1 = EQUATORIAL_RADIUS * (1 - ECCENTRICITY_SQUARED) /
                sqrt((1 - ECCENTRICITY_SQUARED * sin(phi1Rad) * sin(phi1Rad)) *
                        (1 - ECCENTRICITY_SQUARED * sin(phi1Rad) * sin(phi1Rad)) *
                        (1 - ECCENTRICITY_SQUARED * sin(phi1Rad) * sin(phi1Rad)))
        val d = easting / (n1 * K0)

        val lat = phi1Rad - (n1 * Math.tan(phi1Rad) / r1) *
                (d * d / 2
                        - (5 + 3 * t1 + 10 * c1 - 4 * c1 * c1 - 9 * ECCENTRICITY_SQUARED) * d * d * d * d / 24
                        + (61 + 90 * t1 + 298 * c1 + 45 * t1 * t1 - 252 * ECCENTRICITY_SQUARED - 3 * c1 * c1) * d * d * d * d * d * d / 720)
        val lon = (d - (1 + 2 * t1 + c1) * d * d * d / 6
                + (5 - 2 * c1 + 28 * t1 - 3 * c1 * c1 + 8 * ECCENTRICITY_SQUARED + 24 * t1 * t1) * d * d * d * d * d / 120) /
                cos(phi1Rad)

        val latitude = Math.toDegrees(lat)
        val longitude = Math.toDegrees(lon) + (utm.zone - 1) * 6 - 180 + 3

        return Pair(latitude, longitude)
    }

    /**
     * Get UTM zone number from longitude.
     */
    fun getUTMZone(longitude: Double): Int {
        return ((longitude + 180) / 6).toInt() + 1
    }

    /**
     * Format coordinate pair as a human-readable string.
     */
    fun formatCoordinate(lat: Double, lon: Double, format: String = "DD"): String {
        return when (format.uppercase()) {
            "DMS" -> {
                val latDMS = decimalToDMS(lat, true)
                val lonDMS = decimalToDMS(lon, false)
                "$latDMS, $lonDMS"
            }
            "UTM" -> {
                val utm = decimalToUTM(lat, lon)
                utm.toString()
            }
            else -> String.format("%.6f, %.6f", lat, lon)
        }
    }
}
