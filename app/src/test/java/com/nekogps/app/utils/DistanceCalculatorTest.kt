package com.nekogps.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [DistanceCalculator].
 *
 * These are pure-JVM tests (no Robolectric, no device) so they run in
 * milliseconds and gate every build.
 */
class DistanceCalculatorTest {

    // ── Haversine ────────────────────────────────────────────────────────────

    @Test
    fun `haversine of identical points is zero`() {
        val d = DistanceCalculator.haversineDistance(51.5074, -0.1278, 51.5074, -0.1278)
        assertEquals(0.0, d, 0.0001)
    }

    @Test
    fun `haversine along equator for one degree longitude matches known value`() {
        // One degree of longitude at the equator is ~111.19 km on a 6371 km sphere.
        val d = DistanceCalculator.haversineDistance(0.0, 0.0, 0.0, 1.0)
        assertEquals(111_194.9, d, 50.0)
    }

    @Test
    fun `haversine along meridian for one degree latitude matches known value`() {
        val d = DistanceCalculator.haversineDistance(0.0, 0.0, 1.0, 0.0)
        assertEquals(111_194.9, d, 50.0)
    }

    @Test
    fun `haversine London to Paris is approximately 344 km`() {
        val d = DistanceCalculator.haversineDistance(51.5074, -0.1278, 48.8566, 2.3522)
        assertEquals(343_500.0, d, 2_000.0)
    }

    @Test
    fun `haversine is symmetric`() {
        val ab = DistanceCalculator.haversineDistance(40.7128, -74.0060, 34.0522, -118.2437)
        val ba = DistanceCalculator.haversineDistance(34.0522, -118.2437, 40.7128, -74.0060)
        assertEquals(ab, ba, 0.001)
    }

    @Test
    fun `haversine handles antimeridian crossing`() {
        // 179.9E to 179.9W is 0.2 degrees apart, not ~360.
        val d = DistanceCalculator.haversineDistance(0.0, 179.9, 0.0, -179.9)
        assertEquals(22_239.0, d, 100.0)
    }

    // ── Bearing ──────────────────────────────────────────────────────────────

    @Test
    fun `bearing due north is zero`() {
        val b = DistanceCalculator.calculateBearing(0.0, 0.0, 1.0, 0.0)
        assertEquals(0.0, b, 0.01)
    }

    @Test
    fun `bearing due east is ninety`() {
        val b = DistanceCalculator.calculateBearing(0.0, 0.0, 0.0, 1.0)
        assertEquals(90.0, b, 0.01)
    }

    @Test
    fun `bearing due south is one eighty`() {
        val b = DistanceCalculator.calculateBearing(1.0, 0.0, 0.0, 0.0)
        assertEquals(180.0, b, 0.01)
    }

    @Test
    fun `bearing due west is two seventy`() {
        val b = DistanceCalculator.calculateBearing(0.0, 1.0, 0.0, 0.0)
        assertEquals(270.0, b, 0.01)
    }

    @Test
    fun `bearing is always normalised into 0 until 360`() {
        val samples = listOf(
            Triple(0.0, 0.0, 1.0 to 1.0),
            Triple(-33.86, 151.21, -34.0 to 150.0),
            Triple(60.0, -120.0, -60.0 to 120.0)
        )
        for ((lat, lon, target) in samples) {
            val b = DistanceCalculator.calculateBearing(lat, lon, target.first, target.second)
            assertTrue("bearing $b out of range", b >= 0.0 && b < 360.0)
        }
    }

    // ── ETA ──────────────────────────────────────────────────────────────────

    @Test
    fun `eta for 60 km at 60 kmh is 60 minutes`() {
        assertEquals(60.0, DistanceCalculator.estimateETA(60_000.0, 60.0), 0.001)
    }

    @Test
    fun `eta for 30 km at 90 kmh is 20 minutes`() {
        assertEquals(20.0, DistanceCalculator.estimateETA(30_000.0, 90.0), 0.001)
    }

    @Test
    fun `eta with zero speed is max value rather than division by zero`() {
        assertEquals(
            Double.MAX_VALUE,
            DistanceCalculator.estimateETA(1_000.0, 0.0),
            0.0
        )
    }

    @Test
    fun `eta with negative speed is max value`() {
        assertEquals(
            Double.MAX_VALUE,
            DistanceCalculator.estimateETA(1_000.0, -5.0),
            0.0
        )
    }

    // ── Formatting ───────────────────────────────────────────────────────────

    @Test
    fun `formatDistance uses metres below one kilometre`() {
        assertEquals("500 m", DistanceCalculator.formatDistance(500.0))
    }

    @Test
    fun `formatDistance uses kilometres at and above one kilometre`() {
        assertEquals("1.50 km", DistanceCalculator.formatDistance(1500.0))
    }

    @Test
    fun `formatDistance imperial uses feet below a tenth of a mile`() {
        assertEquals("328 ft", DistanceCalculator.formatDistance(100.0, useImperial = true))
    }

    @Test
    fun `formatDistance imperial uses miles otherwise`() {
        assertEquals("1.00 mi", DistanceCalculator.formatDistance(1609.344, useImperial = true))
    }

    @Test
    fun `formatETA shows sub-minute as less than one minute`() {
        assertEquals("< 1 min", DistanceCalculator.formatETA(0.4))
    }

    @Test
    fun `formatETA shows minutes under an hour`() {
        assertEquals("45 min", DistanceCalculator.formatETA(45.0))
    }

    @Test
    fun `formatETA shows hours and minutes beyond an hour`() {
        assertEquals("1h 23m", DistanceCalculator.formatETA(83.0))
    }

    // ── Path distance ────────────────────────────────────────────────────────

    @Test
    fun `path distance of fewer than two points is zero`() {
        assertEquals(0.0, DistanceCalculator.calculatePathDistance(emptyList()), 0.0)
        assertEquals(
            0.0,
            DistanceCalculator.calculatePathDistance(listOf(1.0 to 2.0)),
            0.0
        )
    }

    @Test
    fun `path distance sums consecutive segments`() {
        val path = listOf(0.0 to 0.0, 0.0 to 1.0, 0.0 to 2.0)
        val d = DistanceCalculator.calculatePathDistance(path)
        // Two one-degree equatorial segments.
        assertEquals(222_390.0, d, 100.0)
    }

    // ── Unit conversions ─────────────────────────────────────────────────────

    @Test
    fun `metersToFeet converts correctly`() {
        assertEquals(3.28084, DistanceCalculator.metersToFeet(1.0), 0.00001)
    }

    @Test
    fun `metersToMiles converts correctly`() {
        assertEquals(1.0, DistanceCalculator.metersToMiles(1609.344), 0.0001)
    }

    @Test
    fun `kmhToMs converts correctly`() {
        assertEquals(10.0, DistanceCalculator.kmhToMs(36.0), 0.0001)
    }
}
