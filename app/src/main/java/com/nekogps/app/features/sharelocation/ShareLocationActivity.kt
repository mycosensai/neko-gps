package com.nekogps.app.features.sharelocation

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.nekogps.app.R

/**
 * ShareLocationActivity - UI for sharing current location via SMS, email, or messaging apps.
 */
class ShareLocationActivity : AppCompatActivity() {
    private lateinit var shareLocation: ShareLocation
    private lateinit var tvCoordinates: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvMapsLink: TextView

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_location)

        shareLocation = ShareLocation(this)
        initViews()

        latitude = intent.getDoubleExtra("current_lat", 0.0)
        longitude = intent.getDoubleExtra("current_lon", 0.0)

        displayLocation()
    }

    private fun initViews() {
        tvCoordinates = findViewById(R.id.tvShareCoordinates)
        tvAddress = findViewById(R.id.tvShareAddress)
        tvMapsLink = findViewById(R.id.tvShareMapsLink)

        findViewById<MaterialButton>(R.id.btnShareSms).setOnClickListener {
            shareLocation.shareViaSms(latitude, longitude)
        }
        findViewById<MaterialButton>(R.id.btnShareEmail).setOnClickListener {
            shareLocation.shareViaEmail(latitude, longitude)
        }
        findViewById<MaterialButton>(R.id.btnShareMessage).setOnClickListener {
            shareLocation.shareViaMessaging(latitude, longitude)
        }
        findViewById<MaterialButton>(R.id.btnShareGeneric).setOnClickListener {
            shareLocation.shareGeneric(latitude, longitude)
        }
        findViewById<MaterialButton>(R.id.btnOpenMaps).setOnClickListener {
            shareLocation.openInGoogleMaps(latitude, longitude)
        }
        findViewById<MaterialButton>(R.id.btnCopyLink).setOnClickListener {
            val link = shareLocation.getGoogleMapsLink(latitude, longitude)
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Location Link", link)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Link copied nya~", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayLocation() {
        val coords = shareLocation.getCoordinatesString(latitude, longitude)
        val address = shareLocation.getAddressFromLocation(latitude, longitude)
        val link = shareLocation.getGoogleMapsLink(latitude, longitude)

        tvCoordinates.text = "Coordinates: $coords"
        tvAddress.text = "Address: $address"
        tvMapsLink.text = link
    }
}
