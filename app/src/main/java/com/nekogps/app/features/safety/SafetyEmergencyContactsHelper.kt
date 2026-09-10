package com.nekogps.app.features.safety

import android.content.Context
import android.view.View
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText

class SafetyEmergencyContactsHelper(private val activity: SafetyActivity) {

    private val sosButton: MaterialButton = activity.findViewById(R.id.btn_sos)
    private val sosStatusText: TextView = activity.findViewById(R.id.tv_sos_status)
    private val contactsContainer: LinearLayout = activity.findViewById(R.id.contacts_container)
    private val addContactButton: MaterialButton = activity.findViewById(R.id.btn_add_contact)
    private val autoSmsSwitch: SwitchMaterial = activity.findViewById(R.id.switch_auto_sms)
    private val autoCallSwitch: SwitchMaterial = activity.findViewById(R.id.switch_auto_call)
    private val includeLocationSwitch: SwitchMaterial = activity.findViewById(R.id.switch_include_location)

    fun bindListeners() {
        sosButton.setOnClickListener { showSOSConfirmationDialog() }
        addContactButton.setOnClickListener { showAddContactDialog() }
        autoSmsSwitch.setOnCheckedChangeListener { _, checked -> activity.emergencySOSManager.setAutoSendSMS(checked) }
        autoCallSwitch.setOnCheckedChangeListener { _, checked -> activity.emergencySOSManager.setAutoCall(checked) }
        includeLocationSwitch.setOnCheckedChangeListener { _, checked ->
            activity.emergencySOSManager.setIncludeLocation(checked)
        }
    }

    fun updateStatus() {
        val contacts = activity.emergencySOSManager.getEmergencyContacts()
        sosStatusText.text = activity.getString(R.string.sos_contacts_configured, contacts.size)
    }

    fun renderList() {
        contactsContainer.removeAllViews()
        val contacts = activity.emergencySOSManager.getEmergencyContacts()

        if (contacts.isEmpty()) {
            val emptyText = TextView(activity).apply {
                text = activity.getString(R.string.no_emergency_contacts)
                setTextColor(activity.resources.getColor(R.color.text_secondary, activity.theme))
                setPadding(0, EMPTY_VIEW_VERTICAL_PADDING_PX, 0, EMPTY_VIEW_VERTICAL_PADDING_PX)
                gravity = android.view.Gravity.CENTER
            }
            contactsContainer.addView(emptyText)
            return
        }

        for (contact in contacts) {
            val itemView = activity.layoutInflater.inflate(R.layout.item_emergency_contact, contactsContainer, false)
            itemView.findViewById<TextView>(R.id.tv_contact_name).text = contact.name
            itemView.findViewById<TextView>(R.id.tv_contact_phone).text = contact.phoneNumber
            val contactBadge = itemView.findViewById<TextView>(R.id.tv_contact_primary)
            contactBadge.visibility = if (contact.isPrimary) View.VISIBLE else View.GONE

            itemView.findViewById<MaterialButton>(R.id.btn_edit_contact).setOnClickListener {
                showEditContactDialog(contact)
            }
            itemView.findViewById<MaterialButton>(R.id.btn_delete_contact).setOnClickListener {
                activity.emergencySOSManager.removeEmergencyContact(contact.id)
                renderList()
            }

            contactsContainer.addView(itemView)
        }
    }

    private fun showSOSConfirmationDialog() {
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.emergency_sos)
            .setMessage(R.string.sos_confirm_message)
            .setIcon(R.drawable.ic_emergency)
            .setPositiveButton(R.string.send_sos) { _, _ ->
                activity.emergencySOSManager.triggerSOS()
                showSOSFeedbackDialog()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showSOSFeedbackDialog() {
        val contacts = activity.emergencySOSManager.getEmergencyContacts()
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.sos_sent)
            .setMessage(activity.getString(R.string.sos_sent_to_contacts, contacts.size))
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun showAddContactDialog() {
        val view = activity.layoutInflater.inflate(R.layout.dialog_add_emergency_contact, null)
        val nameEdit = view.findViewById<TextInputEditText>(R.id.et_contact_name)
        val phoneEdit = view.findViewById<TextInputEditText>(R.id.et_contact_phone)
        val primaryCheck = view.findViewById<SwitchMaterial>(R.id.switch_primary_contact)

        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.add_emergency_contact)
            .setView(view)
            .setPositiveButton(R.string.add) { _, _ ->
                val name = nameEdit.text.toString().trim()
                val phone = phoneEdit.text.toString().trim()
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    val contact = EmergencyContact(name = name, phoneNumber = phone, isPrimary = primaryCheck.isChecked)
                    activity.emergencySOSManager.addEmergencyContact(contact)
                    renderList()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEditContactDialog(contact: EmergencyContact) {
        val view = activity.layoutInflater.inflate(R.layout.dialog_add_emergency_contact, null)
        val nameEdit = view.findViewById<TextInputEditText>(R.id.et_contact_name)
        val phoneEdit = view.findViewById<TextInputEditText>(R.id.et_contact_phone)
        val primaryCheck = view.findViewById<SwitchMaterial>(R.id.switch_primary_contact)

        nameEdit.setText(contact.name)
        phoneEdit.setText(contact.phoneNumber)
        primaryCheck.isChecked = contact.isPrimary

        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.edit_emergency_contact)
            .setView(view)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = nameEdit.text.toString().trim()
                val phone = phoneEdit.text.toString().trim()
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    val updated = contact.copy(
                        name = name,
                        phoneNumber = phone,
                        isPrimary = primaryCheck.isChecked
                    )
                    activity.emergencySOSManager.updateEmergencyContact(updated)
                    renderList()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    companion object {
        private const val EMPTY_VIEW_VERTICAL_PADDING_PX = 32
    }
}
