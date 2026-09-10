package com.nekogps.app.features.waypoints

import android.content.Context
import android.location.Location
import com.nekogps.app.features.bookmarks.AppDatabase
import com.nekogps.app.features.waypoints.WaypointEntity
import kotlinx.coroutines.flow.Flow

/**
 * WaypointManager - allows users to add multiple waypoints to a route.
 * Supports reordering via drag-and-drop, calculates total route distance and ETA.
 */
class WaypointManager(context: Context) : WaypointRouteCalculator() {
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
}

data class RouteSummary(
    val totalDistanceMeters: Float,
    val estimatedTimeMinutes: Int,
    val waypointCount: Int
)
