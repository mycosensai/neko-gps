package com.nekogps.app.features.gamification

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photo_waypoints")
data class PhotoWaypointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val waypointId: Long = 0,
    val bookmarkId: Long = 0,
    val photoPath: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val caption: String = "",
    val takenAt: Long = System.currentTimeMillis()
)
