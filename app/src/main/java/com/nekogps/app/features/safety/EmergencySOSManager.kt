package com.nekogps.app.features.safety

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import com.nekogps.app.utils.DistanceCalculator
import org.osmdroid.util.GeoPoint
import java.util.*

/**
 * Emergency SOS Manager with location sharing and emergency contacts.
 * Sends SMS/call with location to configured emergency contacts.
 * Contacts configurable via SharedPreferences.
 */
class EmergencySOSManager private constructor(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "emergency_sos"
        private const val KEY_CONTACTS = "emergency_contacts"
        private const val KEY_AUTO_SEND_SMS = "auto_send_sms"
        private const val KEY_AUTO_CALL = "auto_call"
        private const val KEY_INCLUDE_LOCATION = "include_location"
        private const val KEY_MESSAGE_TEMPLATE = "message_template"
        private const val DEFAULT_MESSAGE = "EMERGENCY: I need help! My location: {location} - Sent from Neko GPS"

        @Volatile
        private var instance: EmergencySOSManager? = null

        fun getInstance(context: Context): EmergencySOSManager {
            return instance ?: synchronized(this) {
                instance ?: EmergencySOSManager(context).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class EmergencyContact(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        val phoneNumber: String,
        var isPrimary: Boolean = false
    ) {
        override fun toString(): String = "$name ($phoneNumber)${if (isPrimary) " ⭐" else ""}"
    }

    interface EmergencySOSListener {
        fun onSOSStarted()
        fun onSOSCompleted(successCount: Int, totalCount: Int)
        fun onSOSFailed(error: String)
        fun onLocationPrepared(location: Location)
    }

    private val listeners = mutableListOf<EmergencySOSListener>()
    private var currentLocation: Location? = null

    init {
        // Initialize with default empty contacts if none exist
        if (getEmergencyContacts().isEmpty()) {
            // No defaults - user must configure
        }
    }

    fun addListener(listener: EmergencySOSListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: EmergencySOSListener) {
        listeners.remove(listener)
    }

    fun getEmergencyContacts(): List<EmergencyContact> {
        val json = prefs.getString(KEY_CONTACTS, null) ?: return emptyList()
        val type = object : com.google.gson.reflect.TypeToken<List<EmergencyContact>>() {}.type
        return try {
            com.google.gson.Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addEmergencyContact(contact: EmergencyContact) {
        val contacts = getEmergencyContacts().toMutableList()
        // Ensure only one primary contact
        if (contact.isPrimary) {
            contacts.forEach { it.isPrimary = false }
        }
        contacts.add(contact)
        saveContacts(contacts)
    }

    fun updateEmergencyContact(contact: EmergencyContact) {
        val contacts = getEmergencyContacts().toMutableList()
        val index = contacts.indexOfFirst { it.id == contact.id }
        if (index >= 0) {
            // Ensure only one primary contact
            if (contact.isPrimary) {
                contacts.forEach { it.isPrimary = false }
            }
            contacts[index] = contact
            saveContacts(contacts)
        }
    }

    fun removeEmergencyContact(contactId: String) {
        val contacts = getEmergencyContacts().toMutableList()
        contacts.removeAll { it.id == contactId }
        saveContacts(contacts)
    }

    fun setAutoSendSMS(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SEND_SMS, enabled).apply()
    }

    fun getAutoSendSMS(): Boolean = prefs.getBoolean(KEY_AUTO_SEND_SMS, true)

    fun setAutoCall(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CALL, enabled).apply()
    }

    fun getAutoCall(): Boolean = prefs.getBoolean(KEY_AUTO_CALL, false)

    fun setIncludeLocation(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_INCLUDE_LOCATION, enabled).apply()
    }

    fun getIncludeLocation(): Boolean = prefs.getBoolean(KEY_INCLUDE_LOCATION, true)

    fun setMessageTemplate(template: String) {
        prefs.edit().putString(KEY_MESSAGE_TEMPLATE, template).apply()
    }

    fun getMessageTemplate(): String = prefs.getString(KEY_MESSAGE_TEMPLATE, DEFAULT_MESSAGE) ?: DEFAULT_MESSAGE

    fun setCurrentLocation(location: Location) {
        currentLocation = location
        listeners.forEach { it.onLocationPrepared(location) }
    }

    fun getCurrentLocation(): Location? = currentLocation

    /**
     * Trigger emergency SOS - sends SMS and/or calls to all emergency contacts
     */
    fun triggerSOS() {
        listeners.forEach { it.onSOSStarted() }

        val contacts = getEmergencyContacts()
        if (contacts.isEmpty()) {
            listeners.forEach { it.onSOSFailed("No emergency contacts configured") }
            return
        }

        val template = getMessageTemplate()
        val includeLocation = getIncludeLocation()
        val message = buildMessage(template, includeLocation)

        var successCount = 0
        var totalActions = 0

        if (getAutoSendSMS()) {
            totalActions += contacts.size
            sendSMSToContacts(contacts, message) { success ->
                if (success) successCount++
                checkCompletion(successCount, totalActions)
            }
        }

        if (getAutoCall()) {
            totalActions += contacts.size
            callContacts(contacts) { success ->
                if (success) successCount++
                checkCompletion(successCount, totalActions)
            }
        }

        if (totalActions == 0) {
            listeners.forEach { it.onSOSFailed("Neither SMS nor call enabled") }
        }
    }

    private fun buildMessage(template: String, includeLocation: Boolean): String {
        var message = template
        if (includeLocation && currentLocation != null) {
            val lat = currentLocation!!.latitude
            val lng = currentLocation!!.longitude
            val mapsUrl = "https://maps.google.com/?q=$lat,$lng"
            message = message.replace("{location}", "$lat, $lng ($mapsUrl)")
                .replace("{lat}", lat.toString())
                .replace("{lng}", lng.toString())
                .replace("{maps_url}", mapsUrl)
        } else {
            message = message.replace("{location}", "Location unavailable")
                .replace("{lat}", "")
                .replace("{lng}", "")
                .replace("{maps_url}", "")
        }
        return message
    }

    private fun sendSMSToContacts(contacts: List<EmergencyContact>, message: String, callback: (Boolean) -> Unit) {
        val smsManager = SmsManager.getDefault()
        for (contact in contacts) {
            try {
                smsManager.sendTextMessage(contact.phoneNumber, null, message, null, null)
                Log.d("EmergencySOS", "SMS sent to ${contact.name} (${contact.phoneNumber})")
                callback(true)
            } catch (e: Exception) {
                Log.e("EmergencySOS", "Failed to send SMS to ${contact.name}", e)
                callback(false)
            }
        }
    }

    private fun callContacts(contacts: List<EmergencyContact>, callback: (Boolean) -> Unit) {
        // For calling, we'd need to use Intent.ACTION_CALL which requires CALL_PHONE permission
        // This is a placeholder - actual implementation would start call intents
        for (contact in contacts) {
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${contact.phoneNumber}")
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Log.d("EmergencySOS", "Call initiated to ${contact.name} (${contact.phoneNumber})")
                callback(true)
            } catch (e: Exception) {
                Log.e("EmergencySOS", "Failed to call ${contact.name}", e)
                callback(false)
            }
        }
    }

    private fun checkCompletion(successCount: Int, totalCount: Int) {
        if (successCount + (totalCount - successCount) >= totalCount) {
            // All actions completed (success or failure)
            listeners.forEach { it.onSOSCompleted(successCount, totalCount) }
        }
    }

    private fun saveContacts(contacts: List<EmergencyContact>) {
        val json = com.google.gson.Gson().toJson(contacts)
        prefs.edit().putString(KEY_CONTACTS, json).apply()
    }
}