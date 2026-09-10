package com.nekogps.app.features.stats

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * StatisticsDashboard - aggregates trip statistics.
 */
class StatisticsDashboard(private val context: Context) {
    companion object {
        private const val MILLIS_PER_DAY = 86_400_000L
        private const val DAYS_PER_WEEK = 7
        private const val DAYS_PER_MONTH = 30
        private const val DAYS_PER_YEAR = 365L
        private const val DAILY_BAR_DAY_COUNT = 7
        private const val WEEKLY_BAR_BUCKET_COUNT = 4
        private const val MONTHLY_BAR_BUCKET_COUNT = 12
        private const val YEARLY_BAR_BUCKET_COUNT = 12
        private const val DEFAULT_BAR_BUCKET_COUNT = 12
        private const val DAILY_LINE_POINT_COUNT = 24
        private const val WEEKLY_LINE_POINT_COUNT = 7
        private const val MONTHLY_LINE_POINT_COUNT = 30
        private const val YEARLY_LINE_POINT_COUNT = 12
        private const val DEFAULT_LINE_POINT_COUNT = 12
        private const val MAX_VISITED_LOCATIONS = 5
    }

    private val odometerManager = OdometerManager(context)
    private val odometerStatistics = OdometerStatistics(context)
    private val routeHistoryManager = RouteHistoryManager(context)

    private val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())

    suspend fun getStats(period: OdometerManager.Period): StatsSummary {
        val now = System.currentTimeMillis()
        val startDate = when (period) {
            OdometerManager.Period.DAILY -> now - MILLIS_PER_DAY
            OdometerManager.Period.WEEKLY -> now - (DAYS_PER_WEEK * MILLIS_PER_DAY)
            OdometerManager.Period.MONTHLY -> now - (DAYS_PER_MONTH * MILLIS_PER_DAY)
            OdometerManager.Period.YEARLY -> now - (DAYS_PER_YEAR * MILLIS_PER_DAY)
        }

        val stats = odometerStatistics.getPeriodStats(startDate, period)
        val recentTrips = odometerManager.getRecentTrips()

        return StatsSummary(
            period = period,
            totalDistanceMeters = stats.totalDistanceMeters,
            averageSpeedKmh = stats.averageSpeedKmh,
            totalTimeSeconds = stats.totalTimeSeconds,
            tripCount = recentTrips.size.toLong(),
            formattedDistance = OdometerManager.formatDistance(stats.totalDistanceMeters),
            formattedTime = stats.formattedTime,
            startDate = startDate,
            endDate = now
        )
    }

    suspend fun getBarChartData(period: OdometerManager.Period): BarChartData {
        val now = System.currentTimeMillis()
        val numDays = when (period) {
            OdometerManager.Period.DAILY -> DAILY_BAR_DAY_COUNT
            OdometerManager.Period.WEEKLY -> WEEKLY_BAR_BUCKET_COUNT
            OdometerManager.Period.MONTHLY -> MONTHLY_BAR_BUCKET_COUNT
            OdometerManager.Period.YEARLY -> YEARLY_BAR_BUCKET_COUNT
            else -> DEFAULT_BAR_BUCKET_COUNT
        }
        val calendar = java.util.Calendar.getInstance()
        val labels = mutableListOf<String>()
        val values = mutableListOf<Float>()
        for (i in numDays - 1 downTo 0) {
            calendar.timeInMillis = now - (i * MILLIS_PER_DAY)
            labels.add(sdf.format(calendar.time))
            values.add(odometerManager.getDailyTotal(calendar.timeInMillis))
        }
        return BarChartData(labels, values)
    }

    suspend fun getLineChartData(period: OdometerManager.Period): LineChartData {
        val now = System.currentTimeMillis()
        val numPoints = when (period) {
            OdometerManager.Period.DAILY -> DAILY_LINE_POINT_COUNT
            OdometerManager.Period.WEEKLY -> WEEKLY_LINE_POINT_COUNT
            OdometerManager.Period.MONTHLY -> MONTHLY_LINE_POINT_COUNT
            OdometerManager.Period.YEARLY -> YEARLY_LINE_POINT_COUNT
            else -> DEFAULT_LINE_POINT_COUNT
        }
        val calendar = java.util.Calendar.getInstance()
        val labels = mutableListOf<String>()
        val values = mutableListOf<Float>()
        for (i in numPoints - 1 downTo 0) {
            calendar.timeInMillis = now - (i * MILLIS_PER_DAY)
            labels.add(sdf.format(calendar.time))
            values.add(odometerManager.getDailyTotal(calendar.timeInMillis))
        }
        return LineChartData(labels, values)
    }

    fun getMostVisitedLocations(): List<String> =
        routeHistoryManager.getAllRouteNames().take(MAX_VISITED_LOCATIONS)

    data class StatsSummary(
        val period: OdometerManager.Period,
        val totalDistanceMeters: Float,
        val averageSpeedKmh: Float,
        val totalTimeSeconds: Long,
        val tripCount: Long,
        val formattedDistance: String,
        val formattedTime: String,
        val startDate: Long,
        val endDate: Long
    )

    data class BarChartData(val labels: List<String>, val values: List<Float>)
    data class LineChartData(val labels: List<String>, val values: List<Float>)
}
