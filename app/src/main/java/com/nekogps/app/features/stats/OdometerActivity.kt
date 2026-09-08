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
    private val odometerManager = OdometerManager(this)
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
        findViewById<MaterialButton>(com.nekogps.app.R.id.tabDay).setOnClickListener { setPeriod(OdometerManager.Period.DAILY) }
        findViewById<MaterialButton>(com.nekogps.app.R.id.tabWeek).setOnClickListener { setPeriod(OdometerManager.Period.WEEKLY) }
        findViewById<MaterialButton>(com.nekogps.app.R.id.tabMonth).setOnClickListener { setPeriod(OdometerManager.Period.MONTHLY) }
        findViewById<MaterialButton>(com.nekogps.app.R.id.tabYear).setOnClickListener { setPeriod(OdometerManager.Period.YEARLY) }
    }

    private fun setPeriod(period: OdometerManager.Period) {
        currentPeriod = period
        updateTabStyles()
        loadData()
    }

    private fun updateTabStyles() {
        val tabs = listOf(com.nekogps.app.R.id.tabDay, com.nekogps.app.R.id.tabWeek, com.nekogps.app.R.id.tabMonth, com.nekogps.app.R.id.tabYear)
        val periodOrdinal = when (currentPeriod) {
            OdometerManager.Period.DAILY -> 0
            OdometerManager.Period.WEEKLY -> 1
            OdometerManager.Period.MONTHLY -> 2
            OdometerManager.Period.YEARLY -> 3
            else -> 2
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
                OdometerManager.Period.DAILY -> now - 86_400_000L
                OdometerManager.Period.WEEKLY -> now - (7 * 86_400_000L)
                OdometerManager.Period.MONTHLY -> now - (30 * 86_400_000L)
                OdometerManager.Period.YEARLY -> now - (365L * 86_400_000L)
                else -> now - (30 * 86_400_000L)
            }

            val stats = odometerManager.getPeriodStats(startDate, currentPeriod)
            val lifetime = odometerManager.getLifetimeTotal()

            findViewById<TextView>(com.nekogps.app.R.id.tvLifetimeDistance).text = OdometerManager.formatDistance(lifetime)
            findViewById<TextView>(com.nekogps.app.R.id.tvAvgSpeed).text = "%.0f km/h".format(stats.averageSpeedKmh)

            val hours = stats.totalTimeSeconds / 3600
            val mins = (stats.totalTimeSeconds % 3600) / 60
            findViewById<TextView>(com.nekogps.app.R.id.tvTotalTime).text = "${hours}h ${mins}m"
            findViewById<TextView>(com.nekogps.app.R.id.tvTripCount).text = "${odometerManager.getTripCount()} trips recorded"

            val trips = odometerManager.getRecentTrips()
            val tripRecords = trips.map { entity ->
                TripRecord(
                    id = entity.id,
                    startTime = entity.date,
                    totalDistanceMeters = entity.dailyDistanceMeters,
                    totalTimeSeconds = entity.dailyDistanceMeters.toLong(),
                    averageSpeedKmh = if (entity.dailyDistanceMeters > 0) (entity.dailyDistanceMeters / 3600) * 3.6f else 0f
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
                Toast.makeText(this@OdometerActivity, getString(com.nekogps.app.R.string.all_data_cleared), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
