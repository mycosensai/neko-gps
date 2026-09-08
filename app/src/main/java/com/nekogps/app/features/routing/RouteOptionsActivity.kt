package com.nekogps.app.features.routing

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.nekogps.app.R
import com.nekogps.app.databinding.ActivityRouteOptionsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RouteOptionsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRouteOptionsBinding
    private val routeOptionsManager by lazy { RouteOptionsManager(this) }
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRouteOptionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupViews()
        loadPreferences()
        setupApplyButton()
    }

    private fun setupViews() {
        binding.tvTitle.text = getString(R.string.route_options)
    }

    private fun loadPreferences() {
        scope.launch {
            val prefs = routeOptionsManager.getPreferences()
            binding.switchAvoidTolls.isChecked = prefs.avoidTolls
            binding.switchAvoidHighways.isChecked = prefs.avoidHighways
            binding.switchAvoidFerries.isChecked = prefs.avoidFerries
            updateSwitchStatus(binding.switchAvoidTolls, binding.tvTollStatus)
            updateSwitchStatus(binding.switchAvoidHighways, binding.tvHighwayStatus)
            updateSwitchStatus(binding.switchAvoidFerries, binding.tvFerryStatus)
            binding.sliderTollPenalty.value = prefs.tollPenaltyMultiplier.toFloat()
            binding.sliderHighwayPenalty.value = prefs.highwayPenaltyMultiplier.toFloat()
            binding.sliderFerryPenalty.value = prefs.ferryPenaltyMultiplier.toFloat()
            updateMultiplierText(binding.sliderTollPenalty, binding.tvTollMultiplierValue)
            updateMultiplierText(binding.sliderHighwayPenalty, binding.tvHighwayMultiplierValue)
            updateMultiplierText(binding.sliderFerryPenalty, binding.tvFerryMultiplierValue)
        }
    }

    private fun updateSwitchStatus(switch: SwitchMaterial, statusText: android.widget.TextView) {
        switch.setOnCheckedChangeListener { _, isChecked ->
            statusText.text = if (isChecked) "ON" else "OFF"
            statusText.setTextColor(
                if (isChecked) ContextCompat.getColor(this, R.color.lavender_glow)
                else ContextCompat.getColor(this, R.color.error)
            )
        }
    }

    private fun updateMultiplierText(slider: Slider, textView: android.widget.TextView) {
        slider.addOnChangeListener { _, value, _ ->
            textView.text = String.format("%.1fx", value)
        }
    }

    private fun setupApplyButton() {
        binding.btnApplyPreferences.setOnClickListener {
            savePreferences()
            Toast.makeText(this, R.string.apply_preferences, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun savePreferences() {
        scope.launch {
            routeOptionsManager.setAvoidTolls(binding.switchAvoidTolls.isChecked)
            routeOptionsManager.setAvoidHighways(binding.switchAvoidHighways.isChecked)
            routeOptionsManager.setAvoidFerries(binding.switchAvoidFerries.isChecked)
            routeOptionsManager.setPenaltyMultiplier(AvoidanceType.TOLLS, binding.sliderTollPenalty.value.toDouble())
            routeOptionsManager.setPenaltyMultiplier(AvoidanceType.HIGHWAYS, binding.sliderHighwayPenalty.value.toDouble())
            routeOptionsManager.setPenaltyMultiplier(AvoidanceType.FERRIES, binding.sliderFerryPenalty.value.toDouble())
        }
    }
}
