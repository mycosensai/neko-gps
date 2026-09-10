package com.nekogps.app.features.integration

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log

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
        return runCatching { queryContacts(query, limit) }
            .onFailure { Log.w("ContactsIntegrationManager", "searchContactsByName: failed", it) }
            .getOrDefault(emptyList())
    }

    private fun queryContacts(query: String, limit: Int): List<NavigableContact> {
        val cr: ContentResolver = context.contentResolver
            val uri: Uri = ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID,
                ContactsContract.CommonDataKinds.StructuredPostal.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS
            )
            val selection = "${ContactsContract.CommonDataKinds.StructuredPostal.DISPLAY_NAME} LIKE ?"
            val args = arrayOf("%$query%")
            return cr.query(uri, projection, selection, args, null)?.use { c ->
                drainContacts(c, limit)
            } ?: emptyList()
    }

    private fun drainContacts(
        c: android.database.Cursor,
        limit: Int
    ): List<NavigableContact> {
        val result = mutableListOf<NavigableContact>()
        val idCol = c.getColumnIndex(
            ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID
        )
        val nameCol = c.getColumnIndex(
            ContactsContract.CommonDataKinds.StructuredPostal.DISPLAY_NAME
        )
        val addrCol = c.getColumnIndex(
            ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS
        )
        while (c.moveToNext() && result.size < limit) {
            toContact(c, idCol, nameCol, addrCol)?.let { result.add(it) }
        }
        return result
    }

    private fun toContact(
        c: android.database.Cursor,
        idCol: Int,
        nameCol: Int,
        addrCol: Int
    ): NavigableContact? {
        val address = if (addrCol >= 0) c.getString(addrCol).orEmpty() else ""
        if (address.isBlank()) return null
        return NavigableContact(
            id = if (idCol >= 0) c.getLong(idCol) else -1L,
            name = if (nameCol >= 0) c.getString(nameCol).orEmpty() else "",
            address = address
        )
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
        } catch (e: ActivityNotFoundException) {
            Log.w("ContactsIntegrationManager", "navigateToContact: suppressed Exception", e)
            false
        } catch (e: SecurityException) {
            Log.w("ContactsIntegrationManager", "navigateToContact: suppressed Exception", e)
            false
        }
    }
}
