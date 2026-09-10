package com.nekogps.app.features.stats

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * StatisticsDashboard - aggregates trip statistics.
 */
class StatisticsDashboard(private val context: Context) {
    private val odometerManager = OdometerManager(context)
    private val routeHistoryManager = RouteHistoryManager(context)

    private val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())

    suspend fun getStats(period: OdometerManager.Period): StatsSummary {
        val now = System.currentTimeMillis()
        val startDate = when (period) {
            OdometerManager.Period.DAILY -> now - 86_400_000L
            OdometerManager.Period.WEEKLY -> now - (7 * 86_400_000L)
            OdometerManager.Period.MONTHLY -> now - (30 * 86_400_000L)
            OdometerManager.Period.YEARLY -> now - (365L * 86_400_000L)
        }

        val stats = odometerManager.getPeriodStats(startDate, period)
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
            OdometerManager.Period.DAILY -> 7
            OdometerManager.Period.WEEKLY -> 4
            OdometerManager.Period.MONTHLY -> 12
            OdometerManager.Period.YEARLY -> 12
            else -> 12
        }
        val calendar = java.util.Calendar.getInstance()
        val labels = mutableListOf<String>()
        val values = mutableListOf<Float>()
        for (i in numDays - 1 downTo 0) {
            calendar.timeInMillis = now - (i * 86_400_000L)
            labels.add(sdf.format(calendar.time))
            values.add(odometerManager.getDailyTotal(calendar.timeInMillis))
        }
        return BarChartData(labels, values)
    }

    suspend fun getLineChartData(period: OdometerManager.Period): LineChartData {
        val now = System.currentTimeMillis()
        val numPoints = when (period) {
            OdometerManager.Period.DAILY -> 24
            OdometerManager.Period.WEEKLY -> 7
            OdometerManager.Period.MONTHLY -> 30
            OdometerManager.Period.YEARLY -> 12
            else -> 12
        }
        val calendar = java.util.Calendar.getInstance()
        val labels = mutableListOf<String>()
        val values = mutableListOf<Float>()
        for (i in numPoints - 1 downTo 0) {
            calendar.timeInMillis = now - (i * 86_400_000L)
            labels.add(sdf.format(calendar.time))
            values.add(odometerManager.getDailyTotal(calendar.timeInMillis))
        }
        return LineChartData(labels, values)
    }

    fun getMostVisitedLocations(): List<String> = routeHistoryManager.getAllRouteNames().take(5)

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
