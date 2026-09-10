package com.nekogps.app.features.safety

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.nekogps.app.R
import com.nekogps.app.features.safety.CrashDetectionManager.CrashDetectionListener
import com.nekogps.app.features.safety.EmergencySOSManager.EmergencyContact
import com.nekogps.app.features.safety.EmergencySOSManager.EmergencySOSListener
import com.nekogps.app.features.safety.FatigueDetectionManager.FatigueDetectionListener
import com.nekogps.app.features.safety.FatigueDetectionManager.RestStop
import com.nekogps.app.features.safety.OfflineEmergencyManager.ICEContact
import com.nekogps.app.features.safety.OfflineEmergencyManager.MedicalInfo
import com.nekogps.app.features.safety.SpeedWarningManager.SpeedWarningListener
import org.osmdroid.util.GeoPoint

/**
 * Main Safety Activity - Central hub for all safety features.
 */
class SafetyActivity : AppCompatActivity() {

    private lateinit var emergencySOSManager: EmergencySOSManager
    private lateinit var crashDetectionManager: CrashDetectionManager
    private lateinit var speedWarningManager: SpeedWarningManager
    private lateinit var fatigueDetectionManager: FatigueDetectionManager
    private lateinit var offlineEmergencyManager: OfflineEmergencyManager

    // UI Elements
    private lateinit var toolbar: Toolbar
    private lateinit var sosButton: MaterialButton
    private lateinit var sosStatusText: TextView

    // Emergency SOS
    private lateinit var contactsContainer: LinearLayout
    private lateinit var addContactButton: MaterialButton
    private lateinit var autoSmsSwitch: SwitchMaterial
    private lateinit var autoCallSwitch: SwitchMaterial
    private lateinit var includeLocationSwitch: SwitchMaterial

    // Crash Detection
    private lateinit var crashMonitorSwitch: SwitchMaterial
    private lateinit var crashStatusText: TextView

    // Speed Warning
    private lateinit var speedWarningSwitch: SwitchMaterial
    private lateinit var visualWarningSwitch: SwitchMaterial
    private lateinit var audioWarningSwitch: SwitchMaterial
    private lateinit var voiceWarningSwitch: SwitchMaterial
    private lateinit var thresholdSeekBar: SeekBar
    private lateinit var thresholdText: TextView

    // Fatigue Detection
    private lateinit var fatigueMonitorSwitch: SwitchMaterial
    private lateinit var drivingTimeText: TextView
    private lateinit var breakIntervalSeekBar: SeekBar
    private lateinit var breakIntervalText: TextView
    private lateinit var restStopsContainer: LinearLayout

