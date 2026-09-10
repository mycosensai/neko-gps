package com.nekogps.app.features.routing

import android.util.Log
import com.nekogps.app.utils.DistanceCalculator
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.math.sqrt

class AlternativeRoutes {
    companion object {
        private const val TAG = "AlternativeRoutes"
        private const val DEFAULT_AVG_SPEED_KMH = 50.0
        private const val FASTEST_ROUTE_OFFSET = 0.3
        private const val SCENIC_ROUTE_OFFSET = 0.8
        private const val FIRST_INTERMEDIATE_FRACTION = 0.3
        private const val SECOND_INTERMEDIATE_FRACTION = 0.7
        private const val TURN_ANGLE_THRESHOLD_DEGREES = 30
        private const val SCENIC_DETOUR_SCALE = 100.0
        private const val SCENIC_TURN_WEIGHT = 5.0
        private const val COORDINATE_GRID_SCALE = 1000
        private const val TOLL_GRID_DIVISOR = 3
        private const val HIGHWAY_GRID_DIVISOR = 5
        private const val FERRY_GRID_DIVISOR = 7
        private const val TOLL_LAT_THRESHOLD = 0.01
        private const val HIGHWAY_LON_THRESHOLD = 0.02
        private const val FERRY_SPAN_THRESHOLD = 0.005
    }

    data class AlternativeRoute(
        val id: Int, val name: String, val description: String, val routePoints: List<GeoPoint>,
        val distanceMeters: Double, val etaMinutes: Double, val preference: RoutePreference,
        val score: Double, val hasTolls: Boolean, val hasHighways: Boolean, val hasFerries: Boolean
    ) {
        enum class RoutePreference(val label: String, val icon: String) {
            SHORTEST("Shortest", "📏"),
            FASTEST("Fastest", "⚡"),
            SCENIC("Scenic", "🌿")
        }
    }

    data class AlternativeRoutesResult(
        val routes: List<AlternativeRoute>,
        val origin: GeoPoint,
        val destination: GeoPoint,
        val totalCalculated: Int
    )

    data class RouteCalculationRequest(
        val id: Int,
        val origin: GeoPoint,
        val destination: GeoPoint,
        val waypoints: List<GeoPoint>,
        val offsetFactor: Double,
        val preference: AlternativeRoute.RoutePreference,
        val routeOptionsManager: RouteOptionsManager?
    )

    suspend fun calculateAlternatives(
        origin: GeoPoint,
        destination: GeoPoint,
        waypoints: List<GeoPoint> = emptyList(),
        routeOptionsManager: RouteOptionsManager? = null
    ): AlternativeRoutesResult {
        Log.d(TAG, "Calculating alternatives from $origin to $destination")
        val alternatives = mutableListOf<AlternativeRoute>()
        alternatives.add(
            calculateRoute(
                RouteCalculationRequest(
                    id = 0,
                    origin = origin,
                    destination = destination,
                    waypoints = waypoints,
                    offsetFactor = 0.0,
                    preference = AlternativeRoute.RoutePreference.SHORTEST,
                    routeOptionsManager = routeOptionsManager
                )
            )
        )
        alternatives.add(
            calculateRoute(
                RouteCalculationRequest(
                    id = 1,
                    origin = origin,
                    destination = destination,
                    waypoints = waypoints,
                    offsetFactor = FASTEST_ROUTE_OFFSET,
                    preference = AlternativeRoute.RoutePreference.FASTEST,
                    routeOptionsManager = routeOptionsManager
                )
            )
        )
        alternatives.add(
            calculateRoute(
                RouteCalculationRequest(
                    id = 2,
                    origin = origin,
                    destination = destination,
                    waypoints = waypoints,
                    offsetFactor = SCENIC_ROUTE_OFFSET,
                    preference = AlternativeRoute.RoutePreference.SCENIC,
                    routeOptionsManager = routeOptionsManager
                )
            )
        )
        val filteredAlternatives = alternatives.filter { route ->
            routeOptionsManager?.let { manager ->
                val prefs = runBlocking { manager.getPreferences() }
                !(prefs.avoidFerries && route.hasFerries)
            } ?: true
        }
        val sortedRoutes = filteredAlternatives.sortedBy { it.score }
        Log.d(TAG, "Calculated ${sortedRoutes.size} alternative routes")
        return AlternativeRoutesResult(
            routes = sortedRoutes,
            origin = origin,
            destination = destination,
            totalCalculated = sortedRoutes.size
        )
    }

