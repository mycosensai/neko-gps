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

    private const val EQUATORIAL_RADIUS = 6_378_137.0
    private const val ECCENTRICITY_SQUARED = 0.00669438
    private const val K0 = 0.9996

    // Angular unit conversion.
    private const val MINUTES_PER_DEGREE = 60.0
    private const val MINUTES_PER_DEGREE_INT = 60
    private const val SECONDS_PER_DEGREE = 3600.0

    // UTM zone geometry.
    private const val UTM_ZONE_WIDTH = 6.0
    private const val UTM_ZONE_WIDTH_INT = 6
    private const val LON_OFFSET_DEGREES = 180.0
    private const val LON_OFFSET_DEGREES_INT = 180
    private const val ZONE_ORIGIN_OFFSET = 3.0
    private const val ZONE_ORIGIN_OFFSET_INT = 3
    private const val UTM_FALSE_EASTING = 500000.0
    private const val UTM_SOUTHERN_HEMISPHERE_OFFSET = 10_000_000.0

    // DMS string parsing.
    private const val MIN_DMS_PART_COUNT = 4
    private const val DMS_DIRECTION_PART_INDEX = 3

    // Meridional arc series coefficients (Snyder formulae).
    private const val ARC_E2_DENOMINATOR = 4
    private const val ARC_E4_NUMERATOR = 3
    private const val ARC_E4_DENOMINATOR = 64
    private const val ARC_E6_NUMERATOR = 5
    private const val ARC_E6_DENOMINATOR = 256
    private const val ARC_SIN2_E2_NUMERATOR = 3
    private const val ARC_SIN2_E2_DENOMINATOR = 8
    private const val ARC_SIN2_E4_NUMERATOR = 3
    private const val ARC_SIN2_E4_DENOMINATOR = 32
    private const val ARC_SIN2_E6_NUMERATOR = 45
    private const val ARC_SIN2_E6_DENOMINATOR = 1024
    private const val ARC_SIN4_E4_NUMERATOR = 15
    private const val ARC_SIN4_E4_DENOMINATOR = 256
    private const val ARC_SIN4_E6_NUMERATOR = 45
    private const val ARC_SIN4_E6_DENOMINATOR = 1024
    private const val ARC_SIN6_E6_NUMERATOR = 35
    private const val ARC_SIN6_E6_DENOMINATOR = 3072
    private const val ARC_SIN4_MULTIPLIER = 4
    private const val ARC_SIN6_MULTIPLIER = 6

    // Easting series coefficients.
    private const val EASTING_DENOMINATOR_A3 = 6
    private const val EASTING_COEF_5 = 5
    private const val EASTING_COEF_18 = 18
    private const val EASTING_COEF_72 = 72
    private const val EASTING_COEF_58 = 58
    private const val EASTING_DENOMINATOR_A5 = 120

    // Northing series coefficients.
    private const val NORTHING_COEF_5 = 5
    private const val NORTHING_COEF_9 = 9
    private const val NORTHING_COEF_4 = 4
    private const val NORTHING_DENOMINATOR_A4 = 24
    private const val NORTHING_COEF_61 = 61
    private const val NORTHING_COEF_58 = 58
    private const val NORTHING_COEF_600 = 600
    private const val NORTHING_COEF_330 = 330
    private const val NORTHING_DENOMINATOR_A6 = 720

    // Footpoint latitude (phi1) series coefficients.
    private const val PHI1_SIN2_E2_NUMERATOR = 3
    private const val PHI1_SIN2_E6_NUMERATOR = 27
    private const val PHI1_SIN2_E6_DENOMINATOR = 32
    private const val PHI1_SIN4_E4_NUMERATOR = 21
    private const val PHI1_SIN4_E4_DENOMINATOR = 16
    private const val PHI1_SIN4_E6_NUMERATOR = 55
    private const val PHI1_SIN4_E6_DENOMINATOR = 32
    private const val PHI1_SIN4_E6_TERM_DENOMINATOR = 4
    private const val PHI1_SIN6_E6_NUMERATOR = 151
    private const val PHI1_SIN6_E6_DENOMINATOR = 96
    private const val PHI1_SIN6_TERM_DENOMINATOR = 6

    // Inverse latitude series coefficients.
    private const val LAT_COEF_5 = 5
    private const val LAT_COEF_3 = 3
    private const val LAT_COEF_10 = 10
    private const val LAT_COEF_4 = 4
    private const val LAT_COEF_9 = 9
    private const val LAT_DENOMINATOR_D4 = 24
    private const val LAT_COEF_61 = 61
    private const val LAT_COEF_90 = 90
    private const val LAT_COEF_298 = 298
    private const val LAT_COEF_45 = 45
    private const val LAT_COEF_252 = 252
    private const val LAT_DENOMINATOR_D6 = 720

    // Inverse longitude series coefficients.
    private const val LON_DENOMINATOR_D3 = 6
    private const val LON_COEF_5 = 5
    private const val LON_COEF_28 = 28
    private const val LON_COEF_3 = 3
    private const val LON_COEF_8 = 8
    private const val LON_COEF_24 = 24
    private const val LON_DENOMINATOR_D5 = 120

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
            val secondsStr = String.format(java.util.Locale.getDefault(), "%.2f", seconds)
            return "${degrees}°${minutes}'${secondsStr}\"$direction\""
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
            val locale = java.util.Locale.getDefault()
            val eastingStr = String.format(locale, "%.2f", easting)
            val northingStr = String.format(locale, "%.2f", northing)
            return "$zone$hemisphere $eastingStr" + "E ${northingStr}N"
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
        val minutesFull = (absVal - degrees) * MINUTES_PER_DEGREE_INT
        val minutes = floor(minutesFull).toInt()
        val seconds = (minutesFull - minutes) * MINUTES_PER_DEGREE_INT

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
        val decimal = dms.degrees + dms.minutes / MINUTES_PER_DEGREE + dms.seconds / SECONDS_PER_DEGREE
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
        if (parts.size < MIN_DMS_PART_COUNT) return 0.0

        val degrees = parts[0].toDoubleOrNull() ?: 0.0
        val minutes = parts[1].toDoubleOrNull() ?: 0.0
        val seconds = parts[2].toDoubleOrNull() ?: 0.0
        val direction = parts[DMS_DIRECTION_PART_INDEX].uppercase()

        val decimal = degrees + minutes / MINUTES_PER_DEGREE + seconds / SECONDS_PER_DEGREE
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
        val lonOrigin = Math.toRadians(
            (zone - 1) * UTM_ZONE_WIDTH - LON_OFFSET_DEGREES + ZONE_ORIGIN_OFFSET
        )
        val eccPrimeSquared = ECCENTRICITY_SQUARED / (1 - ECCENTRICITY_SQUARED)

        val n = EQUATORIAL_RADIUS / sqrt(1 - ECCENTRICITY_SQUARED * sin(latRad) * sin(latRad))
        val t = Math.tan(latRad) * Math.tan(latRad)
        val c = eccPrimeSquared * cos(latRad) * cos(latRad)
        val a = cos(latRad) * (lonRad - lonOrigin)

        val e2 = ECCENTRICITY_SQUARED
        val m = EQUATORIAL_RADIUS * (
                (1 - e2 / ARC_E2_DENOMINATOR
                        - ARC_E4_NUMERATOR * e2 * e2 / ARC_E4_DENOMINATOR
                        - ARC_E6_NUMERATOR * e2 * e2 * e2 / ARC_E6_DENOMINATOR) * latRad
                        - (ARC_SIN2_E2_NUMERATOR * e2 / ARC_SIN2_E2_DENOMINATOR
                        + ARC_SIN2_E4_NUMERATOR * e2 * e2 / ARC_SIN2_E4_DENOMINATOR
                        + ARC_SIN2_E6_NUMERATOR * e2 * e2 * e2 / ARC_SIN2_E6_DENOMINATOR) *
                        sin(2 * latRad)
                        + (ARC_SIN4_E4_NUMERATOR * e2 * e2 / ARC_SIN4_E4_DENOMINATOR
                        + ARC_SIN4_E6_NUMERATOR * e2 * e2 * e2 / ARC_SIN4_E6_DENOMINATOR) *
                        sin(ARC_SIN4_MULTIPLIER * latRad)
                        - (ARC_SIN6_E6_NUMERATOR * e2 * e2 * e2 / ARC_SIN6_E6_DENOMINATOR) *
                        sin(ARC_SIN6_MULTIPLIER * latRad)
                )

        val easting = K0 * n * (a + (1 - t + c) * a * a * a / EASTING_DENOMINATOR_A3
                + (EASTING_COEF_5 - EASTING_COEF_18 * t + t * t +
                EASTING_COEF_72 * c - EASTING_COEF_58 * eccPrimeSquared) *
                a * a * a * a * a / EASTING_DENOMINATOR_A5) +
                UTM_FALSE_EASTING

        var northing = K0 * (m + n * Math.tan(latRad) * (a * a / 2
                + (NORTHING_COEF_5 - t + NORTHING_COEF_9 * c +
                NORTHING_COEF_4 * c * c) * a * a * a * a / NORTHING_DENOMINATOR_A4
                + (NORTHING_COEF_61 - NORTHING_COEF_58 * t + t * t +
                NORTHING_COEF_600 * c - NORTHING_COEF_330 * eccPrimeSquared) *
                a * a * a * a * a * a / NORTHING_DENOMINATOR_A6))

        if (latitude < 0) {
            northing += UTM_SOUTHERN_HEMISPHERE_OFFSET
        }

        return UTM(zone, hemisphere, easting, northing)
    }

    /**
     * Convert UTM to decimal degrees.
     * @param utm UTM coordinate
     * @return Pair of (latitude, longitude)
     */
    fun utmToDecimal(utm: UTM): Pair<Double, Double> {
        val easting = utm.easting - UTM_FALSE_EASTING
        val northing = if (utm.hemisphere == "S") {
            utm.northing - UTM_SOUTHERN_HEMISPHERE_OFFSET
        } else {
            utm.northing
        }

        val e2 = ECCENTRICITY_SQUARED
        val m = northing / K0
        val mu = m / (EQUATORIAL_RADIUS * (1 - e2 / ARC_E2_DENOMINATOR
                - ARC_E4_NUMERATOR * e2 * e2 / ARC_E4_DENOMINATOR
                - ARC_E6_NUMERATOR * e2 * e2 * e2 / ARC_E6_DENOMINATOR))

        val phi1Rad = mu + (PHI1_SIN2_E2_NUMERATOR * e2 / 2
                - PHI1_SIN2_E6_NUMERATOR * e2 * e2 * e2 / PHI1_SIN2_E6_DENOMINATOR) * sin(2 * mu) +
                (PHI1_SIN4_E4_NUMERATOR * e2 * e2 / PHI1_SIN4_E4_DENOMINATOR
                - PHI1_SIN4_E6_NUMERATOR * e2 * e2 * e2 / PHI1_SIN4_E6_DENOMINATOR) *
                sin(PHI1_SIN4_E6_TERM_DENOMINATOR * mu) +
                (PHI1_SIN6_E6_NUMERATOR * e2 * e2 * e2 / PHI1_SIN6_E6_DENOMINATOR) *
                sin(PHI1_SIN6_TERM_DENOMINATOR * mu)

        val n1 = EQUATORIAL_RADIUS / sqrt(1 - e2 * sin(phi1Rad) * sin(phi1Rad))
        val t1 = Math.tan(phi1Rad) * Math.tan(phi1Rad)
        val c1 = e2 * cos(phi1Rad) * cos(phi1Rad) / (1 - e2)
        val r1 = EQUATORIAL_RADIUS * (1 - e2) /
                sqrt((1 - e2 * sin(phi1Rad) * sin(phi1Rad)) *
                        (1 - e2 * sin(phi1Rad) * sin(phi1Rad)) *
                        (1 - e2 * sin(phi1Rad) * sin(phi1Rad)))
        val d = easting / (n1 * K0)

        val lat = phi1Rad - (n1 * Math.tan(phi1Rad) / r1) *
                (d * d / 2
                        - (LAT_COEF_5 + LAT_COEF_3 * t1 + LAT_COEF_10 * c1 -
                        LAT_COEF_4 * c1 * c1 - LAT_COEF_9 * e2) *
                        d * d * d * d / LAT_DENOMINATOR_D4
                        + (LAT_COEF_61 + LAT_COEF_90 * t1 + LAT_COEF_298 * c1 +
                        LAT_COEF_45 * t1 * t1 - LAT_COEF_252 * e2 -
                        LAT_COEF_3 * c1 * c1) *
                        d * d * d * d * d * d / LAT_DENOMINATOR_D6)
        val lon = (d - (1 + 2 * t1 + c1) * d * d * d / LON_DENOMINATOR_D3
                + (LON_COEF_5 - 2 * c1 + LON_COEF_28 * t1 - LON_COEF_3 * c1 * c1 +
                LON_COEF_8 * e2 + LON_COEF_24 * t1 * t1) *
                d * d * d * d * d / LON_DENOMINATOR_D5) /
                cos(phi1Rad)

        val latitude = Math.toDegrees(lat)
        val longitude = Math.toDegrees(lon) +
                (utm.zone - 1) * UTM_ZONE_WIDTH_INT - LON_OFFSET_DEGREES_INT + ZONE_ORIGIN_OFFSET_INT

        return Pair(latitude, longitude)
    }

    /**
     * Get UTM zone number from longitude.
     */
    fun getUTMZone(longitude: Double): Int {
        return ((longitude + LON_OFFSET_DEGREES_INT) / UTM_ZONE_WIDTH_INT).toInt() + 1
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
            else -> String.format(java.util.Locale.getDefault(), "%.6f, %.6f", lat, lon)
        }
    }
}
