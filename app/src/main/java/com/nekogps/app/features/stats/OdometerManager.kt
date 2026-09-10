package com.nekogps.app.features.stats

import android.content.Context
import com.nekogps.app.features.bookmarks.AppDatabase
import kotlinx.coroutines.flow.Flow

/**
 * OdometerManager - tracks lifetime distance using Room database.
 */
class OdometerManager(private val context: Context) {
    private val dao = AppDatabase.getInstance(context).odometerDao()

    val allRecords: Flow<List<OdometerEntity>> = dao.getAll()

    suspend fun getByDate(date: Long): OdometerEntity? = dao.getByDate(date)
    suspend fun saveDailyRecord(entity: OdometerEntity) = dao.insert(entity)
    suspend fun getLifetimeTotal(): Float = dao.getLifetimeTotal()
    suspend fun getDailyTotal(startDate: Long): Float = dao.getDailyTotal(startDate)
    suspend fun getWeeklyTotal(startDate: Long): Float = dao.getWeeklyTotal(startDate)
    suspend fun getMonthlyTotal(startDate: Long): Float = dao.getMonthlyTotal(startDate)
    suspend fun getYearlyTotal(startDate: Long): Float = dao.getYearlyTotal(startDate)
    suspend fun getRecentTrips(): List<OdometerEntity> = dao.getRecentTrips()

    companion object {
        private const val METERS_PER_KILOMETER = 1000
        private const val SECONDS_PER_HOUR = 3600
        private const val SECONDS_PER_MINUTE = 60

        fun formatDistance(meters: Float): String {
            return if (meters >= METERS_PER_KILOMETER) {
                "%.1f km".format(meters / METERS_PER_KILOMETER)
            } else {
                "%.0f m".format(meters)
            }
        }
    }

    suspend fun clearAll() = dao.clearAll()

    enum class Period(val label: String) {
        DAILY("Day"),
        WEEKLY("Week"),
        MONTHLY("Month"),
        YEARLY("Year")
    }

    data class StatsData(
        val period: Period,
        val totalDistanceMeters: Float,
        val averageSpeedKmh: Float,
        val totalTimeSeconds: Long
    ) {
        val formattedDistance: String get() = formatDistance(totalDistanceMeters)
        val formattedTime: String get() {
            val hours = totalTimeSeconds / SECONDS_PER_HOUR
            val mins = (totalTimeSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
            return "${hours}h ${mins}m"
        }
    }
}
