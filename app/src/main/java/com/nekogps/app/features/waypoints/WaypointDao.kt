package com.nekogps.app.features.waypoints

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nekogps.app.features.bookmarks.WaypointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaypointDao {
    @Query("SELECT * FROM waypoints WHERE routeId = :routeId ORDER BY `order` ASC")
    fun getWaypointsForRoute(routeId: Long): Flow<List<WaypointEntity>>

    @Query("SELECT * FROM waypoints WHERE routeId = :routeId ORDER BY `order` ASC")
    suspend fun getWaypointsForRouteSync(routeId: Long): List<WaypointEntity>

    @Query("SELECT * FROM waypoints WHERE id = :id")
    suspend fun getById(id: Long): WaypointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(waypoint: WaypointEntity): Long

    @Update
    suspend fun update(waypoint: WaypointEntity)

    @Delete
    suspend fun delete(waypoint: WaypointEntity)

    @Query("DELETE FROM waypoints WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM waypoints WHERE routeId = :routeId")
    suspend fun deleteRoute(routeId: Long)

    @Query("SELECT COUNT(*) FROM waypoints WHERE routeId = :routeId")
    suspend fun getWaypointCount(routeId: Long): Int

    @Query("SELECT MAX(`order`) FROM waypoints WHERE routeId = :routeId")
    suspend fun getMaxOrder(routeId: Long): Int?
}
