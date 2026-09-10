package com.nekogps.app.features.stats

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.ChipGroup
import com.google.android.material.button.MaterialButton
import com.nekogps.app.MapsActivity
import com.nekogps.app.NekoGpsApp
import com.nekogps.app.R

/**
 * FuelPriceActivity - displays nearby fuel stations sorted by price.
 */
class FuelPriceActivity : AppCompatActivity() {
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 2001
        private const val DEFAULT_LATITUDE = 40.7128
        private const val DEFAULT_LONGITUDE = -74.0060
    }

    private val fuelPriceManager = FuelPriceManager(this)
    private lateinit var adapter: FuelStationAdapter
    private var useMetric = true
    private var currentFilter = "All"

    private lateinit var lastLocation: Location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.nekogps.app.R.layout.activity_fuel_price)

        checkLocationPermission()
        setupRecyclerView()
        setupFilters()
        setupUnitToggle()
        setupBackButton()
        loadStations()
    }

    private fun setupRecyclerView() {
        adapter = FuelStationAdapter { station ->
            val price = fuelPriceManager.formatPrice(station, useMetric)
            Toast.makeText(this, "${station.name}: $price", Toast.LENGTH_SHORT).show()
        }
        findViewById<androidx.recyclerview.widget.RecyclerView>(com.nekogps.app.R.id.rvFuelStations).apply {
            layoutManager = LinearLayoutManager(this@FuelPriceActivity)
            adapter = this@FuelPriceActivity.adapter
        }
    }

    private fun setupFilters() {
        val chipGroup = findViewById<ChipGroup>(com.nekogps.app.R.id.chipGroupFuelType)
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            currentFilter = when (checkedIds.first()) {
                com.nekogps.app.R.id.chipAll -> "All"
                com.nekogps.app.R.id.chipRegular -> "REGULAR"
                com.nekogps.app.R.id.chipPremium -> "PREMIUM"
                com.nekogps.app.R.id.chipDiesel -> "DIESEL"
                com.nekogps.app.R.id.chipLpg -> "LPG"
                else -> "All"
            }
            filterStations()
        }
    }

    private fun setupUnitToggle() {
        findViewById<MaterialButton>(com.nekogps.app.R.id.btnToggleUnit).setOnClickListener {
            useMetric = !useMetric
            val toggleButton =
                findViewById<MaterialButton>(com.nekogps.app.R.id.btnToggleUnit)
            toggleButton.text = if (useMetric) {
                getString(com.nekogps.app.R.string.toggle_unit)
            } else {
                getString(com.nekogps.app.R.string.gal_per_liter)
            }
            adapter.setUseMetric(useMetric)
        }
    }

    private fun setupBackButton() {
        findViewById<MaterialButton>(com.nekogps.app.R.id.btnBack).setOnClickListener { finish() }
    }

    private fun loadStations() {
        val lat = lastLocation.latitude
        val lon = lastLocation.longitude

        val stations = fuelPriceManager.getNearbyStations(lat, lon)
        val filtered = if (currentFilter == "All") {
            stations
        } else {
            stations.filter { it.fuelType.name == currentFilter }
        }
        adapter.submitList(filtered)

        if (filtered.isEmpty()) {
            findViewById<View>(com.nekogps.app.R.id.tvEmpty).visibility = View.VISIBLE
            findViewById<androidx.recyclerview.widget.RecyclerView>(
                com.nekogps.app.R.id.rvFuelStations
            ).visibility = View.GONE
        } else {
            findViewById<View>(com.nekogps.app.R.id.tvEmpty).visibility = View.GONE
            findViewById<androidx.recyclerview.widget.RecyclerView>(
                com.nekogps.app.R.id.rvFuelStations
            ).visibility = View.VISIBLE
        }
    }

    private fun filterStations() {
        val lat = lastLocation.latitude
        val lon = lastLocation.longitude
        val stations = fuelPriceManager.getNearbyStations(lat, lon)
        val filtered = if (currentFilter == "All") stations else stations.filter { it.fuelType.name == currentFilter }
        adapter.submitList(filtered)
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            lastLocation = Location("").apply { latitude = DEFAULT_LATITUDE; longitude = DEFAULT_LONGITUDE }
        } else {
            lastLocation = (application as NekoGpsApp).getLastKnownLocation() ?: Location("").apply {
                latitude = DEFAULT_LATITUDE; longitude = DEFAULT_LONGITUDE
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                lastLocation = (application as NekoGpsApp).getLastKnownLocation() ?: Location("").apply {
                    latitude = DEFAULT_LATITUDE; longitude = DEFAULT_LONGITUDE
                }
                loadStations()
            }
        }
    }
}
