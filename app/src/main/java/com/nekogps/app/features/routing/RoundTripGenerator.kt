package com.nekogps.app.features.routing

import android.content.Context
import android.util.Log
import com.nekogps.app.utils.DistanceCalculator
import org.osmdroid.util.GeoPoint

class RoundTripGenerator(private val context: Context) {
    companion object {
        private const val TAG = "RoundTripGenerator"
        private const val DEFAULT_AVG_SPEED_KMH = 50.0
        private const val SHORTER_RETURN_RATIO = 0.4
        private const val SCENIC_OFFSET_DEGREES = 0.01
    }

    data class RoundTripResult(
        val outboundRoute: List<GeoPoint>, val returnRoute: List<GeoPoint>,
        val outboundWaypoints: List<GeoPoint>, val returnWaypoint: GeoPoint?,
        val origin: GeoPoint, val destination: GeoPoint,
        val outboundDistanceMeters: Double, val returnDistanceMeters: Double,
        val totalDistanceMeters: Double, val outboundEtaMinutes: Double,
        val returnEtaMinutes: Double, val totalEtaMinutes: Double,
        val returnWaypointName: String
    ) {
        fun getSummary(): String {
            return buildString {
                appendLine("Round Trip Summary")
                appendLine("Outbound: ${DistanceCalculator.formatDistance(outboundDistanceMeters)}")
                appendLine("Return: ${DistanceCalculator.formatDistance(returnDistanceMeters)}")
                appendLine("Total: ${DistanceCalculator.formatDistance(totalDistanceMeters)}")
                appendLine("Outbound ETA: ${DistanceCalculator.formatETA(outboundEtaMinutes)}")
                appendLine("Return ETA: ${DistanceCalculator.formatETA(returnEtaMinutes)}")
                appendLine("Total ETA: ${DistanceCalculator.formatETA(totalEtaMinutes)}")
                if (returnWaypoint != null) { appendLine("Return waypoint: $returnWaypointName") }
            }
        }
    }

    data class RoundTripConfig(
        val origin: GeoPoint, val destination: GeoPoint, val waypoints: List<GeoPoint> = emptyList(),
        val returnWaypointLat: Double? = null, val returnWaypointLon: Double? = null,
        val returnWaypointName: String = "Return Waypoint", val optimizeReturn: Boolean = true,
        val avoidTolls: Boolean = false, val avoidHighways: Boolean = false, val avoidFerries: Boolean = false
    )

    suspend fun generateRoundTrip(
        config: RoundTripConfig,
        routeOptionsManager: RouteOptionsManager? = null,
        alternativeRoutes: AlternativeRoutes? = null
    ): RoundTripResult {
        Log.d(TAG, "Generating round trip from ${config.origin} to ${config.destination}")
        val outboundRoute = calculateOutboundRoute(config)
        val returnRoute = if (config.optimizeReturn && alternativeRoutes != null) {
            calculateOptimizedReturnRoute(config, alternativeRoutes, routeOptionsManager)
        } else calculateDirectReturnRoute(config)
        val returnWaypoint = if (config.returnWaypointLat != null && config.returnWaypointLon != null) {
            GeoPoint(config.returnWaypointLat!!, config.returnWaypointLon!!)
        } else null
        val outboundDistance = calculateRouteDistance(outboundRoute)
        val returnDistance = calculateRouteDistance(returnRoute)
        val totalDistance = outboundDistance + returnDistance
        val outboundEta = DistanceCalculator.estimateETA(outboundDistance, DEFAULT_AVG_SPEED_KMH)
        val returnEta = DistanceCalculator.estimateETA(returnDistance, DEFAULT_AVG_SPEED_KMH)
        val totalEta = outboundEta + returnEta
        Log.d(TAG, "Round trip generated: total distance ${DistanceCalculator.formatDistance(totalDistance)}")
        return RoundTripResult(
            outboundRoute = outboundRoute,
            returnRoute = returnRoute,
            outboundWaypoints = config.waypoints,
            returnWaypoint = returnWaypoint,
            origin = config.origin,
            destination = config.destination,
            outboundDistanceMeters = outboundDistance,
            returnDistanceMeters = returnDistance,
            totalDistanceMeters = totalDistance,
            outboundEtaMinutes = outboundEta,
            returnEtaMinutes = returnEta,
            totalEtaMinutes = totalEta,
            returnWaypointName = config.returnWaypointName
        )
    }

    private fun calculateOutboundRoute(config: RoundTripConfig): List<GeoPoint> {
        val route = mutableListOf<GeoPoint>()
        route.add(config.origin); route.addAll(config.waypoints); route.add(config.destination)
        return route
    }

