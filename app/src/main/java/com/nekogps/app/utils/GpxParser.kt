package com.nekogps.app.utils

import org.osmdroid.util.GeoPoint
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import android.util.Log

/**
 * Utility for parsing GPX (GPS Exchange Format) files into track point lists.
 * Supports GPX 1.0 and 1.1 formats with track segments, waypoints, and routes.
 */
object GpxParser {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val dateFormatWithMillis = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Represents a parsed GPX track with metadata and points.
     */
    data class GpxTrack(
        val name: String,
        val description: String,
        val points: List<TrackPoint>,
        val startTime: Long? = null,
        val endTime: Long? = null
    )

    /**
     * Represents a single track point with position and optional metadata.
     */
    data class TrackPoint(
        val latitude: Double,
        val longitude: Double,
        val elevation: Double? = null,
        val time: Long? = null,
        val speed: Double? = null
    )

    /**
     * Parse a GPX file from an InputStream.
     * @param inputStream The input stream of the GPX file
     * @return List of parsed GpxTrack objects
     */
    fun parse(inputStream: InputStream): List<GpxTrack> {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(inputStream, null)
        return GpxDocumentParser(parser).parse()
    }

    /**
     * Parse GPX time string to milliseconds since epoch.
     */
    private fun parseGpxTime(timeStr: String): Long? {
        return try {
            dateFormat.parse(timeStr)?.time
        } catch (e: java.text.ParseException) {
            Log.w("GpxParser", "parseGpxTime: suppressed Exception", e)
            try {
                dateFormatWithMillis.parse(timeStr)?.time
            } catch (e2: java.text.ParseException) {
                Log.w("GpxParser", "parseGpxTime: suppressed Exception", e2)
                null
            }
        }
    }

    /**
     * Convert GPX track points to GeoPoint list for osmdroid.
     */
    fun toGeoPoints(points: List<TrackPoint>): List<GeoPoint> {
        return points.map { GeoPoint(it.latitude, it.longitude) }
    }

    /**
     * Get total distance of a GPX track in meters.
     */
    fun getTrackDistance(track: GpxTrack): Double {
        var total = 0.0
        for (i in 0 until track.points.size - 1) {
            total += DistanceCalculator.haversineDistance(
                track.points[i].latitude, track.points[i].longitude,
                track.points[i + 1].latitude, track.points[i + 1].longitude
            )
        }
        return total
    }

    /**
     * Get total duration of a GPX track in milliseconds.
     */
    fun getTrackDuration(track: GpxTrack): Long {
        return computeTrackDuration(track.startTime, track.endTime)
    }

    private fun computeTrackDuration(startTime: Long?, endTime: Long?): Long {
        return if (startTime == null || endTime == null) 0L else endTime - startTime
    }

    /**
     * Get elevation gain (total ascent) in meters.
     */
    fun getElevationGain(track: GpxTrack): Double {
        return track.points.asSequence()
            .mapNotNull { it.elevation }
            .zipWithNext()
            .map { (prev, curr) -> curr - prev }
            .filter { it > 0 }
            .sum()
    }

    /**
     * Stateful pull-parser that converts GPX XML events into [GpxTrack] objects.
     * Split out of [parse] so each event type is handled by a small focused function.
     */
    private class GpxDocumentParser(private val parser: XmlPullParser) {
        private val tracks = mutableListOf<GpxTrack>()
        private var currentTrack: GpxTrackBuilder? = null
        private var currentSegmentPoints = mutableListOf<TrackPoint>()
        private var currentWaypoint: TrackPoint? = null
        private var currentTag = ""
        private var trackName = ""
        private var trackDescription = ""

        fun parse(): List<GpxTrack> {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                dispatchEvent(eventType)
                eventType = parser.next()
            }
            return tracks
        }

        private fun dispatchEvent(eventType: Int) {
            when (eventType) {
                XmlPullParser.START_TAG -> handleStartTag()
                XmlPullParser.TEXT -> handleText()
                XmlPullParser.END_TAG -> handleEndTag()
            }
        }

        private fun handleStartTag() {
            currentTag = parser.name
            when (parser.name) {
                "trk" -> startTrack()
                "trkseg" -> currentSegmentPoints = mutableListOf()
                "trkpt", "rtept", "wpt" -> startWaypoint()
            }
        }

        private fun startTrack() {
            currentTrack = GpxTrackBuilder()
            currentSegmentPoints = mutableListOf()
            trackName = ""
            trackDescription = ""
        }

        private fun startWaypoint() {
            val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull() ?: 0.0
            val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull() ?: 0.0
            currentWaypoint = TrackPoint(latitude = lat, longitude = lon)
        }

        private fun handleText() {
            val text = parser.text?.trim() ?: ""
            if (text.isEmpty()) return
            if (currentWaypoint != null) {
                applyWaypointText(text)
            } else if (currentTrack != null) {
                applyTrackText(text)
            }
        }

        private fun applyWaypointText(text: String) {
            val waypoint = currentWaypoint ?: return
            currentWaypoint = when (currentTag) {
                "ele" -> waypoint.copy(elevation = text.toDoubleOrNull())
                "time" -> waypoint.copy(time = parseGpxTime(text))
                "speed" -> waypoint.copy(speed = text.toDoubleOrNull())
                else -> waypoint
            }
        }

        private fun applyTrackText(text: String) {
            when (currentTag) {
                "name" -> trackName = text
                "desc" -> trackDescription = text
            }
        }

        private fun handleEndTag() {
            when (parser.name) {
                "trkpt", "rtept" -> {
                    currentWaypoint?.let { currentSegmentPoints.add(it) }
                    currentWaypoint = null
                }
                "wpt" -> {
                    // Waypoints can be handled separately if needed
                    currentWaypoint = null
                }
                "trk" -> finishTrack()
            }
            currentTag = ""
        }

        private fun finishTrack() {
            val allPoints = currentSegmentPoints.toList()
            val track = GpxTrack(
                name = trackName.ifEmpty { "Unnamed Track" },
                description = trackDescription,
                points = allPoints,
                startTime = allPoints.firstOrNull()?.time,
                endTime = allPoints.lastOrNull()?.time
            )
            tracks.add(track)
            currentTrack = null
        }
    }

    /**
     * Builder class for constructing GpxTrack during parsing.
     */
    private class GpxTrackBuilder {
        var name: String = ""
        var description: String = ""
        val points = mutableListOf<TrackPoint>()
    }
}