    // Offline Emergency
    private lateinit var iceContactsContainer: LinearLayout
    private lateinit var addIceContactButton: MaterialButton
    private lateinit var bloodTypeEditText: TextInputEditText
    private lateinit var allergiesEditText: TextInputEditText
    private lateinit var medicationsEditText: TextInputEditText
    private lateinit var conditionsEditText: TextInputEditText
    private lateinit var organDonorSwitch: SwitchMaterial
    private lateinit var emergencyNotesEditText: TextInputEditText

    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safety)

        initializeManagers()
        initializeViews()
        setupToolbar()
        setupListeners()
        loadSettings()
        startPeriodicUpdates()
    }

    private fun initializeManagers() {
        emergencySOSManager = EmergencySOSManager.getInstance(this)
        crashDetectionManager = CrashDetectionManager.getInstance(this)
        speedWarningManager = SpeedWarningManager.getInstance(this)
        fatigueDetectionManager = FatigueDetectionManager.getInstance(this)
        offlineEmergencyManager = OfflineEmergencyManager.getInstance(this)
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        sosButton = findViewById(R.id.btn_sos)
        sosStatusText = findViewById(R.id.tv_sos_status)

        contactsContainer = findViewById(R.id.contacts_container)
        addContactButton = findViewById(R.id.btn_add_contact)
        autoSmsSwitch = findViewById(R.id.switch_auto_sms)
        autoCallSwitch = findViewById(R.id.switch_auto_call)
        includeLocationSwitch = findViewById(R.id.switch_include_location)

        crashMonitorSwitch = findViewById(R.id.switch_crash_monitor)
        crashStatusText = findViewById(R.id.tv_crash_status)

        speedWarningSwitch = findViewById(R.id.switch_speed_warning)
        visualWarningSwitch = findViewById(R.id.switch_visual_warning)
        audioWarningSwitch = findViewById(R.id.switch_audio_warning)
        voiceWarningSwitch = findViewById(R.id.switch_voice_warning)
        thresholdSeekBar = findViewById(R.id.seekbar_threshold)
        thresholdText = findViewById(R.id.tv_threshold)

        fatigueMonitorSwitch = findViewById(R.id.switch_fatigue_monitor)
        drivingTimeText = findViewById(R.id.tv_driving_time)
        breakIntervalSeekBar = findViewById(R.id.seekbar_break_interval)
        breakIntervalText = findViewById(R.id.tv_break_interval)
        restStopsContainer = findViewById(R.id.rest_stops_container)

        iceContactsContainer = findViewById(R.id.ice_contacts_container)
        addIceContactButton = findViewById(R.id.btn_add_ice_contact)
        bloodTypeEditText = findViewById(R.id.et_blood_type)
        allergiesEditText = findViewById(R.id.et_allergies)
        medicationsEditText = findViewById(R.id.et_medications)
        conditionsEditText = findViewById(R.id.et_conditions)
        organDonorSwitch = findViewById(R.id.switch_organ_donor)
        emergencyNotesEditText = findViewById(R.id.et_emergency_notes)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.safety_features)
    }

    private fun setupListeners() {
        // SOS Button
        sosButton.setOnClickListener {
            showSOSConfirmationDialog()
        }

        // Emergency SOS
        addContactButton.setOnClickListener { showAddContactDialog() }
        autoSmsSwitch.setOnCheckedChangeListener { _, checked ->
            emergencySOSManager.setAutoSendSMS(checked)
        }
        autoCallSwitch.setOnCheckedChangeListener { _, checked ->
            emergencySOSManager.setAutoCall(checked)
        }
        includeLocationSwitch.setOnCheckedChangeListener { _, checked ->
            emergencySOSManager.setIncludeLocation(checked)
        }

        // Crash Detection
        crashMonitorSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                crashDetectionManager.startMonitoring()
            } else {
                crashDetectionManager.stopMonitoring()
            }
        }

        // Speed Warning
        speedWarningSwitch.setOnCheckedChangeListener { _, checked ->
            speedWarningManager.setEnabled(checked)
        }
        visualWarningSwitch.setOnCheckedChangeListener { _, checked ->
            speedWarningManager.setVisualWarningEnabled(checked)
        }
        audioWarningSwitch.setOnCheckedChangeListener { _, checked ->
            speedWarningManager.setAudioWarningEnabled(checked)
        }
        voiceWarningSwitch.setOnCheckedChangeListener { _, checked ->
            speedWarningManager.setVoiceWarningEnabled(checked)
        }
        thresholdSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    speedWarningManager.setThresholdKmh(progress)
                    thresholdText.text = getString(R.string.speed_warning_threshold, progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Fatigue Detection
        fatigueMonitorSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                fatigueDetectionManager.startMonitoring()
            } else {
                fatigueDetectionManager.stopMonitoring()
            }
        }
        breakIntervalSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val minutes = progress + 30 // 30-480 min
                    fatigueDetectionManager.setBreakIntervalMinutes(minutes)
                    breakIntervalText.text = getString(R.string.fatigue_break_interval, minutes)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Offline Emergency
        addIceContactButton.setOnClickListener { showAddICEContactDialog() }
        bloodTypeEditText?.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) updateMedicalInfo()
        }
        allergiesEditText?.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) updateMedicalInfo()
        }
        medicationsEditText?.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) updateMedicalInfo()
        }
        conditionsEditText?.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) updateMedicalInfo()
        }
        organDonorSwitch.setOnCheckedChangeListener { _, checked ->
            offlineEmergencyManager.updateOrganDonor(checked)
        }
        emergencyNotesEditText?.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) updateMedicalInfo()
        }
    }

    private fun loadSettings() {
        // Emergency SOS
        autoSmsSwitch.isChecked = emergencySOSManager.getAutoSendSMS()
        autoCallSwitch.isChecked = emergencySOSManager.getAutoCall()
        includeLocationSwitch.isChecked = emergencySOSManager.getIncludeLocation()
        updateContactsList()

        // Crash Detection
        crashMonitorSwitch.isChecked = crashDetectionManager.isMonitoring()
        updateCrashStatus()

        // Speed Warning
        speedWarningSwitch.isChecked = speedWarningManager.isEnabled()
        visualWarningSwitch.isChecked = speedWarningManager.getVisualWarningEnabled()
        audioWarningSwitch.isChecked = speedWarningManager.getAudioWarningEnabled()
        voiceWarningSwitch.isChecked = speedWarningManager.getVoiceWarningEnabled()
        thresholdSeekBar.progress = speedWarningManager.getThresholdKmh()
        thresholdText.text = getString(R.string.speed_warning_threshold, speedWarningManager.getThresholdKmh())

        // Fatigue Detection
        fatigueMonitorSwitch.isChecked = fatigueDetectionManager.isMonitoring()
        updateDrivingTimeDisplay()
        val breakInterval = fatigueDetectionManager.getBreakIntervalMinutes()
        breakIntervalSeekBar.progress = (breakInterval - 30).coerceIn(0, 450)
        breakIntervalText.text = getString(R.string.fatigue_break_interval, breakInterval)

        // Offline Emergency
        updateICEContactsList()
        val medical = offlineEmergencyManager.getMedicalInfo()
        bloodTypeEditText?.setText(medical.bloodType)
        allergiesEditText?.setText(medical.allergies)
        medicationsEditText?.setText(medical.medications)
        conditionsEditText?.setText(medical.conditions)
        organDonorSwitch.isChecked = medical.organDonor
        emergencyNotesEditText?.setText(medical.emergencyNotes)
    }

    private fun startPeriodicUpdates() {
        updateRunnable = object : Runnable {
            override fun run() {
                updateDrivingTimeDisplay()
                updateCrashStatus()
                updateSOSStatus()
                handler.postDelayed(this, 5000)
            }
        }
        handler.post(updateRunnable!!)
    }

    private fun updateDrivingTimeDisplay() {
        val minutes = fatigueDetectionManager.getTotalDrivingMinutes()
        val hours = minutes / 60
        val mins = minutes % 60
        drivingTimeText.text = if (hours > 0) {
            getString(R.string.fatigue_driving_time_hours, hours, mins)
        } else {
            getString(R.string.fatigue_driving_time_minutes, mins)
        }
    }

    private fun updateCrashStatus() {
        if (crashDetectionManager.isMonitoring()) {
            crashStatusText.text = getString(R.string.crash_detection_active)
            crashStatusText.setTextColor(ContextCompat.getColor(this, R.color.success_green))
        } else {
            crashStatusText.text = getString(R.string.crash_detection_inactive)
            crashStatusText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        }
    }

    private fun updateSOSStatus() {
        val contacts = emergencySOSManager.getEmergencyContacts()
        sosStatusText.text = getString(R.string.sos_contacts_configured, contacts.size)
    }

    private fun updateContactsList() {
        contactsContainer.removeAllViews()
        val contacts = emergencySOSManager.getEmergencyContacts()

        if (contacts.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = getString(R.string.no_emergency_contacts)
                setTextColor(ContextCompat.getColor(this@SafetyActivity, R.color.text_secondary))
                setPadding(0, 32, 0, 32)
                gravity = android.view.Gravity.CENTER
            }
            contactsContainer.addView(emptyText)
            return
        }

        for (contact in contacts) {
            val itemView = layoutInflater.inflate(R.layout.item_emergency_contact, contactsContainer, false)
            itemView.findViewById<TextView>(R.id.tv_contact_name).text = contact.name
            itemView.findViewById<TextView>(R.id.tv_contact_phone).text = contact.phoneNumber
            itemView.findViewById<TextView>(R.id.tv_contact_primary).visibility = if (contact.isPrimary) View.VISIBLE else View.GONE

            itemView.findViewById<MaterialButton>(R.id.btn_edit_contact).setOnClickListener {
                showEditContactDialog(contact)
            }
            itemView.findViewById<MaterialButton>(R.id.btn_delete_contact).setOnClickListener {
                emergencySOSManager.removeEmergencyContact(contact.id)
                updateContactsList()
            }

            contactsContainer.addView(itemView)
        }
    }

    private fun updateICEContactsList() {
        iceContactsContainer.removeAllViews()
        val contacts = offlineEmergencyManager.getICEContacts()

        if (contacts.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = getString(R.string.no_ice_contacts)
                setTextColor(ContextCompat.getColor(this@SafetyActivity, R.color.text_secondary))
                setPadding(0, 32, 0, 32)
                gravity = android.view.Gravity.CENTER
            }
            iceContactsContainer.addView(emptyText)
            return
        }

        for (contact in contacts) {
            val itemView = layoutInflater.inflate(R.layout.item_ice_contact, iceContactsContainer, false)
            itemView.findViewById<TextView>(R.id.tv_ice_name).text = contact.name
            itemView.findViewById<TextView>(R.id.tv_ice_phone).text = contact.phoneNumber
            itemView.findViewById<TextView>(R.id.tv_ice_relationship).text = contact.relationship
            itemView.findViewById<TextView>(R.id.tv_ice_primary).visibility = if (contact.isPrimary) View.VISIBLE else View.GONE

            itemView.findViewById<MaterialButton>(R.id.btn_edit_ice).setOnClickListener {
                showEditICEContactDialog(contact)
            }
            itemView.findViewById<MaterialButton>(R.id.btn_delete_ice).setOnClickListener {
                offlineEmergencyManager.removeICEContact(contact.id)
                updateICEContactsList()
            }

            iceContactsContainer.addView(itemView)
        }
    }

    private fun showSOSConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.emergency_sos)
            .setMessage(R.string.sos_confirm_message)
            .setIcon(R.drawable.ic_emergency)
            .setPositiveButton(R.string.send_sos) { _, _ ->
                emergencySOSManager.triggerSOS()
                showSOSFeedbackDialog()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showSOSFeedbackDialog() {
        val contacts = emergencySOSManager.getEmergencyContacts()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sos_sent)
            .setMessage(getString(R.string.sos_sent_to_contacts, contacts.size))
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun showAddContactDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_emergency_contact, null)
        val nameEdit = view.findViewById<TextInputEditText>(R.id.et_contact_name)
        val phoneEdit = view.findViewById<TextInputEditText>(R.id.et_contact_phone)
        val primaryCheck = view.findViewById<SwitchMaterial>(R.id.switch_primary_contact)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_emergency_contact)
            .setView(view)
            .setPositiveButton(R.string.add) { _, _ ->
                val name = nameEdit.text.toString().trim()
                val phone = phoneEdit.text.toString().trim()
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    val contact = EmergencyContact(name = name, phoneNumber = phone, isPrimary = primaryCheck.isChecked)
                    emergencySOSManager.addEmergencyContact(contact)
                    updateContactsList()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEditContactDialog(contact: EmergencyContact) {
        val view = layoutInflater.inflate(R.layout.dialog_add_emergency_contact, null)
        val nameEdit = view.findViewById<TextInputEditText>(R.id.et_contact_name)
        val phoneEdit = view.findViewById<TextInputEditText>(R.id.et_contact_phone)
        val primaryCheck = view.findViewById<SwitchMaterial>(R.id.switch_primary_contact)

        nameEdit.setText(contact.name)
        phoneEdit.setText(contact.phoneNumber)
        primaryCheck.isChecked = contact.isPrimary

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.edit_emergency_contact)
            .setView(view)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = nameEdit.text.toString().trim()
                val phone = phoneEdit.text.toString().trim()
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    val updated = contact.copy(name = name, phoneNumber = phone, isPrimary = primaryCheck.isChecked)
                    emergencySOSManager.updateEmergencyContact(updated)
                    updateContactsList()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showAddICEContactDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_ice_contact, null)
        val nameEdit = view.findViewById<TextInputEditText>(R.id.et_ice_name)
        val phoneEdit = view.findViewById<TextInputEditText>(R.id.et_ice_phone)
        val relationshipEdit = view.findViewById<TextInputEditText>(R.id.et_ice_relationship)
        val emailEdit = view.findViewById<TextInputEditText>(R.id.et_ice_email)
        val primaryCheck = view.findViewById<SwitchMaterial>(R.id.switch_ice_primary)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_ice_contact)
            .setView(view)
            .setPositiveButton(R.string.add) { _, _ ->
                val name = nameEdit.text.toString().trim()
                val phone = phoneEdit.text.toString().trim()
                val relationship = relationshipEdit.text.toString().trim()
                val email = emailEdit.text.toString().trim()
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    val contact = ICEContact(name = name, phoneNumber = phone, relationship = relationship, isPrimary = primaryCheck.isChecked, email = email)
                    offlineEmergencyManager.addICEContact(contact)
                    updateICEContactsList()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEditICEContactDialog(contact: ICEContact) {
        val view = layoutInflater.inflate(R.layout.dialog_add_ice_contact, null)
        val nameEdit = view.findViewById<TextInputEditText>(R.id.et_ice_name)
        val phoneEdit = view.findViewById<TextInputEditText>(R.id.et_ice_phone)
        val relationshipEdit = view.findViewById<TextInputEditText>(R.id.et_ice_relationship)
        val emailEdit = view.findViewById<TextInputEditText>(R.id.et_ice_email)
        val primaryCheck = view.findViewById<SwitchMaterial>(R.id.switch_ice_primary)

        nameEdit.setText(contact.name)
        phoneEdit.setText(contact.phoneNumber)
        relationshipEdit.setText(contact.relationship)
        emailEdit.setText(contact.email)
        primaryCheck.isChecked = contact.isPrimary

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.edit_ice_contact)
            .setView(view)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = nameEdit.text.toString().trim()
                val phone = phoneEdit.text.toString().trim()
                val relationship = relationshipEdit.text.toString().trim()
                val email = emailEdit.text.toString().trim()
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    val updated = contact.copy(name = name, phoneNumber = phone, relationship = relationship, isPrimary = primaryCheck.isChecked, email = email)
                    offlineEmergencyManager.updateICEContact(updated)
                    updateICEContactsList()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateMedicalInfo() {
        val medical = offlineEmergencyManager.getMedicalInfo().copy(
            bloodType = bloodTypeEditText?.text.toString() ?: "",
            allergies = allergiesEditText?.text.toString() ?: "",
            medications = medicationsEditText?.text.toString() ?: "",
            conditions = conditionsEditText?.text.toString() ?: "",
            emergencyNotes = emergencyNotesEditText?.text.toString() ?: "",
            lastUpdated = System.currentTimeMillis()
        )
        offlineEmergencyManager.updateMedicalInfo(medical)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_safety, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_test_sos -> {
                showSOSConfirmationDialog()
                true
            }
            R.id.action_export_emergency -> {
                exportEmergencyData()
                true
            }
            R.id.action_import_emergency -> {
                importEmergencyData()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun exportEmergencyData() {
        val json = offlineEmergencyManager.exportEmergencyData()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_TEXT, json)
            putExtra(Intent.EXTRA_SUBJECT, "Neko GPS Emergency Data Export")
        }
        startActivity(Intent.createChooser(intent, getString(R.string.export_emergency_data)))
    }

    private fun importEmergencyData() {
        // TODO: Implement file picker for import
        Toast.makeText(this, R.string.import_not_implemented, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        crashDetectionManager.stopMonitoring()
        fatigueDetectionManager.stopMonitoring()
        speedWarningManager.cleanup()
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, SafetyActivity::class.java))
        }
    }
}