package com.nekogps.app.features.waypoints

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "waypoints")
data class WaypointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routeId: Long = 0,
    val name: String,
    val description: String = "",
    val latitude: Double,
    val longitude: Double,
    val order: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