    private suspend fun calculateRoute(
        request: RouteCalculationRequest
    ): AlternativeRoute {
        val routePoints = buildRoutePoints(request)
        val dedupedPoints = routePoints.distinctBy { "${it.latitude},${it.longitude}" }
        val totalDistance = computeTotalDistance(dedupedPoints, request.routeOptionsManager)
        val etaMinutes = DistanceCalculator.estimateETA(totalDistance, DEFAULT_AVG_SPEED_KMH)
        val scenicScore = calculateScenicScore(dedupedPoints, request.origin, request.destination)
        return assembleRoute(request, dedupedPoints, totalDistance, etaMinutes, scenicScore)
    }

    private fun buildRoutePoints(request: RouteCalculationRequest): List<GeoPoint> {
        val intermediatePoints = generateIntermediatePoints(
            request.origin,
            request.destination,
            request.offsetFactor
        )
        return mutableListOf<GeoPoint>().apply {
            add(request.origin)
            if (request.waypoints.isNotEmpty()) {
                val splitIndex = intermediatePoints.size / 2
                addAll(intermediatePoints.take(splitIndex))
                addAll(request.waypoints)
                addAll(intermediatePoints.drop(splitIndex))
            } else {
                addAll(intermediatePoints)
            }
            add(request.destination)
        }
    }

