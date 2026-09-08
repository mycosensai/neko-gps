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
        fun formatDistance(meters: Float): String {
            return if (meters >= 1000) "%.1f km".format(meters / 1000) else "%.0f m".format(meters)
        }
    }

    suspend fun clearAll() = dao.clearAll()

    suspend fun getPeriodStats(startDate: Long, period: Period): StatsData {
        val totalDistance = when (period) {
            Period.DAILY -> dao.getDailyTotal(startDate)
            Period.WEEKLY -> dao.getWeeklyTotal(startDate)
            Period.MONTHLY -> dao.getMonthlyTotal(startDate)
            Period.YEARLY -> dao.getYearlyTotal(startDate)
        }
        // Estimate time: daily ~2h, weekly ~10h, monthly ~40h, yearly ~240h
        val estimatedHours = when (period) {
            Period.DAILY -> 2f
            Period.WEEKLY -> 10f
            Period.MONTHLY -> 40f
            Period.YEARLY -> 240f
        }
        val totalTimeSeconds = (estimatedHours * 3600).toLong()
        val averageSpeedKmh = if (totalTimeSeconds > 0) (totalDistance / 1000f) / estimatedHours else 0f
        return StatsData(
            period = period,
            totalDistanceMeters = totalDistance,
            averageSpeedKmh = averageSpeedKmh,
            totalTimeSeconds = totalTimeSeconds
        )
    }

    suspend fun getTripCount(): Long = dao.getRecentTrips().size.toLong()

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
            val hours = totalTimeSeconds / 3600
            val mins = (totalTimeSeconds % 3600) / 60
            return "${hours}h ${mins}m"
        }
    }
}
