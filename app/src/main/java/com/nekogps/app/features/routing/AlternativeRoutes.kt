package com.nekogps.app.features.routing

import android.content.Context
import android.util.Log
import com.nekogps.app.utils.DistanceCalculator
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.math.sqrt

class AlternativeRoutes(private val context: Context) {
    companion object {
        private const val TAG = "AlternativeRoutes"
        private const val NUM_ALTERNATIVES = 3
        private const val DEFAULT_AVG_SPEED_KMH = 50.0
        private const val SCENIC_SCORE_WEIGHT = 0.3
    }

    data class AlternativeRoute(
        val id: Int, val name: String, val description: String, val routePoints: List<GeoPoint>,
        val distanceMeters: Double, val etaMinutes: Double, val preference: RoutePreference,
        val score: Double, val hasTolls: Boolean, val hasHighways: Boolean, val hasFerries: Boolean
    ) {
        enum class RoutePreference(val label: String, val icon: String) { SHORTEST("Shortest", "📏"), FASTEST("Fastest", "⚡"), SCENIC("Scenic", "🌿") }
    }

    data class AlternativeRoutesResult(val routes: List<AlternativeRoute>, val origin: GeoPoint, val destination: GeoPoint, val totalCalculated: Int)

    suspend fun calculateAlternatives(origin: GeoPoint, destination: GeoPoint, waypoints: List<GeoPoint> = emptyList(), routeOptionsManager: RouteOptionsManager? = null): AlternativeRoutesResult {
        Log.d(TAG, "Calculating alternatives from $origin to $destination")
        val alternatives = mutableListOf<AlternativeRoute>()
        alternatives.add(calculateRoute(0, origin, destination, waypoints, 0.0, AlternativeRoute.RoutePreference.SHORTEST, routeOptionsManager))
        alternatives.add(calculateRoute(1, origin, destination, waypoints, 0.3, AlternativeRoute.RoutePreference.FASTEST, routeOptionsManager))
        alternatives.add(calculateRoute(2, origin, destination, waypoints, 0.8, AlternativeRoute.RoutePreference.SCENIC, routeOptionsManager))
        val filteredAlternatives = alternatives.filter { route ->
            routeOptionsManager?.let { manager ->
                val prefs = runBlocking { manager.getPreferences() }
                !(prefs.avoidFerries && route.hasFerries)
            } ?: true
        }
        val sortedRoutes = filteredAlternatives.sortedBy { it.score }
        Log.d(TAG, "Calculated ${sortedRoutes.size} alternative routes")
        return AlternativeRoutesResult(routes = sortedRoutes, origin = origin, destination = destination, totalCalculated = sortedRoutes.size)
    }

