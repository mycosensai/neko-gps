package com.nekogps.app.features.stats

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * OdometerEntity - stores lifetime mileage tracking data per date.
 */
@Entity(tableName = "odometer_data")
data class OdometerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val totalDistanceMeters: Float = 0f,
    val dailyDistanceMeters: Float = 0f,
    val weeklyDistanceMeters: Float = 0f,
    val monthlyDistanceMeters: Float = 0f,
    val yearlyDistanceMeters: Float = 0f
)

/**
 * OdometerDao - Room DAO for odometer data.
 */
@Dao
interface OdometerDao {
    @Query("SELECT * FROM odometer_data ORDER BY date DESC LIMIT 365")
    fun getAll(): Flow<List<OdometerEntity>>

    @Query("SELECT * FROM odometer_data WHERE date = :date")
    suspend fun getByDate(date: Long): OdometerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OdometerEntity)

    @Query("SELECT SUM(dailyDistanceMeters) FROM odometer_data WHERE date >= :startDate")
    suspend fun getDailyTotal(startDate: Long): Float

    @Query("SELECT SUM(weeklyDistanceMeters) FROM odometer_data WHERE date >= :startDate")
    suspend fun getWeeklyTotal(startDate: Long): Float

    @Query("SELECT SUM(monthlyDistanceMeters) FROM odometer_data WHERE date >= :startDate")
    suspend fun getMonthlyTotal(startDate: Long): Float

    @Query("SELECT SUM(yearlyDistanceMeters) FROM odometer_data WHERE date >= :startDate")
    suspend fun getYearlyTotal(startDate: Long): Float

    @Query("SELECT * FROM odometer_data ORDER BY date DESC LIMIT 30")
    suspend fun getRecentTrips(): List<OdometerEntity>

    @Query("SELECT SUM(dailyDistanceMeters) FROM odometer_data")
    suspend fun getLifetimeTotal(): Float

    @Query("DELETE FROM odometer_data")
    suspend fun clearAll()
}
