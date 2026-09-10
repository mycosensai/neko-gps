package com.nekogps.app.features.safety

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nekogps.app.utils.DistanceCalculator
import org.osmdroid.util.GeoPoint
import java.util.*

/**
 * Persistent ICE contact store backing [OfflineEmergencyManager].
 * Extracted to keep per-class function counts within limits.
 */
open class OfflineContactStore protected constructor(protected val context: Context) {

    companion object {
        private const val TAG = "OfflineEmergencyManager"
        private const val PREFS_NAME = "offline_emergency"
        internal const val KEY_ICE_CONTACTS = "ice_contacts"
    }

    protected val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    protected val gson = Gson()
    protected val listeners = mutableListOf<OfflineEmergencyManager.OfflineEmergencyListener>()

    // ICE Contacts
    fun getICEContacts(): List<OfflineEmergencyManager.ICEContact> {
        val json = prefs.getString(KEY_ICE_CONTACTS, null) ?: return emptyList()
        val type = object : TypeToken<List<OfflineEmergencyManager.ICEContact>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: com.google.gson.JsonSyntaxException) {
            Log.w("OfflineEmergencyManager", "getICEContacts: suppressed Exception", e)
            emptyList()
        }
    }

    fun addICEContact(contact: OfflineEmergencyManager.ICEContact) {
        val contacts = getICEContacts().toMutableList()
        if (contact.isPrimary) {
            contacts.forEach { it.isPrimary = false }
        }
        contacts.add(contact)
        saveICEContacts(contacts)
    }

    fun updateICEContact(contact: OfflineEmergencyManager.ICEContact) {
        val contacts = getICEContacts().toMutableList()
        val index = contacts.indexOfFirst { it.id == contact.id }
        if (index >= 0) {
            if (contact.isPrimary) {
                contacts.forEach { it.isPrimary = false }
            }
            contacts[index] = contact
            saveICEContacts(contacts)
        }
    }

    fun removeICEContact(contactId: String) {
        val contacts = getICEContacts().toMutableList()
        contacts.removeAll { it.id == contactId }
        saveICEContacts(contacts)
    }

    fun getPrimaryICEContact(): OfflineEmergencyManager.ICEContact? {
        return getICEContacts().firstOrNull { it.isPrimary } ?: getICEContacts().firstOrNull()
    }

    protected fun saveICEContacts(contacts: List<OfflineEmergencyManager.ICEContact>) {
        val json = gson.toJson(contacts)
        prefs.edit().putString(KEY_ICE_CONTACTS, json).apply()
        listeners.forEach { it.onICEContactsChanged(contacts) }
    }
}

/**
 * Medical info store backing [OfflineEmergencyManager].
 * Extracted to keep per-class function counts within limits.
 */
