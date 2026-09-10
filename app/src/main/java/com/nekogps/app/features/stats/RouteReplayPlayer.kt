package com.nekogps.app.features.stats

import android.os.Handler
import android.os.Looper
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * RouteReplayPlayer - owns route playback state (polyline, marker, handler)
 * and animates saved route playback on an osmdroid map.
 */
class RouteReplayPlayer(
    private val mapView: MapView,
    private val onProgressUpdate: (current: Int, total: Int) -> Unit,
    private val onPlaybackFinished: () -> Unit
) {
    companion object {
        private const val REPLAY_LINE_WIDTH = 8f
        private const val REPLAY_FRAME_DELAY_MS = 500L
    }

    private var replayPolyline: Polyline? = null
    private var replayMarker: Marker? = null
    private var currentPointIndex = 0
    private var handler: Handler? = null
    private lateinit var replayRunnable: Runnable

    var isReplaying = false
        private set

    fun start(routePoints: List<GeoPoint>) {
        currentPointIndex = 0
        isReplaying = true
        handler = Handler(Looper.getMainLooper())
        replayRunnable = Runnable {
            if (currentPointIndex < routePoints.size && isReplaying) {
                val point = routePoints[currentPointIndex]
                val geoPoint = GeoPoint(point.latitude, point.longitude)

                if (replayPolyline == null) {
                    replayPolyline = Polyline(mapView).apply {
                        color = android.graphics.Color.parseColor("#cbb7fb")
                        width = REPLAY_LINE_WIDTH
                    }
                    mapView.overlays.add(replayPolyline)
                }
                val points = ArrayList<GeoPoint>()
                for (i in 0..currentPointIndex) {
                    points.add(GeoPoint(routePoints[i].latitude, routePoints[i].longitude))
                }
                mapView.overlays.remove(replayPolyline)
                val newPolyline = Polyline().apply {
                    setPoints(
                        routePoints.subList(0, currentPointIndex + 1).map {
                            GeoPoint(it.latitude, it.longitude)
                        }
                    )
                    color = android.graphics.Color.parseColor("#cbb7fb")
                    width = REPLAY_LINE_WIDTH
                }
                mapView.overlays.add(newPolyline)
                replayPolyline = newPolyline

                if (replayMarker == null) {
                    replayMarker = Marker(mapView).apply {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Current Location"
                    }
                    mapView.overlays.add(replayMarker)
                }
                replayMarker?.position = geoPoint
                mapView.controller.animateTo(geoPoint)

                onProgressUpdate(currentPointIndex + 1, routePoints.size)

                currentPointIndex++
                handler?.postDelayed(replayRunnable, REPLAY_FRAME_DELAY_MS)
            } else {
                isReplaying = false
                onPlaybackFinished()
            }
        }
        handler?.post(replayRunnable)
    }

    fun stop() {
        isReplaying = false
        if (::replayRunnable.isInitialized) {
            handler?.removeCallbacks(replayRunnable)
        }
    }

    fun clear() {
        replayPolyline?.let { mapView.overlays.remove(it) }
        replayPolyline = null
        replayMarker?.let { mapView.overlays.remove(it) }
        replayMarker = null
        mapView.invalidate()
    }

    fun reset(routePoints: List<GeoPoint>) {
        stop()
        clear()
        currentPointIndex = 0
        if (routePoints.isNotEmpty()) {
            val startPoint = routePoints[0]
            replayMarker = Marker(mapView).apply {
                position = GeoPoint(startPoint.latitude, startPoint.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Start"
            }
            mapView.overlays.add(replayMarker)
            mapView.controller.animateTo(GeoPoint(startPoint.latitude, startPoint.longitude))
            mapView.invalidate()
        }
    }
}