    private suspend fun calculateOptimizedReturnRoute(
        config: RoundTripConfig,
        alternativeRoutes: AlternativeRoutes,
        routeOptionsManager: RouteOptionsManager? = null
    ): List<GeoPoint> {
        val useOptionsManager = config.avoidTolls || config.avoidHighways || config.avoidFerries
        val optionsManager = if (useOptionsManager) {
            routeOptionsManager ?: RouteOptionsManager(context)
        } else {
            null
        }
        val alternatives = alternativeRoutes.calculateAlternatives(
            origin = config.destination,
            destination = config.origin,
            waypoints = config.waypoints.reversed(),
            routeOptionsManager = optionsManager
        )
        val bestRoute = alternatives.routes.minByOrNull { it.distanceMeters }
        return bestRoute?.routePoints ?: calculateDirectReturnRoute(config)
    }

    private fun calculateDirectReturnRoute(config: RoundTripConfig): List<GeoPoint> {
        val route = mutableListOf<GeoPoint>()
        val returnWaypoints = config.waypoints.reversed()
        route.add(config.destination)
        if (config.returnWaypointLat != null && config.returnWaypointLon != null) {
            val midPoint = GeoPoint(config.returnWaypointLat!!, config.returnWaypointLon!!)
            val midIndex = returnWaypoints.size / 2
            route.addAll(returnWaypoints.take(midIndex))
            route.add(midPoint)
            route.addAll(returnWaypoints.drop(midIndex))
        } else { route.addAll(returnWaypoints) }
        route.add(config.origin)
        return route
    }

    private fun calculateRouteDistance(points: List<GeoPoint>): Double {
        if (points.size < 2) return 0.0
        var totalDistance = 0.0
        for (i in 0 until points.size - 1) {
            totalDistance += DistanceCalculator.haversineDistance(
                points[i].latitude,
                points[i].longitude,
                points[i + 1].latitude,
                points[i + 1].longitude
            )
        }
        return totalDistance
    }

    fun calculateMidwayWaypoint(
        origin: GeoPoint,
        destination: GeoPoint,
        strategy: MidwayStrategy = MidwayStrategy.MIDPOINT
    ): GeoPoint {
        return when (strategy) {
            MidwayStrategy.MIDPOINT -> GeoPoint(
                (origin.latitude + destination.latitude) / 2.0,
                (origin.longitude + destination.longitude) / 2.0
            )
            MidwayStrategy.SHORTER_RETURN -> GeoPoint(
                destination.latitude +
                    (origin.latitude - destination.latitude) * SHORTER_RETURN_RATIO,
                destination.longitude +
                    (origin.longitude - destination.longitude) * SHORTER_RETURN_RATIO
            )
            MidwayStrategy.SCENIC -> {
                val midLat = (origin.latitude + destination.latitude) / 2.0
                val midLon = (origin.longitude + destination.longitude) / 2.0
                GeoPoint(midLat + SCENIC_OFFSET_DEGREES, midLon + SCENIC_OFFSET_DEGREES)
            }
        }
    }

    enum class MidwayStrategy(val label: String) {
        MIDPOINT("Midpoint"),
        SHORTER_RETURN("Shorter Return"),
        SCENIC("Scenic")
    }

    fun formatRoundTripResult(result: RoundTripResult): String {
        return buildString {
            appendLine("═══════════════════════")
            appendLine("   ROUND TRIP REPORT")
            appendLine("═══════════════════════")
            appendLine()
            appendLine("From: ${result.origin.latitude}, ${result.origin.longitude}")
            appendLine("To: ${result.destination.latitude}, ${result.destination.longitude}")
            appendLine()
            appendLine("OUTBOUND LEG:")
            appendLine("  Distance: ${DistanceCalculator.formatDistance(result.outboundDistanceMeters)}")
            appendLine("  ETA: ${DistanceCalculator.formatETA(result.outboundEtaMinutes)}")
            appendLine()
            appendLine("RETURN LEG:")
            appendLine("  Distance: ${DistanceCalculator.formatDistance(result.returnDistanceMeters)}")
            appendLine("  ETA: ${DistanceCalculator.formatETA(result.returnEtaMinutes)}")
            if (result.returnWaypoint != null) { appendLine("  Waypoint: ${result.returnWaypointName}") }
            appendLine()
            appendLine("TOTALS:")
            appendLine("  Total Distance: ${DistanceCalculator.formatDistance(result.totalDistanceMeters)}")
            appendLine("  Total ETA: ${DistanceCalculator.formatETA(result.totalEtaMinutes)}")
            appendLine("═══════════════════════")
        }
    }
}
