package com.nekogps.app.features.findmycar

import android.content.Context
import com.nekogps.app.features.bookmarks.AppDatabase
import com.nekogps.app.features.bookmarks.ParkingLocation
import kotlinx.coroutines.flow.Flow

/**
 * FindMyCar - saves parking location with timestamp, shows walking direction back to car.
 * Includes parking time elapsed and optional photo of parking spot.
 */
class FindMyCar(context: Context) {
    private val dao = AppDatabase.getInstance(context).parkingDao()

    val latestParking: Flow<ParkingLocation?> = dao.getLatestParking()
    val allParking: Flow<List<ParkingLocation>> = dao.getAllParking()

    suspend fun saveParkingLocation(
        latitude: Double,
        longitude: Double,
        address: String = "",
        note: String = "",
        photoPath: String = "",
        level: String = "",
        spotNumber: String = ""
    ): Long {
        // Clear previous parking before saving new one
        dao.deleteAll()
        val parking = ParkingLocation(
            latitude = latitude,
            longitude = longitude,
            address = address,
            note = note,
            photoPath = photoPath,
            level = level,
            spotNumber = spotNumber,
            parkedAt = System.currentTimeMillis()
        )
        return dao.insert(parking)
    }

    suspend fun getParkingById(id: Long): ParkingLocation? {
        return dao.getById(id)
    }

    suspend fun deleteParking(parking: ParkingLocation) {
        dao.delete(parking)
    }

    suspend fun clearParking() {
        dao.deleteAll()
    }

    fun getParkingTimeElapsed(parkedAt: Long): String {
        val elapsed = System.currentTimeMillis() - parkedAt
        val minutes = elapsed / 60000
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "${days}d ${hours % 24}h ago"
            hours > 0 -> "${hours}h ${minutes % 60}m ago"
            minutes > 0 -> "${minutes}m ago"
            else -> "Just now"
        }
    }

    fun getWalkingDistance(userLat: Double, userLon: Double, parking: ParkingLocation): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            userLat, userLon,
            parking.latitude, parking.longitude,
            results
        )
        return results[0]
    }

    fun getWalkingTimeMinutes(distanceMeters: Float): Int {
        // Average walking speed ~5 km/h = 1.39 m/s
        return (distanceMeters / 1.39f / 60).toInt().coerceAtLeast(1)
    }
}
