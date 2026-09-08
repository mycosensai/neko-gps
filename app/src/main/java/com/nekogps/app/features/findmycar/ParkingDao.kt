package com.nekogps.app.features.findmycar

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nekogps.app.features.bookmarks.ParkingLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkingDao {
    @Query("SELECT * FROM parking_locations ORDER BY parkedAt DESC LIMIT 1")
    fun getLatestParking(): Flow<ParkingLocation?>

    @Query("SELECT * FROM parking_locations ORDER BY parkedAt DESC")
    fun getAllParking(): Flow<List<ParkingLocation>>

    @Query("SELECT * FROM parking_locations WHERE id = :id")
    suspend fun getById(id: Long): ParkingLocation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(parking: ParkingLocation): Long

    @Delete
    suspend fun delete(parking: ParkingLocation)

    @Query("DELETE FROM parking_locations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM parking_locations")
    suspend fun deleteAll()
}
