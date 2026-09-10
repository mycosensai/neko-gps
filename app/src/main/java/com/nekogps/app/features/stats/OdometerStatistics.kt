package com.nekogps.app.features.stats

import android.content.Context
import com.nekogps.app.features.bookmarks.AppDatabase

/**
 * OdometerStatistics - computes derived odometer statistics (period totals,
 * estimated ride time and trip counts) on top of the stored daily records.
 */
class OdometerStatistics(private val context: Context) {
    private val dao = AppDatabase.getInstance(context).odometerDao()

    companion object {
        private const val METERS_PER_KILOMETER = 1000f
        private const val SECONDS_PER_HOUR = 3600
        private const val DAILY_ESTIMATED_HOURS = 2f
        private const val WEEKLY_ESTIMATED_HOURS = 10f
        private const val MONTHLY_ESTIMATED_HOURS = 40f
        private const val YEARLY_ESTIMATED_HOURS = 240f
    }

    suspend fun getPeriodStats(
        startDate: Long,
        period: OdometerManager.Period
    ): OdometerManager.StatsData {
        val totalDistance = when (period) {
            OdometerManager.Period.DAILY -> dao.getDailyTotal(startDate)
            OdometerManager.Period.WEEKLY -> dao.getWeeklyTotal(startDate)
            OdometerManager.Period.MONTHLY -> dao.getMonthlyTotal(startDate)
            OdometerManager.Period.YEARLY -> dao.getYearlyTotal(startDate)
        }
        // Estimate time: daily ~2h, weekly ~10h, monthly ~40h, yearly ~240h
        val estimatedHours = when (period) {
            OdometerManager.Period.DAILY -> DAILY_ESTIMATED_HOURS
            OdometerManager.Period.WEEKLY -> WEEKLY_ESTIMATED_HOURS
            OdometerManager.Period.MONTHLY -> MONTHLY_ESTIMATED_HOURS
            OdometerManager.Period.YEARLY -> YEARLY_ESTIMATED_HOURS
        }
        val totalTimeSeconds = (estimatedHours * SECONDS_PER_HOUR).toLong()
        val averageSpeedKmh = if (totalTimeSeconds > 0) {
            (totalDistance / METERS_PER_KILOMETER) / estimatedHours
        } else {
            0f
        }
        return OdometerManager.StatsData(
            period = period,
            totalDistanceMeters = totalDistance,
            averageSpeedKmh = averageSpeedKmh,
            totalTimeSeconds = totalTimeSeconds
        )
    }

    suspend fun getTripCount(): Long = dao.getRecentTrips().size.toLong()
}
