package com.nekogps.app.features.findmycar

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parking_locations")
data class ParkingLocation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val note: String = "",
    val photoPath: String = "",
    val parkedAt: Long = System.currentTimeMillis(),
    val level: String = "",
    val spotNumber: String = ""
)