    private suspend fun computeTotalDistance(
        points: List<GeoPoint>,
        routeOptionsManager: RouteOptionsManager?
    ): Double {
        var totalDistance = 0.0
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val directDist = DistanceCalculator.haversineDistance(
                p1.latitude,
                p1.longitude,
                p2.latitude,
                p2.longitude
            )
            val penalizedDist = if (routeOptionsManager != null) {
                val segmentInfo = analyzeSegment(p1, p2)
                runBlocking {
                    routeOptionsManager.calculatePenalizedDistance(
                        directDist,
                        segmentInfo.hasToll,
                        segmentInfo.hasHighway,
                        segmentInfo.hasFerry
                    )
                }
            } else {
                directDist
            }
            totalDistance += penalizedDist
        }
        return totalDistance
    }

    private fun assembleRoute(
        request: RouteCalculationRequest,
        routePoints: List<GeoPoint>,
        totalDistance: Double,
        etaMinutes: Double,
        scenicScore: Double
    ): AlternativeRoute {
        val preference = request.preference
        val score = when (preference) {
            AlternativeRoute.RoutePreference.SHORTEST -> totalDistance
            AlternativeRoute.RoutePreference.FASTEST -> etaMinutes
            AlternativeRoute.RoutePreference.SCENIC -> scenicScore
        }
        val routeName = when (preference) {
            AlternativeRoute.RoutePreference.SHORTEST -> "Shortest Route"
            AlternativeRoute.RoutePreference.FASTEST -> "Fastest Route"
            AlternativeRoute.RoutePreference.SCENIC -> "Scenic Route"
        }
        val routeDesc = when (preference) {
            AlternativeRoute.RoutePreference.SHORTEST -> "Most direct path with minimal detours"
            AlternativeRoute.RoutePreference.FASTEST -> "Optimized for speed with fewer waypoints"
            AlternativeRoute.RoutePreference.SCENIC -> "Longer route prioritizing scenic views"
        }
        return AlternativeRoute(
            id = request.id,
            name = routeName,
            description = routeDesc,
            routePoints = routePoints,
            distanceMeters = totalDistance,
            etaMinutes = etaMinutes,
            preference = preference,
            score = score,
            hasTolls = checkRouteFeature(routePoints, "toll"),
            hasHighways = checkRouteFeature(routePoints, "highway"),
            hasFerries = checkRouteFeature(routePoints, "ferry")
        )
    }

    private fun generateIntermediatePoints(
        origin: GeoPoint,
        destination: GeoPoint,
        offsetFactor: Double
    ): List<GeoPoint> {
        val points = mutableListOf<GeoPoint>()
        val midLat = (origin.latitude + destination.latitude) / 2.0
        val midLon = (origin.longitude + destination.longitude) / 2.0
        val latDiff = destination.latitude - origin.latitude
        val lonDiff = destination.longitude - origin.longitude
        val perpLat = -lonDiff * offsetFactor
        val perpLon = latDiff * offsetFactor
        val firstOffsetLat = midLat + perpLat * FIRST_INTERMEDIATE_FRACTION
        val firstOffsetLon = midLon + perpLon * FIRST_INTERMEDIATE_FRACTION
        points.add(GeoPoint(firstOffsetLat, firstOffsetLon))
        val secondOffsetLat = midLat + perpLat * SECOND_INTERMEDIATE_FRACTION
        val secondOffsetLon = midLon + perpLon * SECOND_INTERMEDIATE_FRACTION
        points.add(GeoPoint(secondOffsetLat, secondOffsetLon))
        return points
    }

    private fun calculateScenicScore(
        routePoints: List<GeoPoint>,
        origin: GeoPoint,
        destination: GeoPoint
    ): Double {
        val directDistance = DistanceCalculator.haversineDistance(
            origin.latitude,
            origin.longitude,
            destination.latitude,
            destination.longitude
        )
        var pathDistance = 0.0
        for (i in 0 until routePoints.size - 1) {
            pathDistance += DistanceCalculator.haversineDistance(
                routePoints[i].latitude,
                routePoints[i].longitude,
                routePoints[i + 1].latitude,
                routePoints[i + 1].longitude
            )
        }
        val detourRatio = if (directDistance > 0) pathDistance / directDistance else 1.0
        var turns = 0
        for (i in 1 until routePoints.size - 1) {
            val bearing1 = DistanceCalculator.calculateBearing(
                routePoints[i - 1].latitude,
                routePoints[i - 1].longitude,
                routePoints[i].latitude,
                routePoints[i].longitude
            )
            val bearing2 = DistanceCalculator.calculateBearing(
                routePoints[i].latitude,
                routePoints[i].longitude,
                routePoints[i + 1].latitude,
                routePoints[i + 1].longitude
            )
            if (abs(bearing1 - bearing2) > TURN_ANGLE_THRESHOLD_DEGREES) turns++
        }
        return detourRatio * SCENIC_DETOUR_SCALE + (turns * SCENIC_TURN_WEIGHT)
    }

    private fun analyzeSegment(p1: GeoPoint, p2: GeoPoint): SegmentInfo {
        val latDiff = abs(p2.latitude - p1.latitude); val lonDiff = abs(p2.longitude - p1.longitude)
        val avgLat = (p1.latitude + p2.latitude) / 2.0; val avgLon = (p1.longitude + p2.longitude) / 2.0
        val latGrid = (avgLat * COORDINATE_GRID_SCALE).toInt()
        val lonGrid = (avgLon * COORDINATE_GRID_SCALE).toInt()
        val hasToll = latGrid % TOLL_GRID_DIVISOR == 0 && latDiff > TOLL_LAT_THRESHOLD
        val hasHighway = lonGrid % HIGHWAY_GRID_DIVISOR == 0 && lonDiff > HIGHWAY_LON_THRESHOLD
        val hasFerry = latGrid % FERRY_GRID_DIVISOR == 0 &&
            latDiff < FERRY_SPAN_THRESHOLD &&
            lonDiff < FERRY_SPAN_THRESHOLD
        return SegmentInfo(hasToll, hasHighway, hasFerry)
    }

    private fun checkRouteFeature(points: List<GeoPoint>, feature: String): Boolean {
        for (i in 0 until points.size - 1) {
            val info = analyzeSegment(points[i], points[i + 1])
            return when (feature) {
                "toll" -> info.hasToll
                "highway" -> info.hasHighway
                "ferry" -> info.hasFerry
                else -> false
            }
        }
        return false
    }

    private data class SegmentInfo(val hasToll: Boolean, val hasHighway: Boolean, val hasFerry: Boolean)
}
