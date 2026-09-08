package com.nekogps.app

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial
import com.nekogps.app.utils.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * DataStore-based settings screen.
 * Settings: map layer, GPS update interval, distance units, theme, auto-center toggle.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var spinnerMapLayer: Spinner
    private lateinit var spinnerGpsInterval: Spinner
    private lateinit var rgUnits: RadioGroup
    private lateinit var rgTheme: RadioGroup
    private lateinit var switchAutoCenter: SwitchMaterial
    private lateinit var btnClearTracks: Button
    private lateinit var btnExportData: Button

    private lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settingsDataStore = SettingsDataStore(applicationContext)

        spinnerMapLayer = findViewById(R.id.spinnerMapLayer)
        spinnerGpsInterval = findViewById(R.id.spinnerGpsInterval)
        rgUnits = findViewById(R.id.rgUnits)
        rgTheme = findViewById(R.id.rgTheme)
        switchAutoCenter = findViewById(R.id.switchAutoCenter)
        btnClearTracks = findViewById(R.id.btnClearTracks)
        btnExportData = findViewById(R.id.btnExportData)

        setupMapLayerSpinner()
        setupGpsIntervalSpinner()
        setupDistanceUnitsRadio()
        setupThemeRadio()
        setupAutoCenterSwitch()
        setupDataButtons()
        loadCurrentSettings()
    }

    private fun setupMapLayerSpinner() {
        val layers = arrayOf("Standard", "Satellite", "Cycle", "Hiking")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, layers)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMapLayer.adapter = adapter
        spinnerMapLayer.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = layers[position]
                lifecycleScope.launch {
                    settingsDataStore.setMapLayer(selected)
                    showToast("Map layer set to: $selected")
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupGpsIntervalSpinner() {
        val intervals = arrayOf("1 second", "2 seconds", "5 seconds")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, intervals)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGpsInterval.adapter = adapter
        spinnerGpsInterval.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedMs = when (position) {
                    0 -> 1000L
                    1 -> 2000L
                    2 -> 5000L
                    else -> 2000L
                }
                lifecycleScope.launch {
                    settingsDataStore.setGpsInterval(selectedMs)
                    showToast("GPS interval set to: ${intervals[position]}")
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupDistanceUnitsRadio() {
        rgUnits.setOnCheckedChangeListener { _, checkedId ->
            val selected = when (checkedId) {
                R.id.rbImperial -> "Imperial"
                else -> "Metric"
            }
            lifecycleScope.launch {
                settingsDataStore.setDistanceUnits(selected)
                showToast("Distance units set to: $selected")
            }
        }
    }

    private fun setupThemeRadio() {
        rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val selected = when (checkedId) {
                R.id.rbLight -> "Light"
                R.id.rbSystem -> "System"
                else -> "Dark"
            }
            lifecycleScope.launch {
                settingsDataStore.setTheme(selected)
                showToast("Theme set to: $selected")
            }
        }
    }

    private fun setupAutoCenterSwitch() {
        switchAutoCenter.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                settingsDataStore.setAutoCenter(isChecked)
                showToast("Auto-center: ${if (isChecked) "ON" else "OFF"}")
            }
        }
    }

    private fun setupDataButtons() {
        btnClearTracks.setOnClickListener {
            Toast.makeText(this, "Tracks cleared", Toast.LENGTH_SHORT).show()
        }
        btnExportData.setOnClickListener {
            Toast.makeText(this, "Export coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadCurrentSettings() {
        lifecycleScope.launch {
            // Map layer
            val mapLayer = settingsDataStore.mapLayer.first()
            val layerPos = when (mapLayer) {
                "Satellite" -> 1
                "Cycle" -> 2
                "Hiking" -> 3
                else -> 0
            }
            spinnerMapLayer.setSelection(layerPos)

            // GPS interval
            val gpsInterval = settingsDataStore.gpsInterval.first()
            val intervalPos = when (gpsInterval) {
                1000L -> 0
                5000L -> 2
                else -> 1
            }
            spinnerGpsInterval.setSelection(intervalPos)

            // Distance units
            val units = settingsDataStore.distanceUnits.first()
            if (units == "Imperial") {
                rgUnits.check(R.id.rbImperial)
            } else {
                rgUnits.check(R.id.rbMetric)
            }

            // Theme
            val theme = settingsDataStore.theme.first()
            when (theme) {
                "Light" -> rgTheme.check(R.id.rbLight)
                "System" -> rgTheme.check(R.id.rbSystem)
                else -> rgTheme.check(R.id.rbDark)
            }

            // Auto-center
            switchAutoCenter.isChecked = settingsDataStore.autoCenter.first()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
