package com.nekogps.app.features.integration

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log

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
        return runCatching { queryEvents(limit) }
            .onFailure { Log.w("CalendarIntegrationManager", "getUpcomingEventsWithLocation: failed", it) }
            .getOrDefault(emptyList())
    }

    private fun queryEvents(limit: Int): List<CalendarEvent> {
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
            return cr.query(uri, projection, selection, args, sort)?.use { c ->
                drainEvents(c, limit)
            } ?: emptyList()
    }

    private fun drainEvents(c: android.database.Cursor, limit: Int): List<CalendarEvent> {
        val result = mutableListOf<CalendarEvent>()
        val idCol = c.getColumnIndex(CalendarContract.Events._ID)
        val titleCol = c.getColumnIndex(CalendarContract.Events.TITLE)
        val locCol = c.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)
        val startCol = c.getColumnIndex(CalendarContract.Events.DTSTART)
        val endCol = c.getColumnIndex(CalendarContract.Events.DTEND)
        while (c.moveToNext() && result.size < limit) {
            toEvent(c, EventCols(idCol, titleCol, locCol, startCol, endCol))?.let { result.add(it) }
        }
        return result
    }

    private data class EventCols(
        val idCol: Int,
        val titleCol: Int,
        val locCol: Int,
        val startCol: Int,
        val endCol: Int
    )

    private fun toEvent(
        c: android.database.Cursor,
        cols: EventCols
    ): CalendarEvent? {
        val location = if (cols.locCol >= 0) c.getString(cols.locCol).orEmpty() else ""
        if (location.isBlank()) return null
        return CalendarEvent(
            id = if (cols.idCol >= 0) c.getLong(cols.idCol) else -1L,
            title = if (cols.titleCol >= 0) c.getString(cols.titleCol).orEmpty() else "",
            location = location,
            startTime = if (cols.startCol >= 0) c.getLong(cols.startCol) else 0L,
            endTime = if (cols.endCol >= 0) c.getLong(cols.endCol) else 0L
        )
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
        } catch (e: ActivityNotFoundException) {
            Log.w("CalendarIntegrationManager", "navigateToEvent: suppressed Exception", e)
            false
        } catch (e: SecurityException) {
            Log.w("CalendarIntegrationManager", "navigateToEvent: suppressed Exception", e)
            false
        }
    }
}
