package com.nekogps.app.features.stats

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

/**
 * StatisticsActivity - tabbed view with bar/line charts.
 */
class StatisticsActivity : AppCompatActivity() {
    private val dashboard = StatisticsDashboard(this)
    private var currentPeriod = OdometerManager.Period.MONTHLY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.nekogps.app.R.layout.activity_statistics)

        setupTabs()
        setupBackButton()
        loadData()
    }

    private fun setupTabs() {
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
            val stats = dashboard.getStats(currentPeriod)

            findViewById<TextView>(com.nekogps.app.R.id.tvStatDistance).text = stats.formattedDistance
            findViewById<TextView>(com.nekogps.app.R.id.tvStatAvgSpeed).text = "%.0f km/h".format(stats.averageSpeedKmh)
            findViewById<TextView>(com.nekogps.app.R.id.tvStatTime).text = stats.formattedTime

            val barData = dashboard.getBarChartData(currentPeriod)
            val lineData = dashboard.getLineChartData(currentPeriod)

            findViewById<SimpleBarChart>(com.nekogps.app.R.id.barChart).setData(barData.values, barData.labels)
            findViewById<SimpleLineChart>(com.nekogps.app.R.id.lineChart).setData(lineData.values, lineData.labels)
        }
    }

    private fun setupBackButton() {
        findViewById<MaterialButton>(com.nekogps.app.R.id.btnBack).setOnClickListener { finish() }
    }
}
