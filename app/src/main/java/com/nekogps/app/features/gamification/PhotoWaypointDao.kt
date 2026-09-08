package com.nekogps.app.features.gamification

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoWaypointDao {
    @Query("SELECT * FROM photo_waypoints ORDER BY takenAt DESC")
    fun getAllPhotos(): Flow<List<PhotoWaypointEntity>>

    @Query("SELECT * FROM photo_waypoints WHERE waypointId = :waypointId")
    suspend fun getByWaypointId(waypointId: Long): List<PhotoWaypointEntity>

    @Query("SELECT * FROM photo_waypoints WHERE bookmarkId = :bookmarkId")
    suspend fun getByBookmarkId(bookmarkId: Long): List<PhotoWaypointEntity>

    @Query("SELECT * FROM photo_waypoints WHERE id = :id")
    suspend fun getById(id: Long): PhotoWaypointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: PhotoWaypointEntity): Long

    @Delete
    suspend fun delete(photo: PhotoWaypointEntity)

    @Query("DELETE FROM photo_waypoints WHERE waypointId = :waypointId")
    suspend fun deleteByWaypointId(waypointId: Long)

    @Query("DELETE FROM photo_waypoints WHERE bookmarkId = :bookmarkId")
    suspend fun deleteByBookmarkId(bookmarkId: Long)

    @Query("DELETE FROM photo_waypoints")
    suspend fun deleteAll()
}
