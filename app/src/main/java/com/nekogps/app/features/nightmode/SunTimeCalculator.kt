package com.nekogps.app.features.nightmode

import kotlin.math.abs

/**
 * Simplified sunrise/sunset hour calculator based on latitude and season.
 * Extracted from [NightModeManager] so the manager stays under the function-count limit.
 */
object SunTimeCalculator {

    private const val BASE_SUNRISE_HOUR = 6
    private const val BASE_SUNSET_HOUR = 18
    private const val LATITUDE_BAND_DEGREES = 30
    private const val MAX_LATITUDE_OFFSET_HOURS = 2
    private const val SPRING_START_MONTH = 3
    private const val SPRING_END_MONTH = 5
    private const val SUMMER_START_MONTH = 6
    private const val SUMMER_END_MONTH = 8
    private const val FALL_START_MONTH = 9
    private const val FALL_END_MONTH = 11
    private const val EARLIEST_SUNRISE_HOUR = 4
    private const val LATEST_SUNRISE_HOUR = 9
    private const val EARLIEST_SUNSET_HOUR = 16
    private const val LATEST_SUNSET_HOUR = 22
    private const val SUMMER_SUNRISE_OFFSET = -2


    fun calculateSunrise(latitude: Double, month: Int): Int {
        // Simplified: sunrise between 5-8 AM depending on latitude and season
        val baseSunrise = BASE_SUNRISE_HOUR
        val latOffset = (abs(latitude) / LATITUDE_BAND_DEGREES).toInt()
            .coerceAtMost(MAX_LATITUDE_OFFSET_HOURS)
        val seasonalOffset = when (month) {
            in SPRING_START_MONTH..SPRING_END_MONTH -> -1 // Spring: earlier sunrise
            in SUMMER_START_MONTH..SUMMER_END_MONTH -> SUMMER_SUNRISE_OFFSET // Summer: earliest
            in FALL_START_MONTH..FALL_END_MONTH -> 0 // Fall: normal
            else -> 1 // Winter: later sunrise
        }
        return (baseSunrise + latOffset + seasonalOffset)
            .coerceIn(EARLIEST_SUNRISE_HOUR, LATEST_SUNRISE_HOUR)
    }

    fun calculateSunset(latitude: Double, month: Int): Int {
        // Simplified: sunset between 5-9 PM depending on latitude and season
        val baseSunset = BASE_SUNSET_HOUR
        val latOffset = (abs(latitude) / LATITUDE_BAND_DEGREES).toInt()
            .coerceAtMost(MAX_LATITUDE_OFFSET_HOURS)
        val seasonalOffset = when (month) {
            in SPRING_START_MONTH..SPRING_END_MONTH -> 1 // Spring: later sunset
            in SUMMER_START_MONTH..SUMMER_END_MONTH -> 2 // Summer: latest sunset
            in FALL_START_MONTH..FALL_END_MONTH -> 0 // Fall: normal
            else -> -1 // Winter: earlier sunset
        }
        return (baseSunset + latOffset + seasonalOffset)
            .coerceIn(EARLIEST_SUNSET_HOUR, LATEST_SUNSET_HOUR)
    }
}
