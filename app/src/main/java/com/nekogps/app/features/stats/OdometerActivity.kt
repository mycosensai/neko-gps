package com.nekogps.app.features.stats

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

/**
 * OdometerActivity - displays lifetime distance tracker.
 */
class OdometerActivity : AppCompatActivity() {
    companion object {
        private const val MILLIS_PER_DAY = 86_400_000L
        private const val DAYS_PER_WEEK = 7
        private const val DAYS_PER_MONTH = 30
        private const val DAYS_PER_YEAR = 365L
        private const val SECONDS_PER_HOUR = 3600
        private const val SECONDS_PER_MINUTE = 60
        private const val MS_TO_KMH_FACTOR = 3.6f
        private const val PERIOD_DAY_INDEX = 0
        private const val PERIOD_WEEK_INDEX = 1
        private const val PERIOD_MONTH_INDEX = 2
        private const val PERIOD_YEAR_INDEX = 3
    }

    private val odometerManager = OdometerManager(this)
    private val odometerStatistics = OdometerStatistics(this)
    private val tripAdapter = TripRecordAdapter(this)
    private var currentPeriod = OdometerManager.Period.MONTHLY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.nekogps.app.R.layout.activity_odometer)

        setupRecyclerView()
        setupPeriodTabs()
        loadData()
        setupBackButton()
        setupClearButton()
    }

    private fun setupRecyclerView() {
        findViewById<androidx.recyclerview.widget.RecyclerView>(com.nekogps.app.R.id.rvRecentTrips).apply {
            layoutManager = LinearLayoutManager(this@OdometerActivity)
            adapter = tripAdapter
        }
    }

    private fun setupPeriodTabs() {
        findViewById<MaterialButton>(
            com.nekogps.app.R.id.tabDay
        ).setOnClickListener { setPeriod(OdometerManager.Period.DAILY) }
        findViewById<MaterialButton>(
            com.nekogps.app.R.id.tabWeek
        ).setOnClickListener { setPeriod(OdometerManager.Period.WEEKLY) }
        findViewById<MaterialButton>(
            com.nekogps.app.R.id.tabMonth
        ).setOnClickListener { setPeriod(OdometerManager.Period.MONTHLY) }
        findViewById<MaterialButton>(
            com.nekogps.app.R.id.tabYear
        ).setOnClickListener { setPeriod(OdometerManager.Period.YEARLY) }
    }

    private fun setPeriod(period: OdometerManager.Period) {
        currentPeriod = period
        updateTabStyles()
        loadData()
    }

    private fun updateTabStyles() {
        val tabs = listOf(
            com.nekogps.app.R.id.tabDay,
            com.nekogps.app.R.id.tabWeek,
            com.nekogps.app.R.id.tabMonth,
            com.nekogps.app.R.id.tabYear
        )
        val periodOrdinal = when (currentPeriod) {
            OdometerManager.Period.DAILY -> PERIOD_DAY_INDEX
            OdometerManager.Period.WEEKLY -> PERIOD_WEEK_INDEX
            OdometerManager.Period.MONTHLY -> PERIOD_MONTH_INDEX
            OdometerManager.Period.YEARLY -> PERIOD_YEAR_INDEX
            else -> PERIOD_MONTH_INDEX
        }
        tabs.forEachIndexed { i, id ->
            val tab = findViewById<MaterialButton>(id)
            if (i == periodOrdinal) {
                tab.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_blue_light))
                tab.setTextColor(android.R.color.black)
            } else {
                tab.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
                tab.setTextColor(android.R.color.white)
            }
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            val now = System.currentTimeMillis()
            val startDate = when (currentPeriod) {
                OdometerManager.Period.DAILY -> now - MILLIS_PER_DAY
                OdometerManager.Period.WEEKLY -> now - (DAYS_PER_WEEK * MILLIS_PER_DAY)
                OdometerManager.Period.MONTHLY -> now - (DAYS_PER_MONTH * MILLIS_PER_DAY)
                OdometerManager.Period.YEARLY -> now - (DAYS_PER_YEAR * MILLIS_PER_DAY)
                else -> now - (DAYS_PER_MONTH * MILLIS_PER_DAY)
            }

            val stats = odometerStatistics.getPeriodStats(startDate, currentPeriod)
            val lifetime = odometerManager.getLifetimeTotal()

            findViewById<TextView>(
                com.nekogps.app.R.id.tvLifetimeDistance
            ).text = OdometerManager.formatDistance(lifetime)
            findViewById<TextView>(com.nekogps.app.R.id.tvAvgSpeed).text = "%.0f km/h".format(stats.averageSpeedKmh)

            val hours = stats.totalTimeSeconds / SECONDS_PER_HOUR
            val mins = (stats.totalTimeSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
            findViewById<TextView>(com.nekogps.app.R.id.tvTotalTime).text = "${hours}h ${mins}m"
            findViewById<TextView>(
                com.nekogps.app.R.id.tvTripCount
            ).text = "${odometerStatistics.getTripCount()} trips recorded"

            val trips = odometerManager.getRecentTrips()
            val tripRecords = trips.map { entity ->
                TripRecord(
                    id = entity.id,
                    startTime = entity.date,
                    totalDistanceMeters = entity.dailyDistanceMeters,
                    totalTimeSeconds = entity.dailyDistanceMeters.toLong(),
                    averageSpeedKmh = if (entity.dailyDistanceMeters > 0) {
                        (entity.dailyDistanceMeters / SECONDS_PER_HOUR) * MS_TO_KMH_FACTOR
                    } else {
                        0f
                    }
                )
            }
            tripAdapter.submitList(tripRecords)
        }
    }

    private fun setupBackButton() {
        findViewById<MaterialButton>(com.nekogps.app.R.id.btnBack).setOnClickListener { finish() }
    }

    private fun setupClearButton() {
        findViewById<MaterialButton>(com.nekogps.app.R.id.btnClear).setOnClickListener {
            lifecycleScope.launch {
                odometerManager.clearAll()
                tripAdapter.submitList(emptyList())
                findViewById<TextView>(com.nekogps.app.R.id.tvLifetimeDistance).text = "0.0 km"
                findViewById<TextView>(com.nekogps.app.R.id.tvAvgSpeed).text = "0 km/h"
                findViewById<TextView>(com.nekogps.app.R.id.tvTotalTime).text = "0h 0m"
                findViewById<TextView>(com.nekogps.app.R.id.tvTripCount).text = "0 trips recorded"
                Toast.makeText(
                    this@OdometerActivity,
                    getString(com.nekogps.app.R.string.all_data_cleared),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
