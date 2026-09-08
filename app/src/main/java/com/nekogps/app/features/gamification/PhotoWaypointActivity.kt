package com.nekogps.app.features.gamification

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.nekogps.app.R
import kotlinx.coroutines.launch
import java.io.File

/**
 * PhotoWaypointActivity - allows users to take a photo at a location
 * and attach it to a waypoint or bookmark.
 * Uses Android's camera intent to capture photos.
 */
class PhotoWaypointActivity : AppCompatActivity() {
    private var photoCaptureUri: Uri? = null
    private lateinit var photoWaypointManager: PhotoWaypointManager

    private var targetWaypointId: Long = 0
    private var targetBookmarkId: Long = 0

    companion object {
        const val EXTRA_WAYPOINT_ID = "extra_waypoint_id"
        const val EXTRA_BOOKMARK_ID = "extra_bookmark_id"
        private const val REQUEST_IMAGE_CAPTURE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_waypoint)

        photoWaypointManager = PhotoWaypointManager(this)
        targetWaypointId = intent.getLongExtra(EXTRA_WAYPOINT_ID, 0)
        targetBookmarkId = intent.getLongExtra(EXTRA_BOOKMARK_ID, 0)

        initViews()
    }

    private fun initViews() {
        findViewById<ImageView>(R.id.ivCapturePreview).setOnClickListener {
            dispatchTakePictureIntent()
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCapturePhoto).setOnClickListener {
            dispatchTakePictureIntent()
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSavePhoto).setOnClickListener {
            savePhoto()
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnUseCamera).setOnClickListener {
            dispatchTakePictureIntent()
        }
    }

    private fun dispatchTakePictureIntent() {
        val captureIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        if (captureIntent.resolveActivity(packageManager) != null) {
            val photoFile = createImageFile()
            photoFile?.let {
                photoCaptureUri = Uri.fromFile(it)
                captureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoCaptureUri)
                startActivityForResult(captureIntent, REQUEST_IMAGE_CAPTURE)
            }
        } else {
            // Fallback to picking from gallery
            val pickIntent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(pickIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    private fun createImageFile(): File? {
        return try {
            val storageDir = getExternalFilesDir("photos")
            val timeStamp = System.currentTimeMillis()
            val image = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
            image
        } catch (e: Exception) {
            Toast.makeText(this, "Error creating file: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageView = findViewById<ImageView>(R.id.ivCapturePreview)
            if (photoCaptureUri != null) {
                imageView.setImageURI(photoCaptureUri)
                Toast.makeText(this, "Photo captured!", Toast.LENGTH_SHORT).show()
            } else {
                data?.data?.let { uri ->
                    imageView.setImageURI(uri)
                    photoCaptureUri = uri
                    Toast.makeText(this, "Image selected!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun savePhoto() {
        val caption = findViewById<EditText>(R.id.etCaption).text?.toString()?.trim() ?: ""
        val photoPath = photoCaptureUri?.path ?: run {
            Toast.makeText(this, "No photo captured", Toast.LENGTH_SHORT).show()
            return
        }

        // Get lat/lon from intent or default to 0
        val lat = intent.getDoubleExtra("lat", 0.0)
        val lon = intent.getDoubleExtra("lon", 0.0)

        lifecycleScope.launch {
            val id = photoWaypointManager.addPhotoWaypoint(
                waypointId = targetWaypointId,
                bookmarkId = targetBookmarkId,
                photoPath = photoPath,
                latitude = lat,
                longitude = lon,
                caption = caption
            )
            if (targetWaypointId > 0) {
                Toast.makeText(this@PhotoWaypointActivity, "Photo saved to waypoint!", Toast.LENGTH_SHORT).show()
            } else if (targetBookmarkId > 0) {
                Toast.makeText(this@PhotoWaypointActivity, "Photo saved to bookmark!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@PhotoWaypointActivity, "Photo saved!", Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }
}
