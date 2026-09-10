package com.nekogps.app.features.findmycar

import android.content.Context
import com.nekogps.app.features.bookmarks.AppDatabase
import com.nekogps.app.features.findmycar.ParkingLocation
import kotlinx.coroutines.flow.Flow

/**
 * FindMyCar - saves parking location with timestamp, shows walking direction back to car.
 * Includes parking time elapsed and optional photo of parking spot.
 */
class FindMyCar(context: Context) {
    private val dao = AppDatabase.getInstance(context).parkingDao()

    companion object {
        private const val MILLIS_PER_MINUTE = 60000L
        private const val MINUTES_PER_HOUR = 60L
        private const val HOURS_PER_DAY = 24L
        private const val WALKING_SPEED_METERS_PER_SECOND = 1.39f
        private const val SECONDS_PER_MINUTE = 60
    }

    val latestParking: Flow<ParkingLocation?> = dao.getLatestParking()
    val allParking: Flow<List<ParkingLocation>> = dao.getAllParking()

    /**
     * Input for saving a parking location, grouping the fields of [saveParkingLocation].
     */
    data class ParkingDetails(
        val latitude: Double,
        val longitude: Double,
        val address: String = "",
        val note: String = "",
        val photoPath: String = "",
        val level: String = "",
        val spotNumber: String = ""
    )

    suspend fun saveParkingLocation(details: ParkingDetails): Long {
        // Clear previous parking before saving new one
        dao.deleteAll()
        val parking = ParkingLocation(
            latitude = details.latitude,
            longitude = details.longitude,
            address = details.address,
            note = details.note,
            photoPath = details.photoPath,
            level = details.level,
            spotNumber = details.spotNumber,
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
        val minutes = elapsed / MILLIS_PER_MINUTE
        val hours = minutes / MINUTES_PER_HOUR
        val days = hours / HOURS_PER_DAY

        return when {
            days > 0 -> "${days}d ${hours % HOURS_PER_DAY}h ago"
            hours > 0 -> "${hours}h ${minutes % MINUTES_PER_HOUR}m ago"
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
        return (distanceMeters / WALKING_SPEED_METERS_PER_SECOND / SECONDS_PER_MINUTE)
            .toInt().coerceAtLeast(1)
    }
}
