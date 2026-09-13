package com.nekogps.app.utils

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

/**
 * Creates [FusedLocationProviderClient] instances safely.
 *
 * On devices without Google Play Services the constructor throws, which would
 * crash activities at startup. Everything in the app goes through here so a
 * GMS-less device degrades gracefully (location screens simply show no fix)
 * instead of crashing.
 */
object LocationClients {

    /** @return a fused client, or null when Play Services is unavailable. */
    fun fused(context: Context): FusedLocationProviderClient? {
        val gmsAvailable = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
        if (!gmsAvailable) return null
        return try {
            LocationServices.getFusedLocationProviderClient(context)
        } catch (e: RuntimeException) {
            android.util.Log.w("LocationClients", "Fused client unavailable", e)
            null
        }
    }
}
