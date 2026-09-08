package com.nekogps.app.features.sharelocation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.location.Geocoder
import java.util.Locale

/**
 * ShareLocation - share current location via SMS, email, or messaging apps.
 * Generates Google Maps link with coordinates and address.
 */
class ShareLocation(private val context: Context) {

    fun getGoogleMapsLink(latitude: Double, longitude: Double): String {
        return "https://maps.google.com/?q=$latitude,$longitude"
    }

    fun getShortLink(latitude: Double, longitude: Double): String {
        return "https://maps.goo.gle/?q=$latitude,$longitude"
    }

    fun getCoordinatesString(latitude: Double, longitude: Double): String {
        return String.format(Locale.US, "%.6f, %.6f", latitude, longitude)
    }

    fun getAddressFromLocation(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()?.getAddressLine(0) ?: "Unknown location"
        } catch (e: Exception) {
            "Unknown location"
        }
    }

    fun getLocationMessage(latitude: Double, longitude: Double): String {
        val address = getAddressFromLocation(latitude, longitude)
        val mapsLink = getGoogleMapsLink(latitude, longitude)
        val coords = getCoordinatesString(latitude, longitude)

        return buildString {
            appendLine("📍 My Location")
            appendLine()
            appendLine("Address: $address")
            appendLine("Coordinates: $coords")
            appendLine()
            appendLine("Open in Maps: $mapsLink")
            appendLine()
            appendLine("Shared via Neko GPS 🐱")
        }
    }

    fun shareViaSms(latitude: Double, longitude: Double) {
        val message = getLocationMessage(latitude, longitude)
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:")
            putExtra("sms_body", message)
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }

    fun shareViaEmail(latitude: Double, longitude: Double) {
        val address = getAddressFromLocation(latitude, longitude)
        val mapsLink = getGoogleMapsLink(latitude, longitude)
        val subject = "My Location"
        val body = getLocationMessage(latitude, longitude)

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }

    fun shareViaMessaging(latitude: Double, longitude: Double) {
        val message = getLocationMessage(latitude, longitude)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            `package` = getMessagingPackage()
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to share sheet
            shareGeneric(latitude, longitude)
        }
    }

    fun shareGeneric(latitude: Double, longitude: Double) {
        val message = getLocationMessage(latitude, longitude)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        val chooser = Intent.createChooser(intent, "Share Location via")
        context.startActivity(chooser)
    }

    fun openInGoogleMaps(latitude: Double, longitude: Double) {
        val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(My+Location)")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback to browser
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(getGoogleMapsLink(latitude, longitude))
            )
            context.startActivity(browserIntent)
        }
    }

    private fun getMessagingPackage(): String? {
        val packages = listOf(
            "com.whatsapp",
            "com.facebook.orca",
            "com.google.android.apps.messaging",
            "org.telegram.messenger",
            "com.discord",
            "com.slack"
        )
        return packages.firstOrNull { isPackageInstalled(it) }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}
