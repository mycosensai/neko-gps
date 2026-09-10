package com.nekogps.app.features.integration

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract

/** Simple calendar event with an optional location. */
data class CalendarEvent(
    val id: Long,
    val title: String,
    val location: String,
    val startTime: Long,
    val endTime: Long
)

/**
 * Reads calendar events that carry a location so the user can navigate to them.
 * Requires READ_CALENDAR (requested by the caller); returns emptyList when
 * the permission is missing or no provider is available.
 */
class CalendarIntegrationManager(private val context: Context) {

    fun getUpcomingEventsWithLocation(limit: Int = 20): List<CalendarEvent> {
        val result = mutableListOf<CalendarEvent>()
        try {
            val now = System.currentTimeMillis()
            val uri: Uri = CalendarContract.Events.CONTENT_URI
            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.EVENT_LOCATION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND
            )
            val selection = "${CalendarContract.Events.DTSTART} >= ?"
            val args = arrayOf(now.toString())
            val sort = "${CalendarContract.Events.DTSTART} ASC"
            val cr: ContentResolver = context.contentResolver
            cr.query(uri, projection, selection, args, sort)?.use { c ->
                val idCol = c.getColumnIndex(CalendarContract.Events._ID)
                val titleCol = c.getColumnIndex(CalendarContract.Events.TITLE)
                val locCol = c.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)
                val startCol = c.getColumnIndex(CalendarContract.Events.DTSTART)
                val endCol = c.getColumnIndex(CalendarContract.Events.DTEND)
                while (c.moveToNext() && result.size < limit) {
                    val location = if (locCol >= 0) c.getString(locCol).orEmpty() else ""
                    if (location.isBlank()) continue
                    result.add(
                        CalendarEvent(
                            id = if (idCol >= 0) c.getLong(idCol) else -1L,
                            title = if (titleCol >= 0) c.getString(titleCol).orEmpty() else "",
                            location = location,
                            startTime = if (startCol >= 0) c.getLong(startCol) else 0L,
                            endTime = if (endCol >= 0) c.getLong(endCol) else 0L
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

    /** Opens the event location in the map via a geo: intent. */
    fun navigateToEvent(event: CalendarEvent): Boolean {
        return try {
            val uri = Uri.parse("geo:0,0?q=" + Uri.encode(event.location))
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