    private suspend fun calculateRoute(id: Int, origin: GeoPoint, destination: GeoPoint, waypoints: List<GeoPoint>, offsetFactor: Double, preference: AlternativeRoute.RoutePreference, routeOptionsManager: RouteOptionsManager?): AlternativeRoute {
        val intermediatePoints = generateIntermediatePoints(origin, destination, offsetFactor)
        val routePoints = mutableListOf<GeoPoint>().apply { add(origin); if (waypoints.isNotEmpty()) { val splitPoint = waypoints.size / 2; addAll(intermediatePoints.take(intermediatePoints.size / 2)); addAll(waypoints); addAll(intermediatePoints.drop(intermediatePoints.size / 2)) } else { addAll(intermediatePoints) }; add(destination) }
        val dedupedPoints = routePoints.distinctBy { "${it.latitude},${it.longitude}" }
        var totalDistance = 0.0
        for (i in 0 until dedupedPoints.size - 1) {
            val p1 = dedupedPoints[i]; val p2 = dedupedPoints[i + 1]
            val directDist = DistanceCalculator.haversineDistance(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
            val penalizedDist = if (routeOptionsManager != null) {
                val segmentInfo = analyzeSegment(p1, p2)
                runBlocking { routeOptionsManager.calculatePenalizedDistance(directDist, segmentInfo.hasToll, segmentInfo.hasHighway, segmentInfo.hasFerry) }
            } else directDist
            totalDistance += penalizedDist
        }
        val etaMinutes = DistanceCalculator.estimateETA(totalDistance, DEFAULT_AVG_SPEED_KMH)
        val scenicScore = calculateScenicScore(dedupedPoints, origin, destination)
        val score = when (preference) { AlternativeRoute.RoutePreference.SHORTEST -> totalDistance; AlternativeRoute.RoutePreference.FASTEST -> etaMinutes; AlternativeRoute.RoutePreference.SCENIC -> scenicScore }
        val hasTolls = checkRouteFeature(dedupedPoints, "toll"); val hasHighways = checkRouteFeature(dedupedPoints, "highway"); val hasFerries = checkRouteFeature(dedupedPoints, "ferry")
        val routeName = when (preference) { AlternativeRoute.RoutePreference.SHORTEST -> "Shortest Route"; AlternativeRoute.RoutePreference.FASTEST -> "Fastest Route"; AlternativeRoute.RoutePreference.SCENIC -> "Scenic Route" }
        val routeDesc = when (preference) { AlternativeRoute.RoutePreference.SHORTEST -> "Most direct path with minimal detours"; AlternativeRoute.RoutePreference.FASTEST -> "Optimized for speed with fewer waypoints"; AlternativeRoute.RoutePreference.SCENIC -> "Longer route prioritizing scenic views" }
        return AlternativeRoute(id, routeName, routeDesc, dedupedPoints, totalDistance, etaMinutes, preference, score, hasTolls, hasHighways, hasFerries)
    }

    private fun generateIntermediatePoints(origin: GeoPoint, destination: GeoPoint, offsetFactor: Double): List<GeoPoint> {
        val points = mutableListOf<GeoPoint>()
        val midLat = (origin.latitude + destination.latitude) / 2.0
        val midLon = (origin.longitude + destination.longitude) / 2.0
        val latDiff = destination.latitude - origin.latitude
        val lonDiff = destination.longitude - origin.longitude
        val perpLat = -lonDiff * offsetFactor
        val perpLon = latDiff * offsetFactor
        points.add(GeoPoint(midLat + perpLat * 0.3, midLon + perpLon * 0.3))
        points.add(GeoPoint(midLat + perpLat * 0.7, midLon + perpLon * 0.7))
        return points
    }

    private fun calculateScenicScore(routePoints: List<GeoPoint>, origin: GeoPoint, destination: GeoPoint): Double {
        val directDistance = DistanceCalculator.haversineDistance(origin.latitude, origin.longitude, destination.latitude, destination.longitude)
        var pathDistance = 0.0
        for (i in 0 until routePoints.size - 1) { pathDistance += DistanceCalculator.haversineDistance(routePoints[i].latitude, routePoints[i].longitude, routePoints[i + 1].latitude, routePoints[i + 1].longitude) }
        val detourRatio = if (directDistance > 0) pathDistance / directDistance else 1.0
        var turns = 0
        for (i in 1 until routePoints.size - 1) {
            val bearing1 = DistanceCalculator.calculateBearing(routePoints[i - 1].latitude, routePoints[i - 1].longitude, routePoints[i].latitude, routePoints[i].longitude)
            val bearing2 = DistanceCalculator.calculateBearing(routePoints[i].latitude, routePoints[i].longitude, routePoints[i + 1].latitude, routePoints[i + 1].longitude)
            if (abs(bearing1 - bearing2) > 30) turns++
        }
        return detourRatio * 100.0 + (turns * 5.0)
    }

    private fun analyzeSegment(p1: GeoPoint, p2: GeoPoint): SegmentInfo {
        val latDiff = abs(p2.latitude - p1.latitude); val lonDiff = abs(p2.longitude - p1.longitude)
        val avgLat = (p1.latitude + p2.latitude) / 2.0; val avgLon = (p1.longitude + p2.longitude) / 2.0
        val hasToll = (avgLat * 1000).toInt() % 3 == 0 && latDiff > 0.01
        val hasHighway = (avgLon * 1000).toInt() % 5 == 0 && lonDiff > 0.02
        val hasFerry = (avgLat * 1000).toInt() % 7 == 0 && latDiff < 0.005 && lonDiff < 0.005
        return SegmentInfo(hasToll, hasHighway, hasFerry)
    }

    private fun checkRouteFeature(points: List<GeoPoint>, feature: String): Boolean {
        for (i in 0 until points.size - 1) { val info = analyzeSegment(points[i], points[i + 1]); return when (feature) { "toll" -> info.hasToll; "highway" -> info.hasHighway; "ferry" -> info.hasFerry; else -> false } }
        return false
    }

    private data class SegmentInfo(val hasToll: Boolean, val hasHighway: Boolean, val hasFerry: Boolean)
}
