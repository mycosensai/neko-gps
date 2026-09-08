package com.nekogps.app.features.waypoints

import android.content.Context
import android.location.Location
import com.nekogps.app.features.bookmarks.AppDatabase
import com.nekogps.app.features.bookmarks.WaypointEntity
import kotlinx.coroutines.flow.Flow

/**
 * WaypointManager - allows users to add multiple waypoints to a route.
 * Supports reordering via drag-and-drop, calculates total route distance and ETA.
 */
class WaypointManager(context: Context) {
    private val dao = AppDatabase.getInstance(context).waypointDao()

    fun getWaypointsForRoute(routeId: Long): Flow<List<WaypointEntity>> {
        return dao.getWaypointsForRoute(routeId)
    }

    suspend fun getWaypointsForRouteSync(routeId: Long): List<WaypointEntity> {
        return dao.getWaypointsForRouteSync(routeId)
    }

    suspend fun addWaypoint(
        routeId: Long,
        name: String,
        description: String = "",
        latitude: Double,
        longitude: Double
    ): Long {
        val currentMax = dao.getMaxOrder(routeId) ?: -1
        val waypoint = WaypointEntity(
            routeId = routeId,
            name = name,
            description = description,
            latitude = latitude,
            longitude = longitude,
            order = currentMax + 1
        )
        return dao.insert(waypoint)
    }

    suspend fun updateWaypoint(waypoint: WaypointEntity) {
        dao.update(waypoint)
    }

    suspend fun deleteWaypoint(waypoint: WaypointEntity) {
        dao.delete(waypoint)
    }

    suspend fun deleteWaypointById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clearRoute(routeId: Long) {
        dao.deleteRoute(routeId)
    }

    suspend fun getWaypointCount(routeId: Long): Int {
        return dao.getWaypointCount(routeId)
    }

    /**
     * Reorder waypoints by moving an item from one position to another.
     */
    suspend fun reorderWaypoints(routeId: Long, fromPosition: Int, toPosition: Int) {
        val waypoints = dao.getWaypointsForRouteSync(routeId).toMutableList()
        if (fromPosition < 0 || fromPosition >= waypoints.size) return
        if (toPosition < 0 || toPosition >= waypoints.size) return

        val movedItem = waypoints.removeAt(fromPosition)
        waypoints.add(toPosition, movedItem)

        // Update order for all waypoints
        waypoints.forEachIndexed { index, waypoint: WaypointEntity ->
            dao.update(waypoint.copy(order = index))
        }
    }

    /**
     * Calculate total route distance in meters.
     */
    fun calculateTotalDistance(waypoints: List<WaypointEntity>): Float {
        if (waypoints.size < 2) return 0f

        var totalDistance = 0f
        val results = FloatArray(1)

        for (i in 0 until waypoints.size - 1) {
            Location.distanceBetween(
                waypoints[i].latitude, waypoints[i].longitude,
                waypoints[i + 1].latitude, waypoints[i + 1].longitude,
                results
            )
            totalDistance += results[0]
        }

        return totalDistance
    }

    /**
     * Calculate total route distance including start and end points.
     */
    fun calculateTotalDistanceWithEndpoints(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        waypoints: List<WaypointEntity>
    ): Float {
        if (waypoints.isEmpty()) {
            val results = FloatArray(1)
            Location.distanceBetween(startLat, startLon, endLat, endLon, results)
            return results[0]
        }

        var totalDistance = 0f
        val results = FloatArray(1)

        // Start to first waypoint
        Location.distanceBetween(
            startLat, startLon,
            waypoints.first().latitude, waypoints.first().longitude,
            results
        )
        totalDistance += results[0]

        // Between waypoints
        totalDistance += calculateTotalDistance(waypoints)

        // Last waypoint to end
        Location.distanceBetween(
            waypoints.last().latitude, waypoints.last().longitude,
            endLat, endLon,
            results
        )
        totalDistance += results[0]

        return totalDistance
    }

    /**
     * Estimate ETA in minutes based on distance and average speed.
     * @param distanceMeters total distance in meters
     * @param averageSpeedKmh average speed in km/h (default 50 km/h for driving)
     */
    fun estimateETA(distanceMeters: Float, averageSpeedKmh: Float = 50f): Int {
        val distanceKm = distanceMeters / 1000f
        val timeHours = distanceKm / averageSpeedKmh
        return (timeHours * 60).toInt().coerceAtLeast(1)
    }

    /**
     * Format distance for display.
     */
    fun formatDistance(distanceMeters: Float): String {
        return if (distanceMeters >= 1000) {
            String.format("%.1f km", distanceMeters / 1000)
        } else {
            String.format("%.0f m", distanceMeters)
        }
    }

    /**
     * Format ETA for display.
     */
    fun formatETA(etaMinutes: Int): String {
        return when {
            etaMinutes >= 60 -> "${etaMinutes / 60}h ${etaMinutes % 60}m"
            etaMinutes > 0 -> "${etaMinutes} min"
            else -> "< 1 min"
        }
    }

    /**
     * Get route summary with total distance and ETA.
     */
    fun getRouteSummary(
        waypoints: List<WaypointEntity>,
        averageSpeedKmh: Float = 50f
    ): RouteSummary {
        val totalDistance = calculateTotalDistance(waypoints)
        val eta = estimateETA(totalDistance, averageSpeedKmh)
        return RouteSummary(
            totalDistanceMeters = totalDistance,
            estimatedTimeMinutes = eta,
            waypointCount = waypoints.size
        )
    }
}

data class RouteSummary(
    val totalDistanceMeters: Float,
    val estimatedTimeMinutes: Int,
    val waypointCount: Int
)
