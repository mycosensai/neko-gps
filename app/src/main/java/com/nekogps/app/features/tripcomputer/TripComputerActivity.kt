package com.nekogps.app.features.tripcomputer

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationRequest
import com.google.android.material.slider.Slider
import com.nekogps.app.R
import com.nekogps.app.databinding.ActivityTripComputerBinding
import com.nekogps.app.utils.DistanceCalculator
import org.osmdroid.util.GeoPoint
import java.util.concurrent.TimeUnit

/**
 * Trip Computer screen showing real-time trip statistics.
 * Displays distance traveled, elapsed time, average speed, max speed, and fuel cost estimate.
 */
class TripComputerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTripComputerBinding
    private lateinit var viewModel: TripComputerViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val handler = Handler(Looper.getMainLooper())
    private var isTracking = false

    companion object {
        private const val TAG = "TripComputerActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripComputerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[TripComputerViewModel::class.java]
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnStartTrip.setOnClickListener {
            startTrip()
        }

        binding.btnStopTrip.setOnClickListener {
            stopTrip()
        }

        binding.btnPauseTrip.setOnClickListener {
            pauseTrip()
        }

        binding.sliderFuelConsumption.addOnChangeListener { _, value, _ ->
            viewModel.setFuelConsumption(value.toDouble())
        }

        binding.sliderFuelPrice.addOnChangeListener { _, value, _ ->
            viewModel.setFuelPrice(value.toDouble())
        }
    }

    private fun observeViewModel() {
        viewModel.tripDistanceMeters.observe(this) { distance ->
            binding.tvDistance.text = DistanceCalculator.formatDistance(distance)
        }

        viewModel.elapsedTimeMillis.observe(this) { time ->
            binding.tvElapsedTime.text = viewModel.getFormattedElapsedTime()
        }

        viewModel.averageSpeedKmh.observe(this) { speed ->
            binding.tvAvgSpeed.text = String.format("%.1f km/h", speed)
        }

        viewModel.maxSpeedKmh.observe(this) { speed ->
            binding.tvMaxSpeed.text = String.format("%.1f km/h", speed)
        }

        viewModel.currentSpeedKmh.observe(this) { speed ->
            binding.tvCurrentSpeed.text = String.format("%.0f km/h", speed)
        }

        viewModel.fuelCostEstimate.observe(this) { cost ->
            binding.tvFuelCost.text = String.format("$%.2f", cost)
        }

        viewModel.isTripActive.observe(this) { isActive ->
            binding.btnStartTrip.visibility = if (isActive) View.GONE else View.VISIBLE
            binding.btnStopTrip.visibility = if (isActive) View.VISIBLE else View.GONE
            binding.btnPauseTrip.visibility = if (isActive) View.VISIBLE else View.GONE
        }
    }

    private fun startTrip() {
        isTracking = true
        viewModel.startTrip()
        requestLocationUpdates()
        Toast.makeText(this, "Trip started!", Toast.LENGTH_SHORT).show()
    }

    private fun stopTrip() {
        isTracking = false
        viewModel.stopTrip()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Toast.makeText(this, "Trip stopped", Toast.LENGTH_SHORT).show()
    }

    private fun pauseTrip() {
        if (viewModel.isTripActive.value == true) {
            viewModel.pauseTrip()
            binding.btnPauseTrip.text = "Resume"
            Toast.makeText(this, "Trip paused", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.resumeTrip()
            binding.btnPauseTrip.text = "Pause"
            Toast.makeText(this, "Trip resumed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = TimeUnit.SECONDS.toMillis(1)
            fastestInterval = TimeUnit.MILLISECONDS.toMillis(500)
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private val locationCallback = object : com.google.android.gms.location.LocationCallback() {
        override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
            val loc = result.lastLocation ?: return
            val geoPoint = GeoPoint(loc.latitude, loc.longitude)
            val speedKmh = (loc.speed * 3.6) // Convert m/s to km/h
            viewModel.updateLocation(geoPoint, speedKmh)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