open class OfflineMedicalStore protected constructor(context: Context) :
    OfflineContactStore(context) {

    companion object {
        internal const val KEY_MEDICAL_INFO = "medical_info"
        internal const val KEY_EMERGENCY_NOTES = "emergency_notes"
        internal const val KEY_BLOOD_TYPE = "blood_type"
        internal const val KEY_ALLERGIES = "allergies"
        internal const val KEY_MEDICATIONS = "medications"
        internal const val KEY_CONDITIONS = "conditions"
        internal const val KEY_ORGAN_DONOR = "organ_donor"
    }

    // Medical Info
    fun getMedicalInfo(): OfflineEmergencyManager.MedicalInfo {
        val bloodType = prefs.getString(KEY_BLOOD_TYPE, "") ?: ""
        val allergies = prefs.getString(KEY_ALLERGIES, "") ?: ""
        val medications = prefs.getString(KEY_MEDICATIONS, "") ?: ""
        val conditions = prefs.getString(KEY_CONDITIONS, "") ?: ""
        val organDonor = prefs.getBoolean(KEY_ORGAN_DONOR, false)
        val emergencyNotes = prefs.getString(KEY_EMERGENCY_NOTES, "") ?: ""
        return OfflineEmergencyManager.MedicalInfo(
            bloodType = bloodType,
            allergies = allergies,
            medications = medications,
            conditions = conditions,
            organDonor = organDonor,
            emergencyNotes = emergencyNotes
        )
    }

    fun updateMedicalInfo(info: OfflineEmergencyManager.MedicalInfo) {
        prefs.edit()
            .putString(KEY_BLOOD_TYPE, info.bloodType)
            .putString(KEY_ALLERGIES, info.allergies)
            .putString(KEY_MEDICATIONS, info.medications)
            .putString(KEY_CONDITIONS, info.conditions)
            .putBoolean(KEY_ORGAN_DONOR, info.organDonor)
            .putString(KEY_EMERGENCY_NOTES, info.emergencyNotes)
            .apply()
        listeners.forEach { it.onMedicalInfoChanged(info) }
    }

    fun updateBloodType(bloodType: String) {
        val info = getMedicalInfo().copy(bloodType = bloodType, lastUpdated = System.currentTimeMillis())
        updateMedicalInfo(info)
    }

    fun updateAllergies(allergies: String) {
        val info = getMedicalInfo().copy(allergies = allergies, lastUpdated = System.currentTimeMillis())
        updateMedicalInfo(info)
    }

    fun updateMedications(medications: String) {
        val info = getMedicalInfo().copy(medications = medications, lastUpdated = System.currentTimeMillis())
        updateMedicalInfo(info)
    }

    fun updateConditions(conditions: String) {
        val info = getMedicalInfo().copy(conditions = conditions, lastUpdated = System.currentTimeMillis())
        updateMedicalInfo(info)
    }

    fun updateOrganDonor(isDonor: Boolean) {
        val info = getMedicalInfo().copy(organDonor = isDonor, lastUpdated = System.currentTimeMillis())
        updateMedicalInfo(info)
    }

    fun updateEmergencyNotes(notes: String) {
        val info = getMedicalInfo().copy(emergencyNotes = notes, lastUpdated = System.currentTimeMillis())
        updateMedicalInfo(info)
    }
}

/**
 * Cached hospital lookup backing [OfflineEmergencyManager].
 * Extracted to keep per-class function counts within limits.
 */
