package com.nekogps.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.nekogps.app.databinding.ActivityMainBinding
import com.nekogps.app.features.safety.SafetyActivity

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkLocationPermission()
        setupUI()
    }

    private fun setupUI() {
        binding.btnOpenMap.setOnClickListener {
            startActivity(Intent(this, MapsActivity::class.java))
        }
        binding.btnStartNavigation.setOnClickListener {
            startActivity(Intent(this, NavigationActivity::class.java))
        }
        binding.btnTracks.setOnClickListener {
            startActivity(Intent(this, TracksActivity::class.java))
        }
        binding.btnPoiSearch.setOnClickListener {
            startActivity(Intent(this, PoiSearchActivity::class.java))
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.btnSafety.setOnClickListener {
            startActivity(Intent(this, SafetyActivity::class.java))
        }
    }

    private fun checkLocationPermission() {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        
        if (fineLocation != PackageManager.PERMISSION_GRANTED || coarseLocation != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "GPS Permission Granted nya~", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "GPS Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
