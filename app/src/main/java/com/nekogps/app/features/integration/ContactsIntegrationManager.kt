package com.nekogps.app.features.integration

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract

/** Contact with a postal address usable for navigation. */
data class NavigableContact(
    val id: Long,
    val name: String,
    val address: String
)

/**
 * Finds contacts that have a postal address so the user can navigate to them.
 * Requires READ_CONTACTS (requested by the caller); returns emptyList otherwise.
 */
class ContactsIntegrationManager(private val context: Context) {

    fun searchContactsByName(query: String, limit: Int = 20): List<NavigableContact> {
        val result = mutableListOf<NavigableContact>()
        try {
            val cr: ContentResolver = context.contentResolver
            val uri: Uri = ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID,
                ContactsContract.CommonDataKinds.StructuredPostal.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS
            )
            val selection = "${ContactsContract.CommonDataKinds.StructuredPostal.DISPLAY_NAME} LIKE ?"
            val args = arrayOf("%$query%")
            cr.query(uri, projection, selection, args, null)?.use { c ->
                val idCol = c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID)
                val nameCol = c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.DISPLAY_NAME)
                val addrCol = c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)
                while (c.moveToNext() && result.size < limit) {
                    val address = if (addrCol >= 0) c.getString(addrCol).orEmpty() else ""
                    if (address.isBlank()) continue
                    result.add(
                        NavigableContact(
                            id = if (idCol >= 0) c.getLong(idCol) else -1L,
                            name = if (nameCol >= 0) c.getString(nameCol).orEmpty() else "",
                            address = address
                        )
                    )
                }
            }
        } catch (se: SecurityException) {
            return emptyList()
        } catch (e: Exception) {
            return emptyList()
        }
        return result
    }

    /** Opens the contact address in the map via a geo: intent. */
    fun navigateToContact(contact: NavigableContact): Boolean {
        return try {
            val uri = Uri.parse("geo:0,0?q=" + Uri.encode(contact.address))
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
