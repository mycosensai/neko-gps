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
        val tracks = mutableListOf<GpxTrack>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(inputStream, null)

        var eventType = parser.eventType
        var currentTrack: GpxTrackBuilder? = null
        var currentSegmentPoints = mutableListOf<TrackPoint>()
        var currentWaypoint: TrackPoint? = null
        var currentTag = ""
        var trackName = ""
        var trackDescription = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    when (parser.name) {
                        "trk" -> {
                            currentTrack = GpxTrackBuilder()
                            currentSegmentPoints = mutableListOf()
                            trackName = ""
                            trackDescription = ""
                        }
                        "trkseg" -> {
                            currentSegmentPoints = mutableListOf()
                        }
                        "trkpt", "rtept", "wpt" -> {
                            val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull() ?: 0.0
                            val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull() ?: 0.0
                            currentWaypoint = TrackPoint(latitude = lat, longitude = lon)
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim() ?: ""
                    if (text.isNotEmpty() && currentWaypoint != null) {
                        when (currentTag) {
                            "ele" -> {
                                currentWaypoint = currentWaypoint.copy(elevation = text.toDoubleOrNull())
                            }
                            "time" -> {
                                val timestamp = parseGpxTime(text)
                                currentWaypoint = currentWaypoint.copy(time = timestamp)
                            }
                            "speed" -> {
                                currentWaypoint = currentWaypoint.copy(speed = text.toDoubleOrNull())
                            }
                        }
                    } else if (text.isNotEmpty() && currentTrack != null) {
                        when (currentTag) {
                            "name" -> trackName = text
                            "desc" -> trackDescription = text
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "trkpt", "rtept" -> {
                            currentWaypoint?.let { currentSegmentPoints.add(it) }
                            currentWaypoint = null
                        }
                        "wpt" -> {
                            // Waypoints can be handled separately if needed
                            currentWaypoint = null
                        }
                        "trkseg" -> {
                            // Segment complete, points already in the list
                        }
                        "trk" -> {
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
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        return tracks
    }

    /**
     * Parse GPX time string to milliseconds since epoch.
     */
    private fun parseGpxTime(timeStr: String): Long? {
        return try {
            dateFormat.parse(timeStr)?.time
        } catch (e: Exception) {
            Log.w("GpxParser", "parseGpxTime: suppressed Exception", e)
            try {
                dateFormatWithMillis.parse(timeStr)?.time
            } catch (e2: Exception) {
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
        val start = track.startTime ?: return 0L
        val end = track.endTime ?: return 0L
        return end - start
    }

    /**
     * Get elevation gain (total ascent) in meters.
     */
    fun getElevationGain(track: GpxTrack): Double {
        var gain = 0.0
        for (i in 1 until track.points.size) {
            val prevElev = track.points[i - 1].elevation ?: continue
            val currElev = track.points[i].elevation ?: continue
            val diff = currElev - prevElev
            if (diff > 0) gain += diff
        }
        return gain
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