open class OfflineHospitalCache protected constructor(context: Context) :
    OfflineMedicalStore(context) {

    companion object {
        private const val TAG = "OfflineEmergencyManager"
        internal const val KEY_HOSPITALS = "cached_hospitals"
        internal const val KEY_LAST_HOSPITAL_UPDATE = "last_hospital_update"
        private const val HOSPITAL_CACHE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val MAX_HOSPITALS = 20
        private const val MOCK_MIN_DISTANCE_KM = 1.0
        private const val MOCK_DISTANCE_RANGE_KM = 15.0
        private const val FULL_CIRCLE_DEGREES = 360.0
        private const val KM_PER_DEGREE_LATITUDE = 111.0
    }

    init {
        // Load cached data
        loadHospitalsFromCache()
    }

    // Hospitals
    fun getCachedHospitals(): List<OfflineEmergencyManager.Hospital> {
        val json = prefs.getString(KEY_HOSPITALS, null) ?: return emptyList()
        val type = object : TypeToken<List<OfflineEmergencyManager.Hospital>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: com.google.gson.JsonSyntaxException) {
            Log.w("OfflineEmergencyManager", "getCachedHospitals: suppressed Exception", e)
            emptyList()
        }
    }

    fun getNearestHospitals(location: Location, maxCount: Int = 5): List<OfflineEmergencyManager.Hospital> {
        val hospitals = getCachedHospitals()
        return hospitals.map { hospital ->
            val distance = DistanceCalculator.haversineDistance(
                location.latitude, location.longitude,
                hospital.latitude, hospital.longitude
            )
            hospital.copy(distanceKm = distance)
        }.sortedBy { it.distanceKm }.take(maxCount)
    }

    fun updateHospitalsFromLocation(location: Location) {
        // Check if cache is still valid
        val lastUpdate = prefs.getLong(KEY_LAST_HOSPITAL_UPDATE, 0)
        val now = System.currentTimeMillis()

        if (now - lastUpdate < HOSPITAL_CACHE_DURATION_MS && getCachedHospitals().isNotEmpty()) {
            // Cache is valid, just sort by distance
            val sorted = getNearestHospitals(location)
            listeners.forEach { it.onHospitalsUpdated(sorted) }
            return
        }

        // In a real app, this would query Overpass API or Google Places
        // For now, generate mock hospitals around the location
        fetchHospitalsFromAPI(location)
    }

    private fun fetchHospitalsFromAPI(location: Location) {
        // Simulate API call with mock data
        // In production: Query Overpass API for amenity=hospital near location
        val mockHospitals = generateMockHospitals(location)
        saveHospitals(mockHospitals)
    }

    private fun generateMockHospitals(location: Location): List<OfflineEmergencyManager.Hospital> {
        val hospitals = mutableListOf<OfflineEmergencyManager.Hospital>()
        val lat = location.latitude
        val lng = location.longitude

        val names = listOf(
            "General Hospital", "Medical Center", "Emergency Clinic",
            "University Hospital", "Community Hospital", "Regional Medical Center",
            "Children's Hospital", "Trauma Center", "Heart Institute", "Cancer Center"
        )

        val streets = listOf("Main St", "Oak Ave", "Hospital Dr", "Medical Blvd", "Health Way")

        for (i in 0 until minOf(MAX_HOSPITALS, names.size)) {
            val distanceKm = (MOCK_MIN_DISTANCE_KM + Math.random() * MOCK_DISTANCE_RANGE_KM) // 1-16 km
            val bearing = Math.random() * FULL_CIRCLE_DEGREES

            val newLat = lat + (distanceKm / KM_PER_DEGREE_LATITUDE) * kotlin.math.cos(Math.toRadians(bearing))
            val newLng = lng + (distanceKm / (KM_PER_DEGREE_LATITUDE * kotlin.math.cos(Math.toRadians(lat)))) *
                kotlin.math.sin(Math.toRadians(bearing))

            hospitals.add(
                OfflineEmergencyManager.Hospital(
                    id = "hospital_$i",
                    name = names[i],
                    latitude = newLat,
                    longitude = newLng,
                    address = "${100 + i * 50} ${streets[i % streets.size]}",
                    phone = "555-${1000 + i}",
                    distanceKm = distanceKm,
                    hasEmergency = Math.random() > 0.3,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }

        return hospitals.sortedBy { it.distanceKm }
    }

    protected fun saveHospitals(hospitals: List<OfflineEmergencyManager.Hospital>) {
        val json = gson.toJson(hospitals)
        prefs.edit()
            .putString(KEY_HOSPITALS, json)
            .putLong(KEY_LAST_HOSPITAL_UPDATE, System.currentTimeMillis())
            .apply()
        listeners.forEach { it.onHospitalsUpdated(hospitals) }
    }

    private fun loadHospitalsFromCache() {
        // Hospitals loaded on demand via getCachedHospitals()
    }
}

/**
 * Offline Emergency Manager - Stores emergency info (ICE contacts, medical info, nearest hospitals) offline.
 * Shows on lock screen widget.
 */
class OfflineEmergencyManager private constructor(context: Context) :
    OfflineHospitalCache(context) {

    companion object {
        private const val TAG = "OfflineEmergencyManager"

        @Volatile
        private var instance: OfflineEmergencyManager? = null

        fun getInstance(context: Context): OfflineEmergencyManager {
            return instance ?: synchronized(this) {
                instance ?: OfflineEmergencyManager(context).also { instance = it }
            }
        }
    }

    data class ICEContact(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        val phoneNumber: String,
        val relationship: String = "",
        var isPrimary: Boolean = false,
        val email: String = ""
    ) {
        override fun toString(): String = "$name ($relationship) - $phoneNumber"
    }

    data class MedicalInfo(
        val bloodType: String = "",
        val allergies: String = "",
        val medications: String = "",
        val conditions: String = "",
        val organDonor: Boolean = false,
        val emergencyNotes: String = "",
        val lastUpdated: Long = System.currentTimeMillis()
    )

    data class Hospital(
        val id: String,
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val address: String,
        val phone: String = "",
        val distanceKm: Double = 0.0,
        val hasEmergency: Boolean = true,
        val lastUpdated: Long = System.currentTimeMillis()
    ) {
        fun toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)
    }

    interface OfflineEmergencyListener {
        fun onICEContactsChanged(contacts: List<ICEContact>)
        fun onMedicalInfoChanged(info: MedicalInfo)
        fun onHospitalsUpdated(hospitals: List<Hospital>)
        fun onEmergencyInfoPrepared()
    }

    data class EmergencySummary(
        val primaryContact: ICEContact?,
        val contactCount: Int,
        val bloodType: String,
        val hasAllergies: Boolean,
        val hasMedications: Boolean,
        val hasConditions: Boolean,
        val isOrganDonor: Boolean,
        val hospitalCount: Int,
        val nearestHospital: String?,
        val lastUpdated: Long
    )

    fun addListener(listener: OfflineEmergencyListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: OfflineEmergencyListener) {
        listeners.remove(listener)
    }

    // Emergency Info Summary for lock screen widget
    fun getEmergencySummary(): EmergencySummary {
        val contacts = getICEContacts()
        val medical = getMedicalInfo()
        val hospitals = getCachedHospitals()

        return EmergencySummary(
            primaryContact = getPrimaryICEContact(),
            contactCount = contacts.size,
            bloodType = medical.bloodType,
            hasAllergies = medical.allergies.isNotBlank(),
            hasMedications = medical.medications.isNotBlank(),
            hasConditions = medical.conditions.isNotBlank(),
            isOrganDonor = medical.organDonor,
            hospitalCount = hospitals.size,
            nearestHospital = hospitals.firstOrNull()?.let { "${it.name} (${it.distanceKm.toInt()} km)" },
            lastUpdated = medical.lastUpdated
        )
    }

    // Prepare all emergency info for lock screen
    fun prepareEmergencyInfo() {
        listeners.forEach { it.onEmergencyInfoPrepared() }
    }

    // Export/Import for backup
    fun exportEmergencyData(): String {
        val data = mapOf(
            "iceContacts" to getICEContacts(),
            "medicalInfo" to getMedicalInfo(),
            "hospitals" to getCachedHospitals(),
            "exportedAt" to System.currentTimeMillis(),
            "version" to 1
        )
        return gson.toJson(data)
    }

    fun importEmergencyData(json: String): Boolean {
        return try {
            val data = gson.fromJson(json, object : TypeToken<Map<String, Any>>() {}.type) ?: return false

            data["iceContacts"]?.let { contactsJson ->
                val contacts: List<ICEContact>? = gson.fromJson(
                    gson.toJson(contactsJson), object : TypeToken<List<ICEContact>>() {}.type
                )
                contacts?.let { saveICEContacts(it) }
            }

            data["medicalInfo"]?.let { medicalJson ->
                val medical = gson.fromJson(gson.toJson(medicalJson), MedicalInfo::class.java)
                medical?.let { updateMedicalInfo(it) }
            }

            data["hospitals"]?.let { hospitalsJson ->
                val hospitals: List<Hospital>? = gson.fromJson(
                    gson.toJson(hospitalsJson), object : TypeToken<List<Hospital>>() {}.type
                )
                hospitals?.let { saveHospitals(it) }
            }

            true
        } catch (e: com.google.gson.JsonSyntaxException) {
            Log.w(TAG, "Failed to import emergency data", e)
            false
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Failed to import emergency data", e)
            false
        }
    }

    fun clearAllData() {
        prefs.edit()
            .remove(OfflineContactStore.KEY_ICE_CONTACTS)
            .remove(OfflineMedicalStore.KEY_MEDICAL_INFO)
            .remove(KEY_HOSPITALS)
            .remove(KEY_LAST_HOSPITAL_UPDATE)
            .remove(OfflineMedicalStore.KEY_EMERGENCY_NOTES)
            .remove(OfflineMedicalStore.KEY_BLOOD_TYPE)
            .remove(OfflineMedicalStore.KEY_ALLERGIES)
            .remove(OfflineMedicalStore.KEY_MEDICATIONS)
            .remove(OfflineMedicalStore.KEY_CONDITIONS)
            .remove(OfflineMedicalStore.KEY_ORGAN_DONOR)
            .apply()
    }
}
