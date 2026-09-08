package com.nekogps.app.features.routing

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.nekogps.app.R
import com.nekogps.app.databinding.ActivityRoundTripBinding
import com.nekogps.app.utils.DistanceCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

class RoundTripActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRoundTripBinding
    private val roundTripGenerator by lazy { RoundTripGenerator(this) }
    private val routeOptionsManager by lazy { RouteOptionsManager(this) }
    private val scope = CoroutineScope(Dispatchers.Main)
    private var selectedStrategy = RoundTripGenerator.MidwayStrategy.MIDPOINT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoundTripBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupViews()
        setupStrategyButtons()
        setupGenerateButton()
        setupReturnWaypointFields()
    }

    private fun setupViews() {
        binding.tvTitle.text = getString(R.string.round_trip_generator)
    }

    private fun setupStrategyButtons() {
        binding.btnStrategyMidpoint.setOnClickListener { selectedStrategy = RoundTripGenerator.MidwayStrategy.MIDPOINT; highlightBtn(binding.btnStrategyMidpoint) }
        binding.btnStrategyShorter.setOnClickListener { selectedStrategy = RoundTripGenerator.MidwayStrategy.SHORTER_RETURN; highlightBtn(binding.btnStrategyShorter) }
        binding.btnStrategyScenic.setOnClickListener { selectedStrategy = RoundTripGenerator.MidwayStrategy.SCENIC; highlightBtn(binding.btnStrategyScenic) }
        highlightBtn(binding.btnStrategyMidpoint)
    }

    private fun highlightBtn(selected: Button) {
        val sel = R.color.lavender_glow
        val norm = R.color.charcoal_ink
        binding.btnStrategyMidpoint.setBackgroundColor(ContextCompat.getColor(this, if (selected == binding.btnStrategyMidpoint) sel else norm))
        binding.btnStrategyShorter.setBackgroundColor(ContextCompat.getColor(this, if (selected == binding.btnStrategyShorter) sel else norm))
        binding.btnStrategyScenic.setBackgroundColor(ContextCompat.getColor(this, if (selected == binding.btnStrategyScenic) sel else norm))
    }

    private fun setupGenerateButton() {
        binding.btnGenerateTrip.setOnClickListener { generateRoundTrip() }
    }

    private fun setupReturnWaypointFields() {
        binding.etWaypointLat.setText("35.6674")
        binding.etWaypointLng.setText("139.6979")
        binding.etWaypointName.setText("Return Waypoint")
    }

    private fun generateRoundTrip() {
        val config = RoundTripGenerator.RoundTripConfig(
            origin = GeoPoint(35.6762, 139.6503),
            destination = GeoPoint(35.6586, 139.7454),
            returnWaypointLat = binding.etWaypointLat.text.toString().toDoubleOrNull(),
            returnWaypointLon = binding.etWaypointLng.text.toString().toDoubleOrNull(),
            returnWaypointName = binding.etWaypointName.text.toString().ifEmpty { "Return Waypoint" },
            optimizeReturn = true
        )
        scope.launch {
            val result = roundTripGenerator.generateRoundTrip(config = config, routeOptionsManager = routeOptionsManager)
            displayResult(result)
        }
    }

    private fun displayResult(result: RoundTripGenerator.RoundTripResult) {
        binding.tvOutboundDistance.text = DistanceCalculator.formatDistance(result.outboundDistanceMeters)
        binding.tvOutboundEta.text = DistanceCalculator.formatETA(result.outboundEtaMinutes)
        binding.tvReturnDistance.text = DistanceCalculator.formatDistance(result.returnDistanceMeters)
        binding.tvReturnEta.text = DistanceCalculator.formatETA(result.returnEtaMinutes)
        binding.tvTotalDistance.text = DistanceCalculator.formatDistance(result.totalDistanceMeters)
        binding.tvTotalEta.text = DistanceCalculator.formatETA(result.totalEtaMinutes)
        Toast.makeText(this, "${getString(R.string.round_trip_complete)}: ${DistanceCalculator.formatDistance(result.totalDistanceMeters)}", Toast.LENGTH_LONG).show()
    }
}
