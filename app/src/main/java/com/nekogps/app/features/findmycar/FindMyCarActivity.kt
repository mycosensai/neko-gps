package com.nekogps.app.features.findmycar

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nekogps.app.MapsActivity
import com.nekogps.app.R
import com.nekogps.app.features.findmycar.ParkingLocation
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * FindMyCarActivity - displays saved parking location with walking directions.
 * Shows parking time elapsed and optional photo of parking spot.
 */
class FindMyCarActivity : AppCompatActivity() {
    private lateinit var findMyCar: FindMyCar
    private lateinit var tvParkingTime: TextView
    private lateinit var tvParkingAddress: TextView
    private lateinit var tvParkingNote: TextView
    private lateinit var tvWalkingDistance: TextView
    private lateinit var tvWalkingTime: TextView
    private lateinit var ivParkingPhoto: ImageView
    private lateinit var fabSaveParking: FloatingActionButton
    private lateinit var btnNavigateToCar: MaterialButton
    private lateinit var btnClearParking: MaterialButton

    private var currentParking: ParkingLocation? = null
    private var photoPath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_my_car)

        findMyCar = FindMyCar(this)
        initViews()
        observeParking()
    }

    private fun initViews() {
        tvParkingTime = findViewById(R.id.tvParkingTime)
        tvParkingAddress = findViewById(R.id.tvParkingAddress)
        tvParkingNote = findViewById(R.id.tvParkingNote)
        tvWalkingDistance = findViewById(R.id.tvWalkingDistance)
        tvWalkingTime = findViewById(R.id.tvWalkingTime)
        ivParkingPhoto = findViewById(R.id.ivParkingPhoto)
        fabSaveParking = findViewById(R.id.fabSaveParking)
        btnNavigateToCar = findViewById(R.id.btnNavigateToCar)
        btnClearParking = findViewById(R.id.btnClearParking)

        fabSaveParking.setOnClickListener { saveCurrentParking() }
        btnNavigateToCar.setOnClickListener { navigateToCar() }
        btnClearParking.setOnClickListener { clearParking() }
    }

    private fun observeParking() {
        lifecycleScope.launch {
            findMyCar.latestParking.collect { parking ->
                currentParking = parking
                if (parking != null) {
                    displayParking(parking)
                } else {
                    displayNoParking()
                }
            }
        }
    }

    private fun displayParking(parking: ParkingLocation) {
        tvParkingTime.text = "Parked: ${findMyCar.getParkingTimeElapsed(parking.parkedAt)}"
        tvParkingAddress.text = parking.address.ifEmpty { "${parking.latitude}, ${parking.longitude}" }
        tvParkingNote.text = parking.note.ifEmpty {
            if (parking.level.isNotEmpty() || parking.spotNumber.isNotEmpty()) {
                "Level: ${parking.level}, Spot: ${parking.spotNumber}"
            } else {
                "No notes"
            }
        }

        // Calculate walking distance and time (using 0,0 as placeholder - real location from GPS)
        val distance = findMyCar.getWalkingDistance(0.0, 0.0, parking)
        val walkTime = findMyCar.getWalkingTimeMinutes(distance)
        tvWalkingDistance.text = String.format(java.util.Locale.getDefault(), "Distance: %.0f m", distance)
        tvWalkingTime.text = "Walking: ~$walkTime min"

        // Load photo if exists
        if (parking.photoPath.isNotEmpty()) {
            val file = File(parking.photoPath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(parking.photoPath)
                ivParkingPhoto.setImageBitmap(bitmap)
            }
        }

        btnNavigateToCar.isEnabled = true
        btnClearParking.isEnabled = true
    }

    private fun displayNoParking() {
        tvParkingTime.text = "No parking saved"
        tvParkingAddress.text = "Tap + to save your parking location"
        tvParkingNote.text = ""
        tvWalkingDistance.text = ""
        tvWalkingTime.text = ""
        ivParkingPhoto.setImageResource(R.drawable.ic_location)
        btnNavigateToCar.isEnabled = false
        btnClearParking.isEnabled = false
    }

    private fun saveCurrentParking() {
        // In real app, get current GPS location
        val lat = intent.getDoubleExtra("current_lat", 0.0)
        val lon = intent.getDoubleExtra("current_lon", 0.0)

        lifecycleScope.launch {
            findMyCar.saveParkingLocation(
                FindMyCar.ParkingDetails(
                    latitude = lat,
                    longitude = lon,
                    note = "Saved from Find My Car"
                )
            )
            Toast.makeText(this@FindMyCarActivity, "Parking saved nya~", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToCar() {
        val parking = currentParking ?: return
        val intent = Intent(this, MapsActivity::class.java).apply {
            putExtra("navigate_to_lat", parking.latitude)
            putExtra("navigate_to_lon", parking.longitude)
            putExtra("navigate_to_name", "My Car")
        }
        startActivity(intent)
    }

    private fun clearParking() {
        lifecycleScope.launch {
            findMyCar.clearParking()
            Toast.makeText(this@FindMyCarActivity, "Parking cleared nya~", Toast.LENGTH_SHORT).show()
        }
    }
}
