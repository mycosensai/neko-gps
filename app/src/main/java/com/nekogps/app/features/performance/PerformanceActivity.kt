package com.nekogps.app.features.performance

import android.content.res.Configuration
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.nekogps.app.R
import org.osmdroid.util.GeoPoint
import android.util.Log

/**
 * Phase 10: Performance & Polish hub screen.
 * Demos TTS packs, battery stats, animations, and tablet/foldable panes.
 */
class PerformanceActivity : AppCompatActivity() {

    private lateinit var ttsManager: OfflineTTSManager
    private lateinit var batteryManager: BatteryOptimizationManager
    private lateinit var animationManager: AnimationManager
    private lateinit var tabletManager: TabletLayoutManager
    private lateinit var foldableManager: FoldableSupportManager

    private lateinit var listPane: LinearLayout
    private lateinit var detailPane: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var batteryText: TextView
    private lateinit var postureText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_performance)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.performance_title)

        ttsManager = OfflineTTSManager(this)
        ttsManager.listener = object : OfflineTTSManager.Listener {
            override fun onTtsReady() { updateTtsStatus() }
            override fun onLanguageChanged(code: String) { updateTtsStatus() }
        }
        ttsManager.init()

        batteryManager = BatteryOptimizationManager(this)
        batteryManager.listener = object : BatteryOptimizationListener {
            override fun onIntervalChanged(ms: Long) { updateBatteryStatus() }
            override fun onBatteryStats(s: BatteryOptimizationManager.BatteryStats) { updateBatteryStatus() }
        }
        batteryManager.startTracking()

        animationManager = AnimationManager()
        tabletManager = TabletLayoutManager(this)
        foldableManager = FoldableSupportManager()
        foldableManager.listener = object : FoldableSupportManager.FoldableListener {
            override fun onPostureChanged(p: FoldableSupportManager.Posture) {
                postureText.text = getString(R.string.performance_posture, p.name)
                foldableManager.adaptLayout(this@PerformanceActivity, listPane, detailPane, tabletManager)
            }
        }

        listPane = findViewById(R.id.list_pane)
        detailPane = findViewById(R.id.detail_pane)
        statusText = findViewById(R.id.tts_status)
        batteryText = findViewById(R.id.battery_status)
        postureText = findViewById(R.id.posture_status)

        findViewById<Button>(R.id.btn_tts_test).setOnClickListener {
            ttsManager.speak(getString(R.string.performance_tts_demo))
        }
        findViewById<Button>(R.id.btn_tts_download).setOnClickListener {
            ttsManager.downloadVoicePack()
        }
        findViewById<Button>(R.id.btn_fade_demo).setOnClickListener {
            animationManager.fadeOut(detailPane, FADE_DEMO_DURATION_MS, false)
            animationManager.fadeIn(detailPane, FADE_DEMO_DURATION_MS)
        }
        val rateBar: SeekBar = findViewById(R.id.tts_rate_bar)
        rateBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) ttsManager.setSpeechRate(MIN_SPEECH_RATE + p / RATE_DIVISOR)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) = Unit
            override fun onStopTrackingTouch(sb: SeekBar?) = Unit
        })

        tabletManager.applyMode(listPane, detailPane)
        foldableManager.refresh(this)
        postureText.text = getString(R.string.performance_posture, foldableManager.currentPosture(this).name)
        updateTtsStatus()
        updateBatteryStatus()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        foldableManager.onConfigurationChanged(this, newConfig)
    }

    override fun onBackPressed() {
        if (!tabletManager.onBackPressed(listPane, detailPane)) super.onBackPressed()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        batteryManager.stopTracking()
        ttsManager.shutdown()
        super.onDestroy()
    }

    private fun updateTtsStatus() {
        val packs = try { ttsManager.getVoicePacks() } catch (e: IllegalStateException) {
            Log.w("PerformanceActivity", "updateTtsStatus: suppressed Exception", e)
            emptyList() }
        val installed = packs.count { it.installed }
        statusText.text = getString(R.string.performance_tts_status, installed, packs.size)
    }

    private fun updateBatteryStatus() {
        val s = try { batteryManager.getStats() } catch (e: IllegalStateException) {
            Log.w("PerformanceActivity", "updateBatteryStatus: suppressed Exception", e)
            null } catch (e: SecurityException) {
            Log.w("PerformanceActivity", "updateBatteryStatus: suppressed Exception", e)
            null } ?: return
        batteryText.text = getString(
            R.string.performance_battery_status,
            s.levelPercent,
            s.locationIntervalMs / MILLIS_PER_SECOND,
            if (s.isPowerSaveMode) 1 else 0
        )
        // Demo adaptive interval with a synthetic fix (no-op权重):
        try {
            val loc = Location(LocationManager.GPS_PROVIDER)
            batteryManager.onLocationUpdate(loc)
        } catch (e: IllegalArgumentException) {
            Log.w("PerformanceActivity", "updateBatteryStatus: suppressed Exception", e)
            /* ignore */ }
    }

    companion object {
        private const val FADE_DEMO_DURATION_MS = 300L
        private const val MIN_SPEECH_RATE = 0.5f
        private const val RATE_DIVISOR = 100f
        private const val MILLIS_PER_SECOND = 1000L
    }
}
